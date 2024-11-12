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

package dev.vlxd.storageservice.controller;

import dev.vlxd.storageservice.constants.ArchiveType;
import dev.vlxd.storageservice.service.storage.IStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.server.PathParam;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

@RestController
@RequestMapping("/v1/storage")
public class StorageController {

    private final IStorageService storageService;

    public StorageController(IStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(value = "/upload", consumes = "application/octet-stream")
    ResponseEntity<String> upload(HttpServletRequest request, @RequestHeader("X-Filename") String filename) {
        try (InputStream inputStream = request.getInputStream()) {
            return ResponseEntity
                    .ok()
                    .body(storageService.uploadFile(inputStream, filename));
        } catch (IOException e) {
            throw new RuntimeException("Failed to process request input stream", e);
        }
    }

    @GetMapping("/resource")
    ResponseEntity<Resource> getResource(@PathParam("fileId") String fileId) {
        Resource resource = storageService.loadAsResource(fileId);

        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        MimeType contentType;

        try {
            contentType = MimeTypeUtils.parseMimeType(Files.probeContentType(resource.getFile().toPath()));
        } catch (IOException e) {
            contentType = MimeTypeUtils.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.asMediaType(contentType))
                .body(resource);
    }

    @GetMapping("/archive")
    public ResponseEntity<Void> archive(@PathParam("fileId") String fileId,
                                        @PathParam("archiveType") ArchiveType archiveType,
                                        HttpServletResponse response) {
        storageService.checkFile(fileId);

        try (OutputStream outputStream = response.getOutputStream()) {
            String[] segments = fileId.split("/");

            response.setContentType(archiveType.getContentType());
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + segments[segments.length - 1] + ".zip");

            storageService.archiveFile(archiveType, fileId, outputStream);

            return ResponseEntity.ok(null);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process response output stream", e);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Boolean> delete(@PathParam("fileId") String fileId) {
        return ResponseEntity.ok(storageService.deleteFile(fileId));
    }
}
