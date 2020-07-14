package com.redhat.codeready.model;

import java.util.UUID;

import io.vertx.core.json.JsonObject;

public class QuarkusDemoDataObject extends AbstractDataObject {

  private static final long serialVersionUID = 1623446833326259234L;

  private String name;

  public QuarkusDemoDataObject() {
    super();
  }

  public QuarkusDemoDataObject(String name) {
    this(UUID.randomUUID(), name);
  }

  public QuarkusDemoDataObject(UUID id, String name) {
    super(id);
    setName(name);
  }

  @Override
  protected StringBuffer toStringBufferOthermembers() {
    return new StringBuffer().append(", name=").append(getName());
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public JsonObject toJson() {
    JsonObject base = super.toJson();
    if (getName() != null) {
      base.put("name", name);
    }
    return base;
  }
}
