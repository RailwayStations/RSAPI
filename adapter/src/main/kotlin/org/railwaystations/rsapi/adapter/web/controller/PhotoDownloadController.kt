package org.railwaystations.rsapi.adapter.web.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.Size
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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

@RestController
class PhotoDownloadController(private val photoStoragePort: PhotoStoragePort) {

    private val log by Logger()

    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/inbox/done/{filename}"],
        produces = ["image/jpeg", "image/png"]
    )
    fun inboxDoneFilenameGet(
        @PathVariable(value = "filename") filename: String,
        @Valid @RequestParam(required = false, value = "width") width: Int?
    ): ResponseEntity<Resource> {
        log.info("Download inbox file={}", filename)
        return downloadPhoto(photoStoragePort.getInboxDoneFile(filename), width)
    }

    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/inbox/{filename}"],
        produces = ["image/jpeg", "image/png"]
    )
    fun inboxFilenameGet(
        @PathVariable(value = "filename") filename: String,
        @Valid @RequestParam(required = false, value = "width") width: Int?
    ): ResponseEntity<Resource> {
        log.info("Download inbox file={}", filename)
        return downloadPhoto(photoStoragePort.getInboxFile(filename), width)
    }

    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/inbox/processed/{filename}"],
        produces = ["image/jpeg", "image/png"]
    )
    fun inboxProcessedFilenameGet(
        @PathVariable(value = "filename") filename: String,
        @Valid @RequestParam(required = false, value = "width") width: Int?
    ): ResponseEntity<Resource> {
        log.info("Download inbox file={}", filename)
        return downloadPhoto(photoStoragePort.getInboxProcessedFile(filename), width)
    }

    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/inbox/rejected/{filename}"],
        produces = ["image/jpeg", "image/png"]
    )
    fun inboxRejectedFilenameGet(
        @PathVariable(value = "filename") filename: String,
        @Valid @RequestParam(required = false, value = "width") width: Int?
    ): ResponseEntity<Resource> {
        log.info("Download inbox file={}", filename)
        return downloadPhoto(photoStoragePort.getInboxRejectedFile(filename), width)
    }

    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/photos/{country}/{filename}"],
        produces = ["image/jpeg", "image/png"]
    )
    fun photosCountryFilenameGet(
        @Size(max = 2, min = 2) @PathVariable(value = "country") country: String,
        @PathVariable(value = "filename") filename: String,
        @Valid @RequestParam(required = false, value = "width") width: Int?
    ): ResponseEntity<Resource> {
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
