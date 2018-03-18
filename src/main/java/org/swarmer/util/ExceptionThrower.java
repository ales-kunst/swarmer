package org.swarmer.util;

public class ExceptionThrower {

   public static void throwRuntimeError(Exception e) {
      throw new RuntimeException(e);
   }

   public static void throwSwarmerCtxException(String msg) throws SwarmerContextException {
      throw new SwarmerContextException(msg);
   }

   public static void throwValidationException(String msg) throws ValidationException {
      throw new ValidationException(msg);
   }

   private ExceptionThrower() {}
}
