package org.swarmer.exception;

public class SwarmerContextException extends Exception {

   public SwarmerContextException() { super(); }

   public SwarmerContextException(String message) { super(message); }

   public SwarmerContextException(String message, Throwable cause) { super(message, cause); }

   public SwarmerContextException(Throwable cause) { super(cause); }
}
