package org.railwaystations.rsapi.adapter.web.resources;

import org.railwaystations.rsapi.domain.port.out.PhotoStorage;
import org.railwaystations.rsapi.utils.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class PhotoDownloadResource {

    private static final Logger LOG = LoggerFactory.getLogger(PhotoDownloadResource.class);
    public static final String COUNTRY_CODE = "countryCode";
    public static final String FILENAME = "filename";
    public static final String WIDTH = "width";

    private final PhotoStorage photoStorage;

    public PhotoDownloadResource(final PhotoStorage photoStorage) {
        this.photoStorage = photoStorage;
    }

    @GetMapping(value = "/fotos/{countryCode}/{filename}", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<byte[]> fotos(@PathVariable(COUNTRY_CODE) final String countryCode,
                                 @PathVariable(FILENAME) final String filename,
                                 @RequestParam(value = WIDTH, required = false) final Integer width) throws IOException {
        return photos(countryCode, filename, width);
    }

    @GetMapping(value = "/photos/{countryCode}/{filename}", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<byte[]> photos(@PathVariable(COUNTRY_CODE) final String countryCode,
                                  @PathVariable(FILENAME) final String filename,
                                  @RequestParam(value = WIDTH, required = false) final Integer width) throws IOException {
        LOG.info("Download photo country={}, file={}", countryCode, filename);
        return downloadPhoto(photoStorage.getPhotoFile(countryCode, filename), width);
    }

    private static ResponseEntity<byte[]> downloadPhoto(final Path photo, final Integer width) throws IOException {
        if (!Files.exists(photo) || !Files.isReadable(photo)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok().contentType(MediaType.valueOf(ImageUtil.extensionToMimeType(ImageUtil.getExtension(photo.toString())))).body(ImageUtil.scalePhoto(photo, width));
    }

    @GetMapping(value = "/inbox/{filename}", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<byte[]> inbox(@PathVariable(FILENAME) final String filename,
                                 @RequestParam(value = WIDTH, required = false) final Integer width) throws IOException {
        LOG.info("Download inbox file={}", filename);
        return downloadPhoto(photoStorage.getInboxFile(filename), width);
    }

    @GetMapping(value = "/inbox/processed/{filename}", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<byte[]> inboxProcessed(@PathVariable(FILENAME) final String filename,
                                          @RequestParam(value = WIDTH, required = false) final Integer width) throws IOException {
        LOG.info("Download inbox file={}", filename);
        return downloadPhoto(photoStorage.getInboxProcessedFile(filename), width);
    }

}
