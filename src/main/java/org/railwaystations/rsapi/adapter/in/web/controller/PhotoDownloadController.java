package org.railwaystations.rsapi.adapter.in.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.railwaystations.rsapi.adapter.in.web.api.PhotoDownloadApi;
import org.railwaystations.rsapi.core.ports.out.PhotoStorage;
import org.railwaystations.rsapi.utils.ImageUtil;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@Slf4j
@RequiredArgsConstructor
public class PhotoDownloadController implements PhotoDownloadApi {

    private final PhotoStorage photoStorage;

    private static ResponseEntity<Resource> downloadPhoto(Path photo, Integer width) {
        if (!Files.exists(photo) || !Files.isReadable(photo)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        byte[] body;
        try {
            body = ImageUtil.scalePhoto(photo, width);
        } catch (IOException e) {
            log.error("Error scaling photo {} to width {}", photo, width, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.ok().contentType(MediaType.valueOf(ImageUtil.extensionToMimeType(ImageUtil.getExtension(photo.toString())))).body(new ByteArrayResource(body));
    }

    @Override
    public ResponseEntity<Resource> inboxFilenameGet(String filename, Integer width) {
        log.info("Download inbox file={}", filename);
        return downloadPhoto(photoStorage.getInboxFile(filename), width);
    }

    @Override
    public ResponseEntity<Resource> inboxProcessedFilenameGet(String filename, Integer width) {
        log.info("Download inbox file={}", filename);
        return downloadPhoto(photoStorage.getInboxProcessedFile(filename), width);
    }

    @Override
    public ResponseEntity<Resource> photosCountryFilenameGet(String country, String filename, Integer width) {
        log.info("Download photo country={}, file={}", country, filename);
        return downloadPhoto(photoStorage.getPhotoFile(country, filename), width);
    }
}
