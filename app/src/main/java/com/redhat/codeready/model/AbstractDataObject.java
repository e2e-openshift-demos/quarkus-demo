package com.redhat.codeready.model;

import java.util.Objects;
import java.util.UUID;

import io.vertx.core.json.JsonObject;

public abstract class AbstractDataObject implements java.io.Serializable {

  private static final long serialVersionUID = -3809134255585721773L;

  private UUID id;

  private UUID version;

  protected AbstractDataObject() {
    id = null;
    version = null;
  }

  protected AbstractDataObject(final UUID id) {
    this.id = id;
    version = UUID.randomUUID();
  }

  public void setId(UUID id) {
    if (this.id != null) {
      throw new UnsupportedOperationException(
          getClass().getName() + "::setId(): Id member is FINAL and can't be updated");
    }
    this.id = id;
  }

  public void setVersion(UUID version) {
    if (this.version != null) {
      throw new UnsupportedOperationException(
          getClass().getName() + "::setVersion(): version member is FINAL and can't be updated");
    }
    this.version = version;
  }

  public UUID getId() {
    return id;
  }

  public UUID getVersion() {
    return version;
  }

  public void newVersion() {
    version = UUID.randomUUID();
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    if (getId() != null) {
      json.put("id", getId().toString());
    }
    if (getVersion() != null) {
      json.put("version", getVersion().toString());
    }
    return json;
  }

  @Override
  public String toString() {
    final StringBuffer buffer = new StringBuffer();
    buffer.append("{ ").append(getClass().getName()).append(": [id=").append(getId()).append(", version=")
        .append(getVersion()).append(toStringBufferOthermembers()).append("] }");
    return buffer.toString();
  }

  @Override 
  public boolean equals(Object o) {
    if(o == null) {
      return false;
    }
    if (this == o) {
      return true;
    }
    if (o instanceof AbstractDataObject) {
      if (id != null && id.equals(o)) {
        return true;
      }
      return false;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, version);
  }
  
  protected abstract StringBuffer toStringBufferOthermembers();
}
