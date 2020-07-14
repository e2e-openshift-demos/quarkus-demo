package com.redhat.codeready;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.redhat.codeready.configuration.DataGridConfiguration;
import com.redhat.codeready.model.AbstractDataObject;
import com.redhat.codeready.model.QuarkusDemoDataObject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class CacheLoader {

  private static final Logger LOGGER = Logger.getLogger(CacheLoader.class);
  private static final int START_PRIORITY = 1000;

  public static final QuarkusDemoDataObject TEST_GET = new QuarkusDemoDataObject(
      UUID.fromString("11111111-0E00-3333-4444-555555555555"), "TEST_GET");
  public static final QuarkusDemoDataObject TEST_REMOVE = new QuarkusDemoDataObject(
      UUID.fromString("11111111-DE00-3333-4444-555555555555"), "TEST_REMOVE");
  public static final QuarkusDemoDataObject TEST_UPDATE_GOOD = new QuarkusDemoDataObject(
      UUID.fromString("11111111-00DA-3333-4444-555555555555"), "TEST_UPDATE_GOOD");
  public static final QuarkusDemoDataObject TEST_UPDATE_BAD = new QuarkusDemoDataObject(
      UUID.fromString("11111111-BADA-3333-4444-555555555555"), "TEST_UPDATE_BAD");
  private static final AbstractDataObject ADOs[] = new AbstractDataObject[] { TEST_GET, TEST_REMOVE, TEST_UPDATE_GOOD,
      TEST_UPDATE_BAD };

  @ConfigProperty(name = "application.cache.name")
  String cacheName;

  @Inject
  @DataGridConfiguration
  RemoteCacheManager cacheManager;

  void onStart(@Observes @Priority(value = START_PRIORITY) final StartupEvent event) {
    LOGGER.tracef("Entering to %s, event: %s, priority %d", "onStart()", event, START_PRIORITY);
    LOGGER.infof("Working dir is: %s", new java.io.File("").getAbsoluteFile());
    final RemoteCache<UUID, AbstractDataObject> cache = cacheManager.getCache(cacheName);
    cleanupCache(cache);
    dataLoad(cache);
  }

  private void cleanupCache(final RemoteCache<?, ?> cache) {
    LOGGER.tracef("Entering to %s, cache: %s", "cleanupCache()", cache.getName());
    try {
      cache.clearAsync().get(30, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      LOGGER.error("Error occured while cache %s cleanup", cache.getName(), e);
    }
  }

  private void dataLoad(final RemoteCache<UUID, AbstractDataObject> cache) {
    LOGGER.tracef("Entering to %s, cache: %s", "dataLoad()", cache.getName());
    for (AbstractDataObject ado : ADOs) {
      LOGGER.debugf("Put '%s' into cache'", ado);
      cache.put(ado.getId(), ado);
      final AbstractDataObject a = cache.get(ado.getId());
      if (a == null) {
        final String msg = String.format("CACHE LOAD ERROR: Object with id %s not found in th cache %s", ado.getId(), cache.getName());
        LOGGER.error(msg);
        throw new RuntimeException(msg);
      }
    }
    LOGGER.infof("Loaded '%d' objects into cache '%s'", cache.size(), cache.getName());
  }
}
