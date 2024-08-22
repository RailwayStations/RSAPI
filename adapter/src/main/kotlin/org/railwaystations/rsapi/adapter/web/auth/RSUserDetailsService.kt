package org.railwaystations.rsapi.app.auth

import org.railwaystations.rsapi.adapter.db.UserDao
import org.railwaystations.rsapi.core.model.EMAIL_VERIFIED
import org.railwaystations.rsapi.core.model.User
import org.railwaystations.rsapi.core.model.normalizeEmail
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class RSUserDetailsService(private val userDao: UserDao) : UserDetailsService {

    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(username: String): AuthUser {
        val user = userDao.findByEmail(normalizeEmail(username))
            ?: userDao.findByName(username)

        if (user == null) {
            throw UsernameNotFoundException("User '$username' not found")
        }
        return AuthUser(user, user.roles
            .map { role -> SimpleGrantedAuthority(role) })
    }

    fun updateEmailVerification(user: User?) {
        if (user!!.isEmailVerifiedWithNextLogin) {
            userDao.updateEmailVerification(user.id, EMAIL_VERIFIED)
        }
    }
}