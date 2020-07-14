package com.redhat.codeready;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;

@Provider
public class LoggingFilter implements ContainerRequestFilter {
  private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class);

  @Context
  UriInfo info;

  @Context
  HttpServerRequest request;

  @Override
  public void filter(ContainerRequestContext context) {

    final String method = context.getMethod();
    final String path = info.getPath();
    final SocketAddress address = request.remoteAddress();

    LOGGER.infof("HTTP: %s %s from IP %s", method, path, address);
  }
}
