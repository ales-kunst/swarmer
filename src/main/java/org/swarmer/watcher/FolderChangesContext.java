package org.swarmer.watcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.SwarmConfig;
import org.swarmer.context.SwarmInstanceData;

import java.io.File;
import java.nio.file.WatchKey;
import java.util.*;

public class FolderChangesContext {
   private static final Logger LOG = LogManager.getLogger(FolderChangesContext.class);

   private List<File>                       checkedFilesForLocking;
   private Map<WatchKey, SwarmInstanceData> watchKeySwarmInstanceMap;

   public FolderChangesContext() {
      watchKeySwarmInstanceMap = new HashMap<WatchKey, SwarmInstanceData>();
      checkedFilesForLocking = new ArrayList<>();
   }

   public boolean addCheckedFileForLocking(File checkedFile) {
      boolean wasAdded = false;
      if (findCheckedFileForLocking(checkedFile) == null) {
         checkedFilesForLocking.add(checkedFile);
         wasAdded = true;
      }
      return wasAdded;
   }

   public void addSwarmInstance(WatchKey key, SwarmInstanceData swarmInstanceData) {
      watchKeySwarmInstanceMap.put(key, swarmInstanceData);
   }

   public File findCheckedFileForLocking(File checkedFile) {
      File fileFromList = null;
      Optional<File> searchResult = checkedFilesForLocking.stream()
                                                          .filter(file -> file.getAbsolutePath()
                                                                              .equalsIgnoreCase(
                                                                                      checkedFile.getAbsolutePath()))
                                                          .findFirst();
      if (searchResult.isPresent()) {
         fileFromList = searchResult.get();
      }

      return fileFromList;
   }

   public SwarmConfig getSwarmConfig(WatchKey key) {
      return getSwarmInstance(key).getSwarmConfig();
   }

   public SwarmInstanceData getSwarmInstance(WatchKey key) {
      return watchKeySwarmInstanceMap.get(key);
   }

   public boolean hasCheckedFileForLocking(File checkedFile) {
      return (findCheckedFileForLocking(checkedFile) != null);
   }

   public boolean isEmpty() {
      return watchKeySwarmInstanceMap.isEmpty();
   }

   public SwarmInstanceData remove(WatchKey key) {
      return watchKeySwarmInstanceMap.remove(key);
   }

   public boolean removeCheckedFileForLocking(File checkedFile) {
      boolean wasRemoved           = false;
      File    fileToRemoveFromList = findCheckedFileForLocking(checkedFile);
      if (fileToRemoveFromList != null) {
         checkedFilesForLocking.remove(fileToRemoveFromList);
         wasRemoved = true;
         LOG.debug("Removed file [{}] from checkedFilesForLocking.", fileToRemoveFromList);
      }
      return wasRemoved;
   }

   public void reset() {
      Iterator<Map.Entry<WatchKey, SwarmInstanceData>> entryIterator = watchKeySwarmInstanceMap.entrySet().iterator();

      while (entryIterator.hasNext()) {
         Map.Entry<WatchKey, SwarmInstanceData> entry = entryIterator.next();
         entryIterator.remove();
      }
   }
}
