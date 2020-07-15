package com.redhat.codeready.mgmt;

public class Counter {
  private long success = 0;
  private long errors = 0;

  public void errors() {
    errors++;
  }

  public void success() {
    success++;
  }

  public long getCount() {
    return errors + success;
  }

  public long getErrorsCount() {
    return errors;
  }

  public long getSuccessCount() {
    return success;
  }
}
