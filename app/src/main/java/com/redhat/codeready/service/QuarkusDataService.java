package com.redhat.codeready.service;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.redhat.codeready.configuration.DataGridConfiguration;
import com.redhat.codeready.model.AbstractDataObject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class QuarkusDataService {

  private static final Logger LOGGER = Logger.getLogger(QuarkusDataService.class);

  @ConfigProperty(name = "application.cache.name")
  String cacheName;

  @Inject
  @DataGridConfiguration
  RemoteCacheManager cacheManager;

  RemoteCache<UUID, AbstractDataObject> quarkus;

  void onStart(@Observes StartupEvent event) {
    LOGGER.tracef("Entering to %s, event: %s", "onStart()", event);
    quarkus = cacheManager.getCache(cacheName);
    LOGGER.debugf("CacheManager classLoader: %s", cacheManager.getClass().getClassLoader().getName());
    LOGGER.debugf("Cache classLoader: %s", quarkus.getClass().getClassLoader().getName());
  }

  public boolean has(UUID key) {
    LOGGER.tracef("Entering to %s, key: %s", "has()", key);
    return quarkus.containsKey(key);
  }

  public boolean has(AbstractDataObject ado) {
    UUID key = ado.getId();
    LOGGER.tracef("Entering to %s, key: %s", "has()", key);
    return quarkus.containsKey(key);
  }

  public AbstractDataObject get(UUID key) {
    LOGGER.tracef("Entering to %s, key: %s", "get()", key);
    AbstractDataObject ado = quarkus.get(key);
    return ado;
  }

  public AbstractDataObject delete(UUID key) {
    LOGGER.tracef("Entering to %s, key: %s", "delete()", key);
    return quarkus.remove(key);
  }

  public AbstractDataObject create(AbstractDataObject ado) {
    LOGGER.tracef("Entering to %s, dataObject: %s", "create()", ado);
    return quarkus.put(ado.getId(), ado);
  }

  public boolean update(AbstractDataObject ado) {
    LOGGER.tracef("Entering to %s, dataObject: %s", "update()", ado);
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
  }
}