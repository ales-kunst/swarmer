package org.swarmer.util;

import org.json.JSONArray;
import org.json.JSONObject;

public class ConsulUtil {

   public static String getServiceId(String jsonText, String hostname, int port) {
      JSONObject jsonObj           = new JSONObject(jsonText);
      JSONArray  consulArray       = jsonObj.getJSONArray("Consul");
      String     serviceIdTextPart = hostname + ":" + Integer.toString(port);
      for (int index = 0; index < consulArray.length(); index++) {
         JSONObject elem        = (JSONObject) consulArray.get(index);
         JSONArray  checksArray = elem.getJSONArray("Checks");
         for (int checksIndex = 0; checksIndex < checksArray.length(); checksIndex++) {
            JSONObject checkElem      = (JSONObject) checksArray.get(checksIndex);
            String     serviceIdValue = checkElem.getString("ServiceID");
            System.out.println("Service Id: " + serviceIdValue);
            if (!serviceIdValue.isEmpty() && serviceIdValue.contains(serviceIdTextPart)) {
               return serviceIdValue;
            }
         }
      }
      return null;
   }

   private ConsulUtil() { }
}
