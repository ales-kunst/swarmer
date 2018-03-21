package org.swarmer.exception;

public class ExceptionThrower {

   public static void throwRuntimeError(Exception e) {
      throw new RuntimeException(e);
   }

   public static void throwSwarmerCtxException(String msg) throws SwarmerContextException {
      throw new SwarmerContextException(msg);
   }

   public static void throwSwarmerException(String msg) throws SwarmerException {
      throw new SwarmerException(msg);
   }

   public static void throwValidationException(String msg) throws ValidationException {
      throw new ValidationException(msg);
   }

   public static void throwIllegalArgumentException(String msg) {
      throw new IllegalArgumentException(msg);
   }

   private ExceptionThrower() {}
}
