package org.railwaystations.rsapi.core.ports

import org.railwaystations.rsapi.core.model.User

interface UserPort {
    fun list(): List<User>
    fun findByNormalizedName(normalizedName: String): User?
    fun findById(id: Int): User?
    fun findByEmail(email: String): User?
    fun updateCredentials(id: Int, key: String)
    fun insert(user: User, key: String?, emailVerification: String?): Int
    fun update(id: Int, user: User)
    fun findByEmailVerification(emailVerification: String): User?
    fun updateEmailVerification(id: Int, emailVerification: String?)
    fun anonymizeUser(id: Int)
    fun addUsernameToBlocklist(normalizedName: String)
    fun countBlockedUsername(normalizedName: String): Int
    fun updateLocale(id: Int, locallocaleLanguageTage: String)
}
