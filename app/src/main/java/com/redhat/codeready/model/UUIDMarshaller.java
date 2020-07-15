package com.redhat.codeready.model;

import java.io.IOException;
import java.util.UUID;

import org.infinispan.protostream.MessageMarshaller;

public class UUIDMarshaller implements MessageMarshaller<UUID> {

  @Override
  public Class<? extends UUID> getJavaClass() {
    return UUID.class;
  }

  @Override
  public String getTypeName() {
    return "dataobject.UUID";
  }

  @Override
  public UUID readFrom(ProtoStreamReader reader) throws IOException {
    final long most = reader.readLong("mostSigBits");
    final long least = reader.readLong("leastSigBits");
    return new UUID(most, least);
  }

  @Override
  public void writeTo(ProtoStreamWriter writer, UUID id) throws IOException {
    writer.writeLong("mostSigBits", id.getMostSignificantBits());
    writer.writeLong("leastSigBits", id.getLeastSignificantBits());
  }
  
}