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

package dev.vlxd.storageservice.service.archive;

import dev.vlxd.storageservice.exception.ArchiveException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ZipArchiveService implements IArchiveService {

    @Override
    public void archive(File file, String root, OutputStream outputStream) {
        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {

            archiveRecursively(file, root, zos);

            zos.flush();
        } catch (IOException e) {
            throw new ArchiveException("Failed to process ZIP output stream", e);
        }
    }

    private void archiveRecursively(File file, String parent, ZipOutputStream zos) {
        if (file.isDirectory()) {
            try {
                String folderName = parent + "/";
                zos.putNextEntry(new ZipEntry(folderName));
                zos.closeEntry();

                try (Stream<Path> paths = Files.list(file.toPath())) {
                    paths.forEach(child -> archiveRecursively(
                            child.toFile(),
                            folderName + child.getFileName(), zos
                    ));
                }
            } catch (IOException e) {
                throw new ArchiveException("Failed to process files to archive", e);
            }
        } else {
            try (FileInputStream fis = new FileInputStream(file)) {
                ZipEntry zipEntry = new ZipEntry(parent);
                zos.putNextEntry(zipEntry);

                byte[] buffer = new byte[8 * 1024];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    zos.write(buffer, 0, bytesRead);
                }

                zos.closeEntry();
            } catch (IOException e) {
                throw new ArchiveException("Failed to archive files to ZIP", e);
            }
        }
    }
}
