package com.redhat.codeready.model;

import java.io.UncheckedIOException;

import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;

public class DataObjectSerializationContextInitializer implements SerializationContextInitializer {

  @Override
  public String getProtoFileName() {
    return "dataobject.proto";
  }

  @Override
  public String getProtoFile() throws UncheckedIOException {
    return FileDescriptorSource.getResourceAsString(getClass(), "/META-INF/" + getProtoFileName());
  }

  @Override
  public void registerSchema(SerializationContext serCtx) {
    serCtx.registerProtoFiles(FileDescriptorSource.fromString(getProtoFileName(), getProtoFile()));
  }

  @Override
  public void registerMarshallers(SerializationContext serCtx) {
    serCtx.registerMarshaller(new QuarkusDemoDataObjectMarshaller());
    serCtx.registerMarshaller(new UUIDMarshaller());
  }
}