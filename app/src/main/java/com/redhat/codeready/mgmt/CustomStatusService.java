package com.redhat.codeready.mgmt;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@Liveness
class CustomStatusServiceChecker implements HealthCheck {

  @Inject
  CustomStatusService service;

  @Override
  public HealthCheckResponse call() {
    return HealthCheckResponse.named(service.getName()).up().withData("type", CustomStatus.TYPE_REST)
        .withData("bind", "/status").build();
  }
}

@ApplicationScoped
@Path("/status")
public class CustomStatusService implements CustomStatus {
  private static final Logger LOGGER = LoggerFactory.getLogger(CustomStatusService.class);

  private Counter counts;
  private CustomStatus root;
  private Set<CustomStatus> components;

  public CustomStatusService() {
    counts = new Counter();
    register(this);
  }

  public void register(CustomStatus component) {
    register(component, false);
  }

  public synchronized void register(CustomStatus component, boolean root) {
    if (root) {
      this.root = component;
    } else {
      getComponents().add(component);
    }
  }

  @GET
  @Path("/alive")
  @Produces(MediaType.TEXT_PLAIN)
  public JsonObject alive() {
    counts.success();
    return root.getAlive() ? CustomStatus.UP : CustomStatus.DOWN;
  }

  @GET
  @Path("/ready")
  @Produces(MediaType.TEXT_PLAIN)
  public JsonObject ready() {
    Set<CustomStatus> comps;
    synchronized (this) {
      comps = new java.util.HashSet<CustomStatus>(components);
    }
    comps.add(root);
    boolean ready = true;
    for (CustomStatus c : comps) {
      ready = ready && c.getReady();
      if (!ready) {
        LOGGER.warn("Component {} is not ready", c.getName());
      } else {
        LOGGER.warn("Component {} is ready", c.getName());
      }
    }
    counts.success();
    return ready ? CustomStatus.UP : CustomStatus.DOWN;
  }

  @GET
  @Path("")
  @Produces(MediaType.APPLICATION_JSON)
  public String status(@QueryParam("extended") boolean extended) {
    JsonArray status = new JsonArray();
    Set<CustomStatus> comps;
    synchronized (this) {
      comps = new java.util.HashSet<CustomStatus>(components);
    }
    if (root != null) {
      comps.add(root);
    }
    for (CustomStatus c : comps) {
      JsonObject s = new JsonObject();
      status.add(s);
      if (c == root) {
        s.put("ROOT", true);
      }
      s.put("name", c.getName());
      s.put("ready", c.getReady());
      if (extended) {
        s.put("details", c.getStatus());
      }
    }
    counts.success();
    return new JsonObject().put("status", status).toString();
  }

  private synchronized Set<CustomStatus> getComponents() {
    if (components == null) {
      components = new java.util.HashSet<CustomStatus>();
    }
    return components;
  }

  public boolean getReady() {
    return true;
  }

  public boolean getAlive() {
    return true;
  }

  @Override
  public JsonObject getStatus() {
    return CustomStatus.UP;
  }

}
