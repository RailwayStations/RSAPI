package org.railwaystations.rsapi.adapter.db

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.railwaystations.rsapi.core.model.License
import org.railwaystations.rsapi.core.model.UserTestFixtures
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import java.util.*

@JooqTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    JooqCustomizerConfiguration::class,
    UserAdapter::class,
)
class UserAdapterTest : AbstractPostgreSqlTest() {

    @Autowired
    private lateinit var sut: UserAdapter

    @Test
    fun findByName() {
        val user = sut.findByName(UserTestFixtures.user10.name)

        assertThat(user).isEqualTo(UserTestFixtures.user10)
    }

    @Test
    fun findById() {
        val user = sut.findById(UserTestFixtures.user10.id)

        assertThat(user).isEqualTo(UserTestFixtures.user10)
    }

    @Test
    fun findByEmail() {
        val user = sut.findByEmail(UserTestFixtures.user10.email!!)

        assertThat(user).isEqualTo(UserTestFixtures.user10)
    }

    @Test
    fun updateCredentials() {
        val user = sut.findById(10)
        assertThat(user!!.key).isNull()

        sut.updateCredentials(10, "newKey")

        assertThat(sut.findById(10)!!.key).isEqualTo("newKey")
    }

    @Test
    fun insert() {
        TODO()
    }

    @Test
    fun update() {
        TODO()
    }

    @Test
    fun updateEmailVerificationAndFindByEmailVerification() {
        val user = sut.findById(22)
        assertThat(user!!.emailVerification).isNull()
        val newEmailVerification = UUID.randomUUID().toString()

        sut.updateEmailVerification(22, newEmailVerification)

        assertThat(sut.findByEmailVerification(newEmailVerification)!!.id).isEqualTo(22L)
    }

    @Test
    fun anonymizeUser() {
        sut.anonymizeUser(15)

        val anonymizedUser = sut.findById(15)
        with(anonymizedUser!!) {
            assertThat(id).isEqualTo(15)
            assertThat(name).isEqualTo("deleteduser15")
            assertThat(url).isNull()
            assertThat(anonymous).isTrue()
            assertThat(email).isNull()
            assertThat(emailVerification).isNull()
            assertThat(ownPhotos).isFalse()
            assertThat(license).isEqualTo(License.UNKNOWN)
            assertThat(key).isNull()
            assertThat(sendNotifications).isFalse()
            assertThat(admin).isFalse()
        }
    }

    @Test
    fun addUsernameToBlocklistAndCountBlockedUsername() {
        val name = "blockedUser"
        assertThat(sut.countBlockedUsername(name)).isEqualTo(0)

        sut.addUsernameToBlocklist(name)

        assertThat(sut.countBlockedUsername(name)).isEqualTo(1)
    }

    @Test
    fun updateLocale() {
        val user = sut.findById(22)
        assertThat(user!!.locale).isEqualTo(Locale.ENGLISH)

        sut.updateLocale(22, Locale.GERMANY.toLanguageTag())

        assertThat(sut.findById(22)!!.locale).isEqualTo(Locale.GERMANY)
    }

}