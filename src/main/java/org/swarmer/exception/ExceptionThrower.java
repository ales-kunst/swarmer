package org.swarmer.exception;

public class ExceptionThrower {

   public static void throwRuntimeError(Throwable e) {
      throw new RuntimeException(e);
   }

   public static void throwRuntimeError(String text) {
      throw new RuntimeException(text);
   }

   public static void throwSwarmerCtxException(String msg) throws SwarmerContextException {
      throw new SwarmerContextException(msg);
   }

   public static void throwIllegalArgumentException(String msg) {
      throw createIllegalArgumentException(msg);
   }

   public static IllegalArgumentException createIllegalArgumentException(String errMsg, Object... args) {
      return new IllegalArgumentException(String.format(errMsg, (Object[]) args));
   }



   public static void throwValidationException(String msg) throws ValidationException {
      throw new ValidationException(msg);
   }

   public static void throwSwarmerException(String msg) throws SwarmerException {
      throw createSwarmerException(msg);
   }

   public static SwarmerException createSwarmerException(String errMsg, Object... args) {
      return new SwarmerException(String.format(errMsg, (Object[]) args));
   }

   private ExceptionThrower() {}
}
