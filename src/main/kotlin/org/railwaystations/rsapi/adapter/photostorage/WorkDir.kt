package org.railwaystations.rsapi.adapter.photostorage

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

@Component
class WorkDir(
    @Value("\${workDir}") workDir: String,
    @Value("\${keepFileCopiesInDays}") keepFileCopiesInDaysInit: Int?
) {
    final val photosDir: Path
    final val inboxDir: Path
    final val inboxProcessedDir: Path
    final val inboxToProcessDir: Path
    final val inboxDoneDir: Path
    final val inboxRejectedDir: Path
    final val keepFileCopiesInDays: Int

    init {
        try {
            photosDir = Files.createDirectories(Path.of(workDir, "photos"))
            inboxDir = Path.of(workDir, "inbox")
            inboxProcessedDir = Files.createDirectories(inboxDir.resolve("processed"))
            inboxToProcessDir = Files.createDirectories(inboxDir.resolve("toprocess"))
            inboxDoneDir = Files.createDirectories(inboxDir.resolve("done"))
            inboxRejectedDir = Files.createDirectories(inboxDir.resolve("rejected"))
        } catch (e: IOException) {
            throw RuntimeException("Unable to create working directories", e)
        }
        keepFileCopiesInDays = keepFileCopiesInDaysInit ?: KEEP_FILE_COPIES_IN_DAYS_DEFAULT
    }

    companion object {
        const val KEEP_FILE_COPIES_IN_DAYS_DEFAULT: Int = 60
    }
}
