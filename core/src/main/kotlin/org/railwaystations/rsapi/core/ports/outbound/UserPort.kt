package org.railwaystations.rsapi.core.ports.outbound

import org.railwaystations.rsapi.core.model.User

interface UserPort {
    fun list(): List<User>
    fun findByName(name: String): User?
    fun findById(id: Long): User?
    fun findByEmail(email: String): User?
    fun updateCredentials(id: Long, key: String)
    fun insert(user: User, key: String?, emailVerification: String?): Long
    fun update(id: Long, user: User)
    fun findByEmailVerification(emailVerification: String): User?
    fun updateEmailVerification(id: Long, emailVerification: String?)
    fun anonymizeUser(id: Long)
    fun addUsernameToBlocklist(name: String)
    fun countBlockedUsername(name: String): Int
    fun updateLocale(id: Long, locallocaleLanguageTage: String)
}