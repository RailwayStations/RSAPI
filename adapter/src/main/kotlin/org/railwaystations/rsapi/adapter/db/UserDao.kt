package org.railwaystations.rsapi.adapter.db

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.sqlobject.config.RegisterRowMapper
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.railwaystations.rsapi.core.model.License.Companion.of
import org.railwaystations.rsapi.core.model.User
import org.railwaystations.rsapi.core.ports.outbound.UserPort
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

interface UserDao : UserPort {
    @SqlQuery("SELECT * FROM users")
    @RegisterRowMapper(UserMapper::class)
    override fun list(): List<User>

    @SqlQuery("SELECT * FROM users WHERE normalizedName = :normalizedName")
    @RegisterRowMapper(
        UserMapper::class
    )
    override fun findByNormalizedName(@Bind("normalizedName") normalizedName: String): User?

    @SqlQuery("SELECT * FROM users WHERE id = :id")
    @RegisterRowMapper(UserMapper::class)
    override fun findById(@Bind("id") id: Int): User?

    @SqlQuery("SELECT * FROM users WHERE email = :email")
    @RegisterRowMapper(UserMapper::class)
    override fun findByEmail(@Bind("email") email: String): User?

    @SqlUpdate("UPDATE users SET `key` = :key WHERE id = :id")
    override fun updateCredentials(@Bind("id") id: Int, @Bind("key") key: String)

    @SqlUpdate(
        """
            INSERT INTO users (id, name, url, license, email, normalizedName, ownPhotos, anonymous, `key`, emailVerification, sendNotifications, locale)
                VALUES (:id, :name, :url, :license, :email, :normalizedName, :ownPhotos, :anonymous, :key, :emailVerification, :sendNotifications, :localeLanguageTag)
            
            """
    )
    @GetGeneratedKeys("id")
    override fun insert(
        @BindBean user: User,
        @Bind("key") key: String?,
        @Bind("emailVerification") emailVerification: String?
    ): Int

    @SqlUpdate(
        """
            UPDATE users SET name = :name, url = :url, license = :license, email = :email, normalizedName = :normalizedName, ownPhotos = :ownPhotos,
                            anonymous = :anonymous, sendNotifications = :sendNotifications, locale = :localeLanguageTag
            WHERE id = :id
            
            """
    )
    override fun update(@Bind("id") id: Int, @BindBean user: User)

    @SqlQuery("SELECT * FROM users WHERE emailVerification = :emailVerification")
    @RegisterRowMapper(
        UserMapper::class
    )
    override fun findByEmailVerification(@Bind("emailVerification") emailVerification: String): User?

    @SqlUpdate("UPDATE users SET emailVerification = :emailVerification WHERE id = :id")
    override fun updateEmailVerification(@Bind("id") id: Int, @Bind("emailVerification") emailVerification: String?)

    @SqlUpdate(
        """
            UPDATE users
             SET name = CONCAT('deleteduser', id),
                 url = NULL,
                 normalizedName = CONCAT('deleteduser', id),
                 anonymous = true,
                 email = NULL,
                 emailVerification = NULL,
                 ownPhotos = false,
                 license = NULL,
                 `key` = NULL,
                 sendNotifications = false,
                 admin = false
             WHERE id = :id
            
            """
    )
    override fun anonymizeUser(@Bind("id") id: Int)

    @SqlUpdate(
        """
            INSERT INTO blocked_usernames (normalizedName)
                VALUES (:normalizedName)
            
            """
    )
    override fun addUsernameToBlocklist(@Bind("normalizedName") normalizedName: String)

    @SqlQuery("SELECT COUNT(*) FROM blocked_usernames WHERE normalizedName = :normalizedName")
    override fun countBlockedUsername(@Bind("normalizedName") normalizedName: String): Int

    @SqlUpdate("UPDATE users SET locale = :localeLanguageTag WHERE id = :id")
    override fun updateLocale(@Bind("id") id: Int, @Bind("localeLanguageTag") locallocaleLanguageTage: String)

    class UserMapper : RowMapper<User> {
        @Throws(SQLException::class)
        override fun map(rs: ResultSet, ctx: StatementContext): User {
            val locale = rs.getString("locale")
            return User(
                id = rs.getInt("id"),
                name = rs.getString("name"),
                url = rs.getString("url"),
                license = of(rs.getString("license")),
                email = rs.getString("email"),
                ownPhotos = rs.getBoolean("ownPhotos"),
                anonymous = rs.getBoolean("anonymous"),
                key = rs.getString("key"),
                admin = rs.getBoolean("admin"),
                emailVerification = rs.getString("emailVerification"),
                newPassword = null,
                sendNotifications = rs.getBoolean("sendNotifications"),
                locale = if (locale != null) Locale.forLanguageTag(locale) else Locale.ENGLISH
            )
        }
    }
}
