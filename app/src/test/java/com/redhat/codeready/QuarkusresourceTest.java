package com.redhat.codeready;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.is;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

import com.redhat.codeready.mgmt.CustomStatusService;
import com.redhat.codeready.model.QuarkusDemoDataObject;
import com.redhat.codeready.service.CacheInitializer;
import com.redhat.codeready.service.QuarkusDataService;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QuarkusresourceTest {

  private static final Logger LOGGER = Logger.getLogger(QuarkusresourceTest.class);

  private static final String QDO_PATH = "/quarkus";

  @Test
  public void testHealth() {
    given().when().get("/health").then().statusCode(200).body("status", is("UP")).body("checks.size()", is(4))
        .body("checks.name",
            everyItem(anyOf(is(CacheInitializer.class.getSimpleName()), is(QuarkusDataService.class.getSimpleName()),
                is(CustomStatusService.class.getSimpleName()), is(QuarkusResource.class.getSimpleName()))))
        .body("checks.status", everyItem(is("UP")));
  }

  @Test
  public void testGetDO() {
    LOGGER.warnf("Query DataService for id: %s", CacheLoader.TEST_GET.getId());
    given().when().get(QDO_PATH + "/" + CacheLoader.TEST_GET.getId()).then().statusCode(Status.OK.getStatusCode())
        .body(is(CacheLoader.TEST_GET.toJson().encode()));
  }

  @Test
  public void testGetNotExistDO() {
    QuarkusDemoDataObject qdo = new QuarkusDemoDataObject("fake");
    LOGGER.warnf("Query DataService for id: %s", qdo);
    given().when().get(QDO_PATH + "/" + qdo.getId()).then().statusCode(Status.NOT_FOUND.getStatusCode());
  }

  @Test
  public void testDeleteExistingDO() {
    LOGGER.warnf("Query DataService to delete id: %s", CacheLoader.TEST_REMOVE.getId());
    given().when().delete(QDO_PATH + "/" + CacheLoader.TEST_REMOVE.getId()).then()
        .statusCode(Status.NO_CONTENT.getStatusCode());
  }

  @Test
  public void testDeleteNotExistingDO() {
    QuarkusDemoDataObject qdo = new QuarkusDemoDataObject("fake");
    LOGGER.warnf("Query DataService to delete id: %s", qdo.getId());
    given().when().delete(QDO_PATH + "/" + qdo.getId()).then().statusCode(Status.NOT_MODIFIED.getStatusCode());
  }

  @Test
  public void testCreateDO() {
    QuarkusDemoDataObject qdo = new QuarkusDemoDataObject(getClass().getName());
    LOGGER.warnf("Asking DataService to create: %s", qdo);
    given().body(qdo).header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON).header(HttpHeaders.ACCEPT, APPLICATION_JSON)
        .when().post(QDO_PATH).then().statusCode(Status.CREATED.getStatusCode()).extract().header("Location")
        .endsWith(QDO_PATH + "/" + qdo.getId().toString());
    LOGGER.warnf("Query cache for id: {}", qdo.getId());
    given().when().get(QDO_PATH + "/" + qdo.getId()).then().statusCode(Status.OK.getStatusCode())
        .body(is(qdo.toJson().encode()));
  }

  @Test
  public void testUpdateGoodDO() {
    QuarkusDemoDataObject qdo = CacheLoader.TEST_UPDATE_GOOD;
    qdo.setName("update-good");
    LOGGER.warnf("Asking DataService for GOOD update: %s", qdo);
    given().body(qdo).header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON).header(HttpHeaders.ACCEPT, APPLICATION_JSON)
        .when().put(QDO_PATH).then().statusCode(Status.NO_CONTENT.getStatusCode());
  }

  @Test
  public void testUpdateBadDO() {
    QuarkusDemoDataObject qdo = CacheLoader.TEST_UPDATE_BAD;
    LOGGER.warnf("Asking DataService for BAD update: {}", qdo);
    qdo.newVersion();
    qdo.setName("update-bad");
    given().body(qdo).header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON).header(HttpHeaders.ACCEPT, APPLICATION_JSON)
        .when().put(QDO_PATH).then().statusCode(Status.CONFLICT.getStatusCode());
  }
}
