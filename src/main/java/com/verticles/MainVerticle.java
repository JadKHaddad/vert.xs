package com.verticles;

import com.beust.jcommander.JCommander;
import com.verticles.verticles.HttpVerticle;
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
    } catch (Exception e) {
      logger.error("Error parsing arguments: " + e.getMessage());
      jCommander.usage();
      System.exit(1);
    }

    logger.info("Api keys: " + parsedArgs.getApiKeys());
    logger.info("Users: " + parsedArgs.getUsers());

    vertx.deployVerticle(new HttpVerticle(new HashSet<>(parsedArgs.getApiKeys()), parsedArgs.getUsers())).onSuccess(id -> {
      logger.info("HttpVerticle deployed with id: " + id);
    }).onFailure(err -> {
      logger.error("Error deploying HttpVerticle: " + err.getMessage());
    });
  }
}
