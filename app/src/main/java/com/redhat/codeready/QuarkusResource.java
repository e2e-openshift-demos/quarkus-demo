package com.redhat.codeready;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.redhat.codeready.mgmt.Counter;
import com.redhat.codeready.mgmt.CustomStatus;
import com.redhat.codeready.mgmt.CustomStatusService;
import com.redhat.codeready.model.AbstractDataObject;
import com.redhat.codeready.model.QuarkusDemoDataObject;
import com.redhat.codeready.service.QuarkusDataService;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.JsonObject;

@Liveness
@ApplicationScoped
class QuarkusResourceChecker implements HealthCheck {
  @Inject
  QuarkusResource service;

  @Override
  public HealthCheckResponse call() {
    return HealthCheckResponse.named(service.getName()).up().withData("type", CustomStatus.TYPE_REST)
        .withData("bind", "/quarkus").build();
  }
}

@Path("/quarkus")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class QuarkusResource implements CustomStatus {
  private static final Logger LOGGER = Logger.getLogger(QuarkusResource.class);

  @Inject
  CustomStatusService customStatus;

  @Inject
  QuarkusDataService dataService;

  private Counter counter = new Counter();

  private boolean alive = false;

  void onStart(@Observes StartupEvent event) {
    alive = true;
    customStatus.register(this, true);
  }

  @GET
  @Path("/{id}")
  public Response get(@PathParam UUID id) {
    LOGGER.debugf("Ask DataService for id %s", id);
    AbstractDataObject ado = dataService.get(id);
    if (ado == null) {
      LOGGER.debugf("DataObject for id %s not found. response HTTP.code: %d", id, Status.NOT_FOUND.getStatusCode());
      counter.success();
      return Response.status(Status.NOT_FOUND).build();
    } else {
      LOGGER.debugf("DataService returned object: %s, response HTTP.code: %s", ado, Status.OK.getStatusCode());
      counter.success();
      return Response.ok(ado).build();
    }
  }

  @DELETE
  @Path("/{id}")
  public Response delete(@PathParam UUID id) {
    LOGGER.debugf("Ask DataService to delete object with id %s", id);
    if (dataService.has(id)) {
      LOGGER.debugf("DataObject for id %s found, deleting, response HTTP.code: %d", id,
          Status.NO_CONTENT.getStatusCode());
      dataService.delete(id);
      counter.success();
      return Response.ok().status(Status.NO_CONTENT).build();
    } else {
      LOGGER.debugf("DataObject for id %s not found, operation skipped, response HTTP.code: %d", id,
          Status.NOT_MODIFIED.getStatusCode());
      counter.success();
      return Response.notModified().build();
    }
  }

  @POST
  @Path("")
  public Response create(@Valid QuarkusDemoDataObject ado, @Context UriInfo uri) {
    LOGGER.debugf("Ask DataService to Create DataObject %s", ado);
    dataService.create(ado);
    UriBuilder builder = uri.getAbsolutePathBuilder().path(ado.getId().toString());
    LOGGER.debugf("New DataObject created with URI %s, response HTTP.code: %s", builder.build().toString(),
        Status.CREATED.getStatusCode());
    counter.success();
    return Response.created(builder.build()).build();
  }

  @PUT
  @Path("")
  public Response update(@Valid QuarkusDemoDataObject ado) {
    LOGGER.debugf("Ask DataSerice to update for object %s", ado);
    UUID id = ado.getId();
    if (dataService.has(ado)) {
      if (dataService.update(ado)) {
        LOGGER.debugf("DataObject for id %s found, updated, response HTTP.code: %s", id,
            Status.NO_CONTENT.getStatusCode());
        counter.success();
        return Response.status(Response.Status.NO_CONTENT).build();
      } else {
        LOGGER.debugf("DataObject for id %s found, but version mismatch, response HTTP.code: %s", id,
            Status.CONFLICT.getStatusCode());
        counter.success();
        return Response.status(Status.CONFLICT).build();
      }
    } else {
      LOGGER.debugf("DataObject for id %s not found, operation skipped, response HTTP.code: %s", id,
          Status.NOT_MODIFIED.getStatusCode());
      counter.success();
      return Response.notModified().build();
    }
  }

  @Override
  public boolean getAlive() {
    return alive;
  }

  @Override
  public boolean getReady() {
    return dataService.getReady() && getAlive();
  }

  @Override
  public JsonObject getStatus() {
    JsonObject status = new JsonObject();
    status.put("requests", new JsonObject().put("all", counter.getCount()).put("success", counter.getSuccessCount())
        .put("errors", counter.getErrorsCount()));
    return status;
  }

  @Override
  public String getType() {
    return CustomStatus.TYPE_REST;
  }
}