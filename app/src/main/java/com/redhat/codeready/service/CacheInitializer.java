package com.redhat.codeready.service;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.redhat.codeready.configuration.DataGridConfiguration;
import com.redhat.codeready.mgmt.Counter;
import com.redhat.codeready.mgmt.CustomStatus;
import com.redhat.codeready.mgmt.CustomStatusService;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ProfileManager;
import io.vertx.core.json.JsonObject;

@Readiness
@ApplicationScoped
public class CacheInitializer implements CustomStatus, HealthCheck {
  private static final Logger LOGGER = Logger.getLogger(CacheInitializer.class);
  private static final String DEFAULT_CACHE_CONFIGURATION = "<infinispan><cache-container><distributed-cache name=\"%s\">"
      + "</distributed-cache></cache-container></infinispan>";
  private static final int START_PRIORITY = 500;

  @ConfigProperty(name = "application.cache.name")
  String cacheName;

  @Inject
  @DataGridConfiguration
  RemoteCacheManager cacheManager;

  @Inject
  CustomStatusService customStatus;

  private Counter counter = new Counter();

  private boolean alive = false;

  void onStart(@Observes @Priority(value = START_PRIORITY) final StartupEvent event) {
    LOGGER.tracef("Entering to %s, event: %s, priority %d", "onStart()", event, START_PRIORITY);
    alive = true;
    customStatus.register(this);
    LOGGER.infof("The application is starting with profile: %s", ProfileManager.getActiveProfile());
    LOGGER.infof("Working dir is: %s", new java.io.File("").getAbsoluteFile());
    // LOGGER.infof("Classpath: %s", System.getProperty("java.class.path"));
    final XMLStringConfiguration configuration = new XMLStringConfiguration(
        String.format(DEFAULT_CACHE_CONFIGURATION, cacheName));
    LOGGER.infof("Remote cache configuration: %s", configuration.toXMLString("configuration"));
    cacheManager.administration().getOrCreateCache(cacheName, configuration);
    counter.success();
    LOGGER.debugf("CacheManager classLoader: %s", cacheManager.getClass().getClassLoader().getName());
  }

  @Override
  public boolean getReady() {
    return cacheManager.isStarted() && cacheManager.getCache(cacheName) != null;
  }

  @Override
  public boolean getAlive() {
    return alive;
  }

  @Override
  public JsonObject getStatus() {
    JsonObject status = new JsonObject();
    status.put("requests", new JsonObject().put("all", counter.getCount()).put("success", counter.getSuccessCount())
        .put("errors", counter.getErrorsCount()));
    status.put("cache", new JsonObject().put("name", cacheName).put("size", cacheManager.getCache(cacheName).size()));
    return status;
  }

  @Override
  public String getType() {
    return CustomStatus.TYPE_DATAGRID;
  }

  @Override
  public HealthCheckResponse call() {
    HealthCheckResponseBuilder rb = HealthCheckResponse.named(getName());
    if (cacheManager.isStarted() && cacheManager.getCache(cacheName) != null) {
      rb.up();
    } else {
      rb.down();
    }
    rb.withData("type", getType()).withData("cache", cacheName).withData("size", cacheManager.getCache(cacheName).size());
    return rb.build();
  }
}