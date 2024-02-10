package org.railwaystations.rsapi.core.ports

import org.railwaystations.rsapi.core.model.User
import java.util.*

interface ManageProfileUseCase {
    fun changePassword(user: User, newPassword: String)

    fun resetPassword(nameOrEmail: String, clientInfo: String?)

    @Throws(ProfileConflictException::class)
    fun register(newUser: User, clientInfo: String?)

    @Throws(ProfileConflictException::class)
    fun updateProfile(user: User, newProfile: User, clientInfo: String?)

    fun resendEmailVerification(user: User)

    fun emailVerification(token: String): User?

    fun deleteProfile(user: User, userAgent: String?)

    fun updateLocale(user: User, locale: Locale)

    class ProfileConflictException : RuntimeException("Name or eMail is already taken")
}
