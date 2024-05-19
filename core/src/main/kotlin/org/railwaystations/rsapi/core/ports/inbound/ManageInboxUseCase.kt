package org.railwaystations.rsapi.core.ports.inbound

import org.railwaystations.rsapi.core.model.InboxCommand
import org.railwaystations.rsapi.core.model.InboxEntry
import org.railwaystations.rsapi.core.model.InboxResponse
import org.railwaystations.rsapi.core.model.InboxStateQuery
import org.railwaystations.rsapi.core.model.ProblemReport
import org.railwaystations.rsapi.core.model.PublicInboxEntry
import org.railwaystations.rsapi.core.model.User
import java.io.InputStream

interface ManageInboxUseCase {
    fun reportProblem(problemReport: ProblemReport, user: User, clientInfo: String?): InboxResponse

    fun publicInbox(): List<PublicInboxEntry>

    fun userInbox(user: User, showCompletedEntries: Boolean): List<InboxStateQuery>

    fun userInbox(user: User, ids: List<Long>): List<InboxStateQuery>

    fun listAdminInbox(user: User): List<InboxEntry>

    fun countPendingInboxEntries(): Long

    fun uploadPhoto(
        clientInfo: String?, body: InputStream?, stationId: String?,
        countryCode: String?, contentType: String?, stationTitle: String?,
        latitude: Double?, longitude: Double?, comment: String?,
        active: Boolean, user: User
    ): InboxResponse

    fun changeStationTitle(command: InboxCommand)

    fun rejectInboxEntry(command: InboxCommand)

    fun importPhoto(command: InboxCommand)

    fun importMissingStation(command: InboxCommand)

    fun updateStationActiveState(command: InboxCommand, active: Boolean)

    fun deleteStation(command: InboxCommand)

    fun deletePhoto(command: InboxCommand)

    fun markProblemReportSolved(command: InboxCommand)

    fun updateLocation(command: InboxCommand)

    fun markPhotoOutdated(command: InboxCommand)
    fun deleteUserInboxEntry(user: User, id: Long)

    class InboxEntryNotFoundException : RuntimeException("InboxEntry not found")
    class InboxEntryNotOwnerException : RuntimeException("InboxEntry not owned by user")

}