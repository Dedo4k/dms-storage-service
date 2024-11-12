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

package dev.vlxd.storageservice.constants;

import dev.vlxd.storageservice.exception.UnsupportedArchiveTypeException;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ArchiveType {
    ZIP("application/zip"),
    RAR("application/vnd.rar"),
    SEVEN_Z("application/x-7z-compressed"),
    TAR("application/x-tar");

    private final String contentType;

    ArchiveType(String contentType) {
        this.contentType = contentType;
    }

    public static ArchiveType valueOfType(String contentType) {
        return Arrays.stream(values())
                .filter(value -> value.contentType.equals(contentType))
                .findFirst()
                .orElseThrow(() -> new UnsupportedArchiveTypeException("Unsupported archive type"));
    }

}
