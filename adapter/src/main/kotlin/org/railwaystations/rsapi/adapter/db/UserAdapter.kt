package org.railwaystations.rsapi.adapter.db

import org.jooq.DSLContext
import org.railwaystations.rsapi.adapter.db.jooq.tables.records.BlockedUsernameRecord
import org.railwaystations.rsapi.adapter.db.jooq.tables.records.UserRecord
import org.railwaystations.rsapi.adapter.db.jooq.tables.references.BlockedUsernameTable
import org.railwaystations.rsapi.adapter.db.jooq.tables.references.UserTable
import org.railwaystations.rsapi.core.model.User
import org.railwaystations.rsapi.core.model.nameToLicense
import org.railwaystations.rsapi.core.ports.outbound.UserPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Component
class UserAdapter(private val dsl: DSLContext) : UserPort {

    override fun findByName(name: String): User? {
        return dsl.selectFrom(UserTable)
            .where(UserTable.name.eq(name))
            .fetchOne()?.toUser()
    }

    private fun UserRecord.toUser() =
        User(
            id = id!!,
            name = name,
            url = url,
            license = license.nameToLicense(),
            email = email,
            ownPhotos = ownphotos,
            anonymous = anonymous,
            key = key,
            admin = admin,
            emailVerification = emailverification,
            newPassword = null,
            sendNotifications = sendnotifications == true,
            locale = if (locale != null) Locale.forLanguageTag(locale) else Locale.ENGLISH
        )

    override fun findById(id: Long): User? {
        return dsl.selectFrom(UserTable)
            .where(UserTable.id.eq(id))
            .fetchOne()?.toUser()
    }

    override fun findByEmail(email: String): User? {
        return dsl.selectFrom(UserTable)
            .where(UserTable.email.eq(email))
            .fetchOne()?.toUser()

    }

    @Transactional
    override fun updateCredentials(id: Long, key: String) {
        dsl.update(UserTable)
            .set(UserTable.key, key)
            .where(UserTable.id.eq(id))
            .execute()
    }

    @Transactional
    override fun insert(user: User, key: String?, emailVerification: String?): Long {
        val userRecord = UserRecord(
            id = null,
            name = user.name,
            email = user.email,
            url = user.url,
            ownphotos = user.ownPhotos,
            anonymous = user.anonymous,
            license = user.license.name,
            key = key,
            admin = user.admin,
            emailverification = emailVerification,
            sendnotifications = user.sendNotifications,
            locale = user.localeLanguageTag,
        )
        dsl.attach(userRecord)
        userRecord.store()
        return userRecord.id!!
    }

    @Transactional
    override fun update(id: Long, user: User) {
        dsl.update(UserTable)
            .set(UserTable.name, user.name)
            .set(UserTable.url, user.url)
            .set(UserTable.email, user.email)
            .set(UserTable.ownphotos, user.ownPhotos)
            .set(UserTable.anonymous, user.anonymous)
            .set(UserTable.sendnotifications, user.sendNotifications)
            .set(UserTable.locale, user.localeLanguageTag)
            .where(UserTable.id.eq(id))
            .execute()
    }

    override fun findByEmailVerification(emailVerification: String): User? {
        return dsl.selectFrom(UserTable)
            .where(UserTable.emailverification.eq(emailVerification))
            .fetchOne()?.toUser()
    }

    @Transactional
    override fun updateEmailVerification(id: Long, emailVerification: String?) {
        dsl.update(UserTable)
            .set(UserTable.emailverification, emailVerification)
            .where(UserTable.id.eq(id))
            .execute()
    }

    @Transactional
    override fun anonymizeUser(id: Long) {
        dsl.update(UserTable)
            .set(UserTable.name, "deleteduser$id")
            .setNull(UserTable.url)
            .set(UserTable.anonymous, true)
            .setNull(UserTable.email)
            .setNull(UserTable.emailverification)
            .set(UserTable.ownphotos, false)
            .setNull(UserTable.license)
            .setNull(UserTable.key)
            .set(UserTable.sendnotifications, false)
            .set(UserTable.admin, false)
            .where(UserTable.id.eq(id))
            .execute()
    }

    @Transactional
    override fun addUsernameToBlocklist(name: String) {
        dsl.executeInsert(BlockedUsernameRecord(null, name, Instant.now()))
    }

    override fun countBlockedUsername(name: String): Int {
        return dsl.fetchCount(BlockedUsernameTable.where(BlockedUsernameTable.name.eq(name)))
    }

    @Transactional
    override fun updateLocale(id: Long, localeLanguageTag: String) {
        dsl.update(UserTable)
            .set(UserTable.locale, localeLanguageTag)
            .where(UserTable.id.eq(id))
            .execute()
    }

}
