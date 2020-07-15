package com.redhat.codeready.service;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.redhat.codeready.configuration.DataGridConfiguration;
import com.redhat.codeready.mgmt.Counter;
import com.redhat.codeready.mgmt.CustomStatus;
import com.redhat.codeready.mgmt.CustomStatusService;
import com.redhat.codeready.model.AbstractDataObject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.JsonObject;

@Readiness
class QuarkusDataServiceChecker implements HealthCheck {

  @Inject
  QuarkusDataService service;

  @Override
  public HealthCheckResponse call() {
    return service.call();
  }
}

@ApplicationScoped
public class QuarkusDataService implements CustomStatus {

  private static final Logger LOGGER = Logger.getLogger(QuarkusDataService.class);

  @ConfigProperty(name = "application.cache.name")
  String cacheName;

  @Inject
  @DataGridConfiguration
  RemoteCacheManager cacheManager;

  RemoteCache<UUID, AbstractDataObject> quarkus;

  @Inject
  CustomStatusService customStatus;

  private Counter counter = new Counter();

  private boolean alive = false;

  void onStart(@Observes StartupEvent event) {
    LOGGER.tracef("Entering to %s, event: %s", "onStart()", event);
    alive = true;
    customStatus.register(this);
    quarkus = cacheManager.getCache(cacheName);
    LOGGER.debugf("CacheManager classLoader: %s", cacheManager.getClass().getClassLoader().getName());
    LOGGER.debugf("Cache classLoader: %s", quarkus.getClass().getClassLoader().getName());
  }

  public boolean has(UUID key) {
    LOGGER.tracef("Entering to %s, key: %s", "has()", key);
    try {
      boolean has = quarkus.containsKey(key);
      counter.success();
      return has;
    } catch (RuntimeException e) {
      counter.errors();
      throw e;
    }
  }

  public boolean has(AbstractDataObject ado) {
    UUID key = ado.getId();
    LOGGER.tracef("Entering to %s, key: %s", "has()", key);
    try {
      boolean has = quarkus.containsKey(key);
      counter.success();
      return has;
    } catch (RuntimeException e) {
      counter.errors();
      throw e;
    }
  }

  public AbstractDataObject get(UUID key) {
    LOGGER.tracef("Entering to %s, key: %s", "get()", key);
    AbstractDataObject ado = quarkus.get(key);
    try {
      counter.success();
      return ado;
    } catch (RuntimeException e) {
      counter.errors();
      throw e;
    }
  }

  public AbstractDataObject delete(UUID key) {
    LOGGER.tracef("Entering to %s, key: %s", "delete()", key);
    try {
      AbstractDataObject ado = quarkus.remove(key);
      counter.success();
      return ado;
    } catch (RuntimeException e) {
      counter.errors();
      throw e;
    }
  }

  public AbstractDataObject create(AbstractDataObject ado) {
    LOGGER.tracef("Entering to %s, dataObject: %s", "create()", ado);
    try {
      AbstractDataObject ado1 = quarkus.put(ado.getId(), ado);
      counter.success();
      return ado1;
    } catch (RuntimeException e) {
      counter.errors();
      throw e;
    }
  }

  public boolean update(AbstractDataObject ado) {
    LOGGER.tracef("Entering to %s, dataObject: %s", "update()", ado);
    try {
      UUID key = ado.getId();
      if (quarkus.containsKey(key)) {
        LOGGER.debugf("Key %s found in cache %s", key, cacheName);
        AbstractDataObject old = quarkus.get(key);
        if (old.getVersion().equals(ado.getVersion())) {
          synchronized (quarkus) {
            ado.newVersion();
            quarkus.replace(key, ado);
            return true;
          }
        } else {
          LOGGER.debugf("Version missmatch detected for key %s, stored version: %s, requested version %s", key,
              old.getVersion(), ado.getVersion());
        }
      } else {
        LOGGER.debugf("Cache does not contain object for key: %s", key);
      }
      return false;
    } catch (RuntimeException e) {
      counter.errors();
      throw e;
    }
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
    return CustomStatus.TYPE_GENERIC;
  }

  HealthCheckResponse call() {
    HealthCheckResponseBuilder rb = HealthCheckResponse.named(getName());
    if (cacheManager.isStarted() && cacheManager.getCache(cacheName) != null) {
      rb.up();
    } else {
      rb.down();
    }
    rb.withData("type", getType()).withData("cache", cacheName).withData("size",
        cacheManager.getCache(cacheName).size());
    return rb.build();
  }

}