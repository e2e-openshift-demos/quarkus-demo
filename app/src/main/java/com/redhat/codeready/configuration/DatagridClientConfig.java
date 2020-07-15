package com.redhat.codeready.configuration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import com.redhat.codeready.model.DataObjectSerializationContextInitializer;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.marshall.JavaSerializationMarshaller;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.jboss.logging.Logger;

public class DatagridClientConfig {
  private static final Logger LOGGER = Logger.getLogger(DatagridClientConfig.class);

  @ConfigProperty(name = "application.infinispan-client.java_serial_whitelist", defaultValue = "com.redhat.codeready.,java.util.UUID")
  String java_serial_whitelist;

  @ConfigProperty(name = "application.infinispan.hotrod.config", defaultValue = "hotrod-client.properties")
  String hotrodConfigFile;

  @ConfigProperty(name = "application.infinispan-client.sni")
  String sni;

  @ConfigProperty(name = "application.infinispan-client.trust_store_file_name")
  String caCert;

  @ConfigProperty(name = "quarkus.infinispan-client.auth-username")
  String user;

  @ConfigProperty(name = "quarkus.infinispan-client.auth-password")
  String passwd;

  @ConfigProperty(name = "quarkus.infinispan-client.client-intelligence", defaultValue = "BASIC")
  String clientIntelligence;

  @ConfigProperty(name = "quarkus.infinispan-client.sasl-mechanism", defaultValue = "DIGEST-MD5")
  String clientSaslMechanism;

  @ConfigProperty(name = "quarkus.infinispan-client.server-list")
  String clientServerList;

  @DataGridConfiguration
  @Produces
  @ApplicationScoped
  public RemoteCacheManager remoteCacheManager() {
    LOGGER.tracef("Entering to %s", "remoteCacheManager()");
    ConfigurationBuilder builder = new ConfigurationBuilder();
    LOGGER.info("Configuring HotRod client with quarkus.infinispan-client.* properties first");
    ClientIntelligence intellgence;
    switch (clientIntelligence) {
      case "BASIC":
        intellgence = ClientIntelligence.BASIC;
        break;
      case "TOPOLOGY_AWARE":
        intellgence = ClientIntelligence.TOPOLOGY_AWARE;
        break;
      default:
        intellgence = ClientIntelligence.HASH_DISTRIBUTION_AWARE;
        break;
    }
    builder.addServers(clientServerList).security().authentication().username(user).password(passwd)
        .saslMechanism(clientSaslMechanism).ssl().sniHostName(sni).trustStorePath(caCert)
        .clientIntelligence(intellgence);

    java.io.File hotrodConfig = new java.io.File(hotrodConfigFile);

    if (hotrodConfig.exists()) {
      LOGGER.infof("Read HotRod configuration from: %s", hotrodConfig.getAbsolutePath());
      try (Reader r = new BufferedReader(new FileReader(hotrodConfigFile))) {
        Properties p = new Properties();
        p.load(r);
        builder.withProperties(p);
      } catch (IOException e) {
        LOGGER.infof("Exception {} occuerd while reading confg file %s", e.getMessage(), hotrodConfigFile);
        LOGGER.debug("Exception occured while loading properties", e);
      }
    } else {
      LOGGER.warnf("HotRod Client %s not found, tuning skipped", hotrodConfig.getAbsolutePath());
    }

    ProtoStreamMarshaller marshaller = new ProtoStreamMarshaller();
    marshaller.register(new DataObjectSerializationContextInitializer());
    builder.marshaller(marshaller);

    // builder.marshaller(new JavaSerializationMarshaller());
    // StringTokenizer whiteList = new StringTokenizer(java_serial_whitelist, ",");
    // while (whiteList.hasMoreTokens()) {
    //   builder.addJavaSerialWhiteList(whiteList.nextToken());
    // }
    Configuration config = builder.build();
    LOGGER.debugf("RemoteCache configuration: %s", config.properties());
    RemoteCacheManager manager = new RemoteCacheManager(config);
    return manager;
  }
}
