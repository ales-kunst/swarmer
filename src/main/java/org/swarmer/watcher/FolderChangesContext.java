package org.swarmer.watcher;

import org.swarmer.context.SwarmConfig;
import org.swarmer.context.SwarmInstanceData;

import java.nio.file.WatchKey;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FolderChangesContext {
   private Map<WatchKey, SwarmInstanceData> watchKeySwarmInstanceMap;

   public FolderChangesContext() {
      watchKeySwarmInstanceMap = new HashMap<WatchKey, SwarmInstanceData>();
   }

   public void addSwarmInstance(WatchKey key, SwarmInstanceData swarmInstanceData) {
      watchKeySwarmInstanceMap.put(key, swarmInstanceData);
   }

   public SwarmConfig getSwarmConfig(WatchKey key) {
      return getSwarmInstance(key).getSwarmConfig();
   }

   public SwarmInstanceData getSwarmInstance(WatchKey key) {
      return watchKeySwarmInstanceMap.get(key);
   }

   public boolean isEmpty() {
      return watchKeySwarmInstanceMap.isEmpty();
   }

   public SwarmInstanceData remove(WatchKey key) {
      return watchKeySwarmInstanceMap.remove(key);
   }

   public void reset() {
      Iterator<Map.Entry<WatchKey, SwarmInstanceData>> entryIterator = watchKeySwarmInstanceMap.entrySet().iterator();

      while (entryIterator.hasNext()) {
         Map.Entry<WatchKey, SwarmInstanceData> entry = entryIterator.next();
         entryIterator.remove();
      }
   }
}
