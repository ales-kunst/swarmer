package org.swarmer.exception;

public class SwarmerException extends Exception {
   public SwarmerException() { super(); }

   public SwarmerException(String message) { super(message); }

   public SwarmerException(String message, Throwable cause) { super(message, cause); }

   public SwarmerException(Throwable cause) { super(cause); }
}
