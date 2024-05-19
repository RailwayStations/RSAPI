package org.railwaystations.rsapi.core.ports.outbound

import org.railwaystations.rsapi.core.model.InboxEntry
import org.railwaystations.rsapi.core.model.Station
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path

interface PhotoStoragePort {
    fun isProcessed(filename: String): Boolean

    @Throws(IOException::class)
    fun importPhoto(inboxEntry: InboxEntry, station: Station): String

    @Throws(IOException::class)
    fun reject(inboxEntry: InboxEntry)

    fun getUploadFile(filename: String): Path

    @Throws(IOException::class, PhotoTooLargeException::class)
    fun storeUpload(body: InputStream, filename: String): Long

    fun getPhotoFile(countryCode: String, filename: String): Path

    fun getInboxFile(filename: String): Path

    fun getInboxProcessedFile(filename: String): Path

    fun getInboxToProcessFile(filename: String): Path

    fun cleanupOldCopies()

    fun getInboxDoneFile(filename: String): Path

    fun getInboxRejectedFile(filename: String): Path

    class PhotoTooLargeException(val maxSize: Long) : Exception()
}