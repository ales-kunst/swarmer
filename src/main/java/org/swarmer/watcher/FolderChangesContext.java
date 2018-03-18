package org.swarmer.watcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.SwarmConfig;
import org.swarmer.context.SwarmDeployment;

import java.io.File;
import java.nio.file.WatchKey;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FolderChangesContext {
   private final static Integer INITIAL_REF_VALUE = 1;
   private static final Logger  LOG               = LogManager.getLogger(FolderChangesContext.class);

   private Map<File, Integer>             checkedFilesForLockingRef;
   private Map<WatchKey, SwarmDeployment> watchKeySwarmDeploymentMap;

   public FolderChangesContext() {
      watchKeySwarmDeploymentMap = new HashMap<>();
      checkedFilesForLockingRef = new HashMap<>();
   }

   public boolean addCheckedFileForLocking(File checkedFile) {
      boolean wasAdded             = false;
      File    fileToRemoveFromList = findCheckedFileForLocking(checkedFile);
      if (fileToRemoveFromList != null) {
         Integer refValue = checkedFilesForLockingRef.get(fileToRemoveFromList);
         ++refValue;
         checkedFilesForLockingRef.put(fileToRemoveFromList, refValue);
         LOG.trace("Increased element value [{} -> {}] in checkedFilesForLocking collection.",
                   fileToRemoveFromList, refValue);
      } else {
         checkedFilesForLockingRef.put(checkedFile, INITIAL_REF_VALUE);
         wasAdded = true;
         LOG.trace("Added element value [{} -> {}] to checkedFilesForLocking collection.",
                   checkedFile, INITIAL_REF_VALUE);
      }

      return wasAdded;
   }

   public void addSwarmDeployment(WatchKey key, SwarmDeployment swarmDeployment) {
      watchKeySwarmDeploymentMap.put(key, swarmDeployment);
   }

   public SwarmDeployment getSwarmDeployment(WatchKey key) {
      return watchKeySwarmDeploymentMap.get(key);
   }

   public File findCheckedFileForLocking(File checkedFile) {
      File fileFromList = null;
      for (Map.Entry<File, Integer> entry : checkedFilesForLockingRef.entrySet()) {
         String entryAbsolutePath = entry.getKey().getAbsolutePath();
         if (entryAbsolutePath.equalsIgnoreCase(checkedFile.getAbsolutePath())) {
            fileFromList = entry.getKey();
            break;
         }
      }

      return fileFromList;
   }

   public SwarmConfig getSwarmConfig(WatchKey key) {
      return getSwarmInstance(key).getSwarmConfig();
   }

   public SwarmDeployment getSwarmInstance(WatchKey key) {
      return watchKeySwarmDeploymentMap.get(key);
   }

   public boolean hasCheckedFileForLocking(File checkedFile) {
      return (findCheckedFileForLocking(checkedFile) != null);
   }

   public boolean isEmpty() {
      return watchKeySwarmDeploymentMap.isEmpty();
   }

   public SwarmDeployment remove(WatchKey key) {
      return watchKeySwarmDeploymentMap.remove(key);
   }

   public boolean removeCheckedFileForLocking(File checkedFile) {
      boolean wasRemoved           = false;
      File    fileToRemoveFromList = findCheckedFileForLocking(checkedFile);
      if (fileToRemoveFromList != null) {
         Integer refValue = checkedFilesForLockingRef.get(fileToRemoveFromList);
         if (refValue.equals(INITIAL_REF_VALUE)) {
            LOG.trace("Removed element [{} -> {}] from checkedFilesForLocking collection.", fileToRemoveFromList,
                      refValue);
            checkedFilesForLockingRef.remove(fileToRemoveFromList);
            wasRemoved = true;
         } else {
            --refValue;
            checkedFilesForLockingRef.put(fileToRemoveFromList, refValue);
            LOG.trace("Decreased element value [{} -> {}] from checkedFilesForLocking collection.",
                      fileToRemoveFromList, refValue);
         }
      }
      return wasRemoved;
   }

   public void reset() {
      Iterator<Map.Entry<WatchKey, SwarmDeployment>> entryIterator = watchKeySwarmDeploymentMap.entrySet().iterator();

      while (entryIterator.hasNext()) {
         Map.Entry<WatchKey, SwarmDeployment> entry = entryIterator.next();
         entryIterator.remove();
      }
   }
}
