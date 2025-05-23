/*
 * Copyright (c) 2024 Uladzislau Lailo.
 *
 * All rights reserved.
 *
 * This source code, and any associated documentation, is the intellectual property of Uladzislau Lailo.
 * Unauthorized copying, modification, distribution, or any form of reuse of this code, in whole or in part,
 * without explicit permission from the copyright holder is strictly prohibited, except where explicitly permitted
 * under applicable open-source licenses (if any).
 *
 * Licensed use:
 * If the code is provided under an open-source license, you must follow the terms of that license, which can be found in the LICENSE file.
 * For any permissions not covered by the license or any inquiries about usage, please contact: [lailo.vlad@gmail.com]
 */

package dev.vlxd.storageservice.service.storage;

import dev.vlxd.storageservice.constants.ArchiveType;
import dev.vlxd.storageservice.exception.StorageException;
import dev.vlxd.storageservice.service.archive.ArchiveManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.*;

@Service
@Profile("local")
public class LocalStorageService implements IStorageService {

    private final Path root;
    private final ArchiveManagerService archiveService;

    @Autowired
    public LocalStorageService(
            @Value("${storage.root}") String rootPath,
            ArchiveManagerService archiveService
    ) {
        this.root = Paths.get(rootPath);
        this.archiveService = archiveService;
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }

    @Override
    public String uploadFile(InputStream inputStream, String filename) {
        Path dest = resolvePath(filename);

        if (!dest.startsWith(this.root)) {
            throw new StorageException(
                    "Can not store file outside storage root directory");
        }

        try {
            try {
                Files.createDirectories(dest);
            } catch (FileAlreadyExistsException ignored) {
            }
            Files.copy(inputStream, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Failed to store file", e);
        }

        return filename;
    }

    @Override
    public Resource loadAsResource(String fileId) {
        try {
            UrlResource resource = new UrlResource(resolvePath(fileId).toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new StorageException("Failed to read file");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new StorageException("Failed to read file", e);
        }
    }

    @Override
    public void archiveFile(ArchiveType archiveType, String fileId, OutputStream outputStream) {
        Path path = resolvePath(fileId);

        if (!path.startsWith(this.root)) {
            throw new StorageException(
                    "Can not archive file outside storage root directory");
        }

        archiveService.archive(archiveType, path.toFile(), path.toFile().getName(), outputStream);
    }

    @Override
    public boolean deleteFile(String fileId) {
        try {
            Path path = resolvePath(fileId);

            if (!path.startsWith(this.root)) {
                throw new StorageException(
                        "Can not delete file outside storage root directory");
            }

            return FileSystemUtils.deleteRecursively(path);
        } catch (IOException e) {
            throw new StorageException("Failed to delete file", e);
        }
    }

    @Override
    public void checkFile(String fileId) {
        Path path = resolvePath(fileId);

        if (!path.startsWith(this.root)) {
            throw new StorageException(
                    "Can not access file outside storage root directory");
        }

        if (!path.toFile().exists()) {
            throw new StorageException(
                    "File does not exist");
        }
    }

    public Path resolvePath(String filePath) {
        return this.root.resolve(filePath);
    }

    public Path relativePath(Path filePath) {
        return this.root.relativize(filePath);
    }
}
