package org.railwaystations.rsapi.adapter.photostorage

import org.apache.commons.io.IOUtils
import org.railwaystations.rsapi.core.model.InboxEntry
import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.ports.PhotoStorage
import org.railwaystations.rsapi.core.ports.PhotoStorage.PhotoTooLargeException
import org.railwaystations.rsapi.core.utils.Logger
import org.springframework.stereotype.Repository
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream

@Repository
class PhotoFileStorage(private val workDir: WorkDir) : PhotoStorage {


    override fun isProcessed(filename: String): Boolean {
        return Files.exists(workDir.inboxProcessedDir.resolve(filename))
    }

    @Throws(IOException::class)
    override fun importPhoto(inboxEntry: InboxEntry, station: Station): String {
        val uploadedFile = getUploadFile(inboxEntry.filename!!)
        val processedFile = workDir.inboxProcessedDir.resolve(inboxEntry.filename!!)
        val destinationDir = workDir.photosDir.resolve(station.key.country)
        Files.createDirectories(destinationDir)
        val destinationFile = getDestinationFile(destinationDir, station.key.id, inboxEntry.extension)
        if (Files.exists(processedFile)) {
            Files.move(processedFile, destinationFile, StandardCopyOption.REPLACE_EXISTING)
        } else {
            Files.copy(uploadedFile, destinationFile, StandardCopyOption.REPLACE_EXISTING)
        }
        try {
            Files.move(
                uploadedFile,
                workDir.inboxDoneDir.resolve(uploadedFile.fileName),
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (e: Exception) {
            log.warn("Couldn't move original file {} to done dir", uploadedFile, e)
        }
        return "/${destinationDir.fileName}/${destinationFile.fileName}"
    }

    private fun getDestinationFile(destinationDir: Path, stationId: String, extension: String?): Path {
        for (sequence in 1..99) {
            val destinationFile = destinationDir.resolve(sanitizeFilename("${stationId}_$sequence.$extension"))
            if (Files.notExists(destinationFile)) {
                return destinationFile
            }
        }
        throw RuntimeException("Number of photos per station ${destinationDir.fileName}/$stationId exceeded")
    }

    @Throws(IOException::class)
    override fun reject(inboxEntry: InboxEntry) {
        val filename = inboxEntry.filename
        if (filename != null) {
            val file = getUploadFile(filename)
            Files.move(file, workDir.inboxRejectedDir.resolve(file.fileName), StandardCopyOption.REPLACE_EXISTING)
            Files.deleteIfExists(workDir.inboxToProcessDir.resolve(filename))
            Files.deleteIfExists(workDir.inboxProcessedDir.resolve(filename))
        }
    }

    @Throws(PhotoTooLargeException::class, IOException::class)
    override fun storeUpload(body: InputStream, filename: String): Long {
        val file = getUploadFile(filename)
        log.info("Writing photo to {}", file)

        // write the file to the inbox directory
        val cos = CheckedOutputStream(Files.newOutputStream(file), CRC32())
        val bytesRead = IOUtils.copyLarge(body, cos, 0L, MAX_SIZE)
        if (bytesRead == MAX_SIZE) {
            Files.deleteIfExists(file)
            throw PhotoTooLargeException(MAX_SIZE)
        }
        cos.close()

        // additionally write the file to the input directory for Vsion.AI
        Files.copy(
            file,
            workDir.inboxToProcessDir.resolve(file.fileName),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.COPY_ATTRIBUTES
        )
        return cos.checksum.value
    }

    override fun getUploadFile(filename: String): Path {
        return workDir.inboxDir.resolve(filename)
    }

    override fun getPhotoFile(countryCode: String, filename: String): Path {
        return workDir.photosDir.resolve(sanitizeFilename(countryCode)).resolve(sanitizeFilename(filename))
    }

    override fun getInboxFile(filename: String): Path {
        return workDir.inboxDir.resolve(sanitizeFilename(filename))
    }

    override fun getInboxProcessedFile(filename: String): Path {
        return workDir.inboxProcessedDir.resolve(sanitizeFilename(filename))
    }

    override fun getInboxToProcessFile(filename: String): Path {
        return workDir.inboxToProcessDir.resolve(sanitizeFilename(filename))
    }

    override fun cleanupOldCopies() {
        val maxAge = Instant.now().minus(workDir.keepFileCopiesInDays.toLong(), ChronoUnit.DAYS)
        cleanupOldCopiesFrom(workDir.inboxDoneDir, maxAge)
        cleanupOldCopiesFrom(workDir.inboxRejectedDir, maxAge)
    }

    override fun getInboxDoneFile(filename: String): Path {
        return workDir.inboxDoneDir.resolve(sanitizeFilename(filename))
    }

    override fun getInboxRejectedFile(filename: String): Path {
        return workDir.inboxRejectedDir.resolve(sanitizeFilename(filename))
    }

    companion object {
        private val log by Logger()

        private const val MAX_SIZE = 20000000L

        fun cleanupOldCopiesFrom(dir: Path, maxAge: Instant) {
            try {
                Files.list(dir).use { pathStream ->
                    pathStream
                        .filter { path -> Files.isRegularFile(path) }
                        .filter { path -> isOlderThan(path, maxAge) }
                        .forEach { path -> deleteSilently(path) }
                }
            } catch (e: Exception) {
                log.error("Failed to cleanup old copies from {}", dir, e)
            }
        }

        private fun deleteSilently(path: Path) {
            try {
                Files.delete(path)
                log.info("Deleted {}", path)
            } catch (e: IOException) {
                log.warn("Unable to delete {}", path)
            }
        }

        private fun isOlderThan(path: Path, maxAge: Instant): Boolean {
            try {
                return Files.getLastModifiedTime(path).toInstant().isBefore(maxAge)
            } catch (e: IOException) {
                log.warn("Unable to getLastModifiedTime of {}", path.fileName)
            }
            return false
        }

        fun sanitizeFilename(fileName: String): String {
            return fileName.replace(" ", "_")
                .replace("/", "_")
                .replace(":", "_")
                .replace("\"", "_")
                .replace("|", "_")
                .replace("*", "_")
                .replace("?", "_")
                .replace("<", "_")
                .replace(">", "_")
                .replace('\\', '_')
        }
    }
}
