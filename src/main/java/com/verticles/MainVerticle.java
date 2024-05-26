package com.verticles;

import com.beust.jcommander.JCommander;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerHandler;

import java.util.*;

public class MainVerticle extends AbstractVerticle {
  private static final String API_KEY_HEADER = "X-Api-Key";
  private static final String AUTHORIZED_USER = "authorized-user";
  private static final String USED_API_KEY = "used-api-key";
  private final Set<String> apiKeys = new HashSet<>();
  private final Map<String, String> users = new HashMap<>();
  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public void start() throws Exception {
    Args parsedArgs = new Args();

    // TODO: Test this thing
    String[] args = vertx.getOrCreateContext().config().getString("args", "").trim().split("\\s+");
    if (args.length == 1 && args[0].isEmpty()) {
      args = new String[0];
    }

    String[] testArgs = {
      "-api-keys=key3,key4",
      "-users=user3:password3,user4:password4"
    };

    logger.info("args: " + Arrays.toString(args));

    JCommander jCommander = new JCommander(parsedArgs);
    jCommander.setDefaultProvider(new DefaultProvider());

    try {
      jCommander.parse(testArgs);
    }
    catch (Exception e) {
      logger.error("Error parsing arguments: " + e.getMessage());
      jCommander.usage();
      System.exit(1);
    }

    apiKeys.addAll(parsedArgs.getApiKeys());
    users.putAll(parsedArgs.getUsers());

    logger.info("Api keys: " + apiKeys);
    logger.info("Users: " + users);

    Router router = Router.router(vertx);

    // curl -X POST http://localhost:9000/ -H "X-Api-Key: unexpected-value-1" -d 'Hello' -i
    // curl -X POST http://localhost:9000/ -H "X-Api-Key: expected-value-1" -d 'Hello' -i
    // curl -X POST http://localhost:9000/ -H "X-Api-Key: expected-value-1" -d 'Hello' -i -u user1:password1

    router.route()
      .handler(LoggerHandler.create())
      .handler(this::apiKeyHandler)
      .handler(this::basicAuthHandler);
    router.post().handler(BodyHandler.create()); // to get body from request. Otherwise, null. Have to set it on post route or options etc. If set on get route, it will throw error.

    // curl -X GET http://localhost:9000/ -H "X-Api-Key: expected-value-1" -i -u user1:password1
    router.get("/")
      .handler(context -> {
        String usedApiKey = context.get(USED_API_KEY);
        AuthorizedUser authorizedUser = context.get(AUTHORIZED_USER);

        logger.info(String.format("Request with user [%s], password [%s], API key [%s", authorizedUser.username(), authorizedUser.password(), usedApiKey));

        context.response().end("Hello Vert.x!");
      });

    // curl -X POST http://localhost:9000/data -H "X-Api-Key: expected-value-1" -u user1:password1 -d '{"name":"John Doe","email":"email@email.com", "age":"17"}' -i
    router.post("/data").handler(context -> {
      JsonObject jsonBody = context.body().asJsonObject();
      logger.info(String.format("Received json body: %s", jsonBody));

      if (jsonBody == null) {
        logger.warn("Invalid request body");
        context.response().setStatusCode(400).end("Invalid request body");
        return;
      }

      try {
        IncomingData incomingData = jsonBody.mapTo(IncomingData.class);
        logger.info(String.format("Received data: %s", incomingData));
        context.json(incomingData);
      } catch (IllegalArgumentException e) {
        logger.error("Error parsing json body: " + e.getMessage());
        context.response().setStatusCode(400).end("Invalid request body");
      }
    });

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(9000)
      .onSuccess(server -> logger.info(String.format("HTTP server started on port %d", server.actualPort())));
  }

  private void apiKeyHandler(RoutingContext context) { // custom header validation and it also passes the header value to next handler
    String apiKeyValue = context.request().getHeader(API_KEY_HEADER);
    if (apiKeyValue == null) {
      logger.warn("Unauthorized request without API key");
      context.response().setStatusCode(401).end("Unauthorized: Missing API key");
      return;
    }

    if (!apiKeys.contains(apiKeyValue)) {
      logger.warn(String.format("Unauthorized request with API key %s", apiKeyValue));
      context.response().setStatusCode(401).end("Unauthorized: Invalid API key");
      return;
    }

    context.put(USED_API_KEY, apiKeyValue);
    context.next();
  }

  private void basicAuthHandler(RoutingContext context) { // custom basic auth validation and it also passes the user to next handler
    String authHeader = context.request().getHeader("Authorization");
    if (authHeader == null) {
      logger.warn("Unauthorized request without Authorization header");
      context.response().setStatusCode(401).end("Unauthorized: Missing Authorization header");
      return;
    }

    String[] parts = authHeader.split(" ");
    if (parts.length != 2) {
      logger.warn("Unauthorized request with invalid Authorization header");
      context.response().setStatusCode(401).end("Unauthorized: Invalid Authorization header");
      return;
    }

    String authType = parts[0];
    String authValue = parts[1];

    if (!authType.equals("Basic")) {
      logger.warn("Unauthorized request with invalid Authorization type");
      context.response().setStatusCode(401).end("Unauthorized: Invalid Authorization type");
      return;
    }

    String decodedAuthValue = new String(Base64.getDecoder().decode(authValue));
    String[] authParts = decodedAuthValue.split(":");
    if (authParts.length != 2) {
      logger.warn("Unauthorized request with invalid Authorization value");
      context.response().setStatusCode(401).end("Unauthorized: Invalid Authorization value");
      return;
    }

    String username = authParts[0];
    String password = authParts[1];

    if (!users.containsKey(username) || !users.get(username).equals(password)) {
      logger.warn(String.format("Unauthorized request with invalid credentials %s:%s", username, password));
      context.response().setStatusCode(401).end("Unauthorized: Invalid credentials");
      return;
    }

    AuthorizedUser authorizedUser = new AuthorizedUser(username, password);
    context.put(AUTHORIZED_USER, authorizedUser);
    context.next();
  }
}
