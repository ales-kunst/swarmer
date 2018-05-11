package org.swarmer.test.entity;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Car {
   @JsonProperty
   private List<String>        colors          = new ArrayList<String>();
   @JsonProperty("carModel")
   private int                 model;
   @JsonProperty
   private String              name;
   private Map<String, Object> otherProperties = new HashMap<String, Object>();
   @JsonProperty
   private String              price;

   public void addColor(String color) {
      colors.add(color);
   }

   @JsonAnyGetter
   public Map<String, Object> any() {
      return otherProperties;
   }

   @JsonAnySetter
   public void set(String name, Object value) {
      otherProperties.put(name, value);
   }

   public void setModel(int model) {
      this.model = model;
   }

   public void setName(String name) {
      this.name = name;
   }

   @Override
   public String toString() {
      return "Car [name=" + name + ", model=" + model + ", price=" + price
             + ", colors=" + colors + ", otherProperties=" + otherProperties + "]";
   }
}
