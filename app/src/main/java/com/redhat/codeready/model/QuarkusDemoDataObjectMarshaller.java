package com.redhat.codeready.model;

import java.io.IOException;
import java.util.UUID;

import org.infinispan.protostream.MessageMarshaller;

public class QuarkusDemoDataObjectMarshaller implements MessageMarshaller<QuarkusDemoDataObject> {

  @Override
  public Class<? extends QuarkusDemoDataObject> getJavaClass() {
    return QuarkusDemoDataObject.class;
  }

  @Override
  public String getTypeName() {
    return "dataobject.QuarkusDemoDataObject";
  }

  @Override
  public QuarkusDemoDataObject readFrom(ProtoStreamReader reader) throws IOException {
 
    final long id_most = reader.readLong("id_mostSigBits");
    final long id_least = reader.readLong("id_leastSigBits");
    final long ver_most = reader.readLong("version_mostSigBits");
    final long ver_least = reader.readLong("version_leastSigBits");
    final String name = reader.readString("name");

    final UUID id = new UUID(id_most, id_least);
    final UUID version = new UUID(ver_most, ver_least);

    final QuarkusDemoDataObject qdo = new QuarkusDemoDataObject();
    
    qdo.setId(id);
    qdo.setVersion(version);
    qdo.setName(name);
    return qdo;
  }

  @Override
  public void writeTo(ProtoStreamWriter writer, QuarkusDemoDataObject dataObject) throws IOException {
    final UUID id = dataObject.getId();
    final UUID version = dataObject.getVersion();
    writer.writeLong("id_mostSigBits", id.getMostSignificantBits());
    writer.writeLong("id_leastSigBits", id.getLeastSignificantBits());
    writer.writeLong("version_mostSigBits", version.getMostSignificantBits());
    writer.writeLong("version_leastSigBits", version.getLeastSignificantBits());
    writer.writeString("name", dataObject.getName());
  }
}