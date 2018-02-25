package com.swarmer.watcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.SwarmConfig;
import org.swarmer.context.SwarmerContext;
import org.swarmer.util.FileUtil;

public class FolderChangesWatcher {

	private static final Logger LOG = LogManager.getLogger(FolderChangesWatcher.class);

	private Map<WatchKey, SwarmConfig> keySwarmConfigMap = new HashMap<WatchKey, SwarmConfig>();

	public FolderChangesWatcher() {
	}

	public void start() throws IOException, InterruptedException {
		WatchService watchService = createDefaultWatchService();
		try {
			watchLoop(watchService);
		} finally {
			watchService.close();
		}
	}

	private void watchLoop(WatchService watchService) throws IOException, InterruptedException {
		registerFolders(watchService, SwarmerContext.instance().getSwarmConfigs());
		LOG.info("Folder changes watcher started.");
		while (true) {
			WatchKey queuedKey = watchService.take();

			for (WatchEvent<?> watchEvent : queuedKey.pollEvents()) {
				Path watchEventPath = (Path) watchEvent.context();
				LOG.trace("Event... kind={}, count={}, context={} Context type={}", watchEvent.kind(),
						watchEvent.count(), watchEvent.context(), (watchEventPath).getClass());

				if ((watchEvent.kind() == StandardWatchEventKinds.ENTRY_MODIFY) && watchEventPath.toFile().isFile()) {
					String srcFilename = ((Path) watchEvent.context()).toFile().getName();
					boolean fileMatchesPattern = keySwarmConfigMap.get(queuedKey).matchesFilePattern(srcFilename);
					Path srcPath = getSrcPath(queuedKey, watchEvent);

					if (!FileUtil.isFileLocked(srcPath) && fileMatchesPattern) {
						Path destPath = getDestPath(queuedKey, watchEvent);
						LOG.trace("File [{}] ready for copying [size: {}]", srcPath.toString(),
								srcPath.toFile().length());
						if (shouldCopyFile(srcPath, destPath)) {
							FileUtil.nioBufferCopy(srcPath.toFile(), destPath.toFile());
						}
					}
				}
			}
			if (!queuedKey.reset()) {
				LOG.info("Removed WatchKey {}.", queuedKey.toString());
				keySwarmConfigMap.remove(queuedKey);
			}
			if (keySwarmConfigMap.isEmpty()) {
				LOG.info("Folder changes map is empty. Going out of loop for folder changes watching.",
						queuedKey.toString());
				break;
			}
		}
		LOG.warn("Folder changes watcher ended.");
	}

	private boolean shouldCopyFile(Path srcPath, Path destPath) {
		boolean shouldCopy = true;
		final File destFile = destPath.toFile();

		if (destFile.exists()) {
			final BasicFileAttributes srcBfa = FileUtil.getFileAttributes(srcPath);
			final BasicFileAttributes destBfa = FileUtil.getFileAttributes(destPath);
			
			// If source file is older destination file then we do not need to copy file, because
			// file has already been copied.
			if (srcBfa.lastModifiedTime().toMillis() < destBfa.lastModifiedTime().toMillis()) {
				shouldCopy = false;
			}
		}
		return shouldCopy;
	}

	private Path getSrcPath(WatchKey queuedKey, WatchEvent<?> watchEvent) {
		// this is not a complete path
		Path path = (Path) watchEvent.context();
		// need to get parent path
		SwarmConfig swarmConfig = keySwarmConfigMap.get(queuedKey);
		Path parentPath = swarmConfig.getSourcePath().toPath();
		// get complete path
		path = parentPath.resolve(path);

		return path;
	}

	private Path getDestPath(WatchKey queuedKey, WatchEvent<?> watchEvent) {
		// this is not a complete path
		Path path = (Path) watchEvent.context();
		// need to get parent path
		SwarmConfig swarmConfig = keySwarmConfigMap.get(queuedKey);
		Path parentPath = swarmConfig.getTargetPath().toPath();
		// get complete path
		path = parentPath.resolve(path);

		return path;
	}

	private WatchService createDefaultWatchService() throws IOException {
		return FileSystems.getDefault().newWatchService();

	}

	private void registerFolders(WatchService watchService, SwarmConfig[] swarmConfigs) throws IOException {
		for (SwarmConfig swarmConfig : swarmConfigs) {
			File srcFolder = swarmConfig.getSourcePath();
			if (srcFolder.exists() && srcFolder.isDirectory()) {
				Path path = srcFolder.toPath();
				WatchKey key = path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
						StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
				keySwarmConfigMap.put(key, swarmConfig);
			} else {
				throw new IOException("Folder [" + srcFolder.getAbsolutePath() + "] does not exist!");
			}
		}
	}
}
