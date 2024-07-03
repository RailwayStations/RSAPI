package org.railwaystations.rsapi.adapter.web.controller

import org.railwaystations.rsapi.adapter.web.api.PhotoDownloadApi
import org.railwaystations.rsapi.core.ports.outbound.PhotoStoragePort
import org.railwaystations.rsapi.core.utils.ImageUtil.extensionToMimeType
import org.railwaystations.rsapi.core.utils.ImageUtil.getExtension
import org.railwaystations.rsapi.core.utils.ImageUtil.scalePhoto
import org.railwaystations.rsapi.core.utils.Logger
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

@RestController
class PhotoDownloadController(private val photoStoragePort: PhotoStoragePort) : PhotoDownloadApi {

    private val log by Logger()

    override fun getInboxDoneFile(filename: String, width: Int?): ResponseEntity<Resource> {
        log.info("Download inbox done file={}", filename)
        return downloadPhoto(photoStoragePort.getInboxDoneFile(filename), width)
    }

    override fun getInboxFile(filename: String, width: Int?): ResponseEntity<Resource> {
        log.info("Download inbox file={}", filename)
        return downloadPhoto(photoStoragePort.getInboxFile(filename), width)
    }

    override fun getInboxProcessedFile(filename: String, width: Int?): ResponseEntity<Resource> {
        log.info("Download inbox processed file={}", filename)
        return downloadPhoto(photoStoragePort.getInboxProcessedFile(filename), width)
    }

    override fun getInboxRejectedFile(filename: String, width: Int?): ResponseEntity<Resource> {
        log.info("Download inbox rejected file={}", filename)
        return downloadPhoto(photoStoragePort.getInboxRejectedFile(filename), width)
    }

    override fun getPhotos(country: String, filename: String, width: Int?): ResponseEntity<Resource> {
        log.info("Download photo country={}, file={}", country, filename)
        return downloadPhoto(photoStoragePort.getPhotoFile(country, filename), width)
    }

    private fun downloadPhoto(photoPath: Path, width: Int?): ResponseEntity<Resource> {
        if (!Files.exists(photoPath) || !Files.isReadable(photoPath)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }

        val body: ByteArray
        try {
            body = scalePhoto(photoPath, width)
        } catch (e: IOException) {
            log.error("Error scaling photo {} to width {}", photoPath, width, e)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        }

        return ResponseEntity.ok()
            .contentType(MediaType.valueOf(extensionToMimeType(getExtension(photoPath.toString()))))
            .body(ByteArrayResource(body))
    }

}
