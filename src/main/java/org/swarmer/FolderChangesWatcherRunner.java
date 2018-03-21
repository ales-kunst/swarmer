package org.swarmer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.watcher.FolderChangesWatcher;

public class FolderChangesWatcherRunner extends Runner {
   private static final Logger LOG = LogManager.getLogger(FolderChangesWatcherRunner.class);

   public FolderChangesWatcherRunner() {
      super();
   }

   @Override
   public void runLocal() throws Exception {
      FolderChangesWatcher folderChangesWatcher = new FolderChangesWatcher();
      folderChangesWatcher.watch();
   }

   @Override
   protected String getName() {
      return FolderChangesWatcherRunner.class.getName();
   }
}
