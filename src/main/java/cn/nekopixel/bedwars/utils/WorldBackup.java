package cn.nekopixel.bedwars.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;

public class WorldBackup {
    private final Plugin plugin;
    private final Logger logger;
    private final Path serverRoot;
    private final Path backupDir;
    private final Path worldDir;
    private final Path backupWorldDir;
    private final Path hashFile;

    public WorldBackup(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.serverRoot = Bukkit.getWorldContainer().toPath();
        this.backupDir = serverRoot.resolve("backup");
        this.worldDir = serverRoot.resolve("world");
        this.backupWorldDir = backupDir.resolve("world");
        this.hashFile = backupDir.resolve("world.sha256");
    }

    public boolean backupWorld() {
        try {
            if (!Files.exists(backupDir)) {
                logger.info("Creating backup directory...");
                Files.createDirectory(backupDir);
            }

            if (!Files.exists(backupWorldDir)) {
                if (Files.exists(worldDir)) {
                    logger.info("Backing up current world...");
                    copyWorld(worldDir, backupWorldDir);
                    saveWorldHash(backupWorldDir);
                    logger.info("World backup completed");
                    return true;
                } else {
                    logger.severe("Current server does not have world directory");
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            logger.severe("Error occurred while processing backup: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean restoreWorldOnLoad() {
        try {
            if (!Files.exists(backupWorldDir)) {
                logger.info("Backup world does not exist, skipping restoration");
                return false;
            }
            
            if (!verifyWorldIntegrity(backupWorldDir)) {
                logger.severe("Backup integrity verification failed");
                return false;
            }

            logger.info("Integrity verification passed, restoring world files...");
            
            if (Files.exists(worldDir)) {
                deleteWorld(worldDir);
            }

            copyWorld(backupWorldDir, worldDir);
            logger.info("World files restoration completed!");
            return true;
        } catch (Exception e) {
            logger.severe("Error occurred while restoring world files: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void saveWorldHash(Path world) throws IOException {
        TreeMap<String, String> fileHashes = calculateDirectoryHash(world);
        try (BufferedWriter writer = Files.newBufferedWriter(hashFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (var entry : fileHashes.entrySet()) {
                writer.write(entry.getKey() + ":" + entry.getValue() + "\n");
            }
        }
    }

    private TreeMap<String, String> calculateDirectoryHash(Path directory) throws IOException {
        TreeMap<String, String> fileHashes = new TreeMap<>();
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!Files.isDirectory(file) && !file.getFileName().toString().endsWith(".lock")) {
                    String relativePath = directory.relativize(file).toString();
                    String hash = calculateFileHash(file);
                    fileHashes.put(relativePath, hash);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return fileHashes;
    }

    private String calculateFileHash(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int count;
            try (InputStream is = Files.newInputStream(file)) {
                while ((count = is.read(buffer)) > 0) {
                    digest.update(buffer, 0, count);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new IOException("Calculation failed: " + e.getMessage());
        }
    }

    private void copyWorld(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!file.getFileName().toString().endsWith(".lock")) {
                    Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteWorld(Path world) throws IOException {
        Files.walkFileTree(world, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean verifyWorldIntegrity(Path world) {
        try {
            if (!Files.exists(hashFile)) {
                logger.warning("Hash file not found, unable to verify integrity");
                return false;
            }

            TreeMap<String, String> storedHashes = new TreeMap<>();
            List<String> lines = Files.readAllLines(hashFile);
            for (String line : lines) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    storedHashes.put(parts[0], parts[1]);
                }
            }

            TreeMap<String, String> currentHashes = calculateDirectoryHash(world);

            if (!storedHashes.equals(currentHashes)) {
                logger.warning("World file hash mismatch, current backup may be corrupted");
                return false;
            }

            return true;
        } catch (Exception e) {
            logger.severe("Error occurred during verification: " + e.getMessage());
            return false;
        }
    }
} 