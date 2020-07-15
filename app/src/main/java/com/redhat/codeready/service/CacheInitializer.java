package com.redhat.codeready.service;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.redhat.codeready.configuration.DataGridConfiguration;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ProfileManager;

@ApplicationScoped
public class CacheInitializer {
  private static final Logger LOGGER = Logger.getLogger(CacheInitializer.class);
  private static final String DEFAULT_CACHE_CONFIGURATION = "<infinispan><cache-container><distributed-cache name=\"%s\">"
      + "</distributed-cache></cache-container></infinispan>";
  private static final int START_PRIORITY = 500;

  @ConfigProperty(name = "application.cache.name")
  String cacheName;

  @Inject
  @DataGridConfiguration
  RemoteCacheManager cacheManager;

  void onStart(@Observes @Priority(value = START_PRIORITY) final StartupEvent event) {
    LOGGER.tracef("Entering to %s, event: %s, priority %d", "onStart()", event, START_PRIORITY);
    LOGGER.infof("The application is starting with profile: %s", ProfileManager.getActiveProfile());
    LOGGER.infof("Working dir is: %s", new java.io.File("").getAbsoluteFile());
    // LOGGER.infof("Classpath: %s", System.getProperty("java.class.path"));
    final XMLStringConfiguration configuration = new XMLStringConfiguration(
        String.format(DEFAULT_CACHE_CONFIGURATION, cacheName));
    LOGGER.infof("Remote cache configuration: %s", configuration.toXMLString("configuration"));
    cacheManager.administration().getOrCreateCache(cacheName, configuration);
    LOGGER.debugf("CacheManager classLoader: %s", cacheManager.getClass().getClassLoader().getName());
  }
}