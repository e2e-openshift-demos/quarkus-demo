package com.redhat.codeready.mgmt;

import io.vertx.core.json.JsonObject;

public interface CustomStatus {
  public boolean getReady();
  public boolean getAlive();
  public JsonObject getStatus();
  default public String getType() {
    return TYPE_GENERIC;
  }

  default public String getName() {
    return getClass().getSimpleName();
  };

  public JsonObject UP = new JsonObject().put("status", "UP");
  public JsonObject DOWN = new JsonObject().put("status", "DOWN");
  public String TYPE_DATAGRID = "datagrid";
  public String TYPE_GENERIC = "generic";
  public String TYPE_REST = "rest";
}
