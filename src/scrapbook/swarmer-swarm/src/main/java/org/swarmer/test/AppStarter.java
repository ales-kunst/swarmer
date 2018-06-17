package org.swarmer.test;

import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@Singleton
@Startup
public class AppStarter {

   private static final Logger log = Logger.getLogger(AppStarter.class);

   @PostConstruct
   private void init() {
      log.info("HELLO LOG");
      System.out.println("IIIIIIIIIIIII--------------------------------------------------------");
   }

   @PreDestroy
   private void shutdown() {
      System.out.println("DDDDDDDDDDDDDDDDDDD--------------------------------------------------------");
   }
}
