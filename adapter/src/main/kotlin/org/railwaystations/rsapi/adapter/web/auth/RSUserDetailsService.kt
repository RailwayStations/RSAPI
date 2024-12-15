package org.railwaystations.rsapi.adapter.web.auth

import org.railwaystations.rsapi.adapter.db.UserAdapter
import org.railwaystations.rsapi.core.model.EMAIL_VERIFIED
import org.railwaystations.rsapi.core.model.User
import org.railwaystations.rsapi.core.model.normalizeEmail
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class RSUserDetailsService(private val userAdapter: UserAdapter) : UserDetailsService {

    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(username: String): AuthUser {
        val user = userAdapter.findByEmail(normalizeEmail(username))
            ?: userAdapter.findByName(username)

        if (user == null) {
            throw UsernameNotFoundException("User '$username' not found")
        }
        return AuthUser(
            user, user.roles
                .map { role -> SimpleGrantedAuthority(role) })
    }

    fun updateEmailVerification(user: User?) {
        if (user!!.isEmailVerifiedWithNextLogin) {
            userAdapter.updateEmailVerification(user.id, EMAIL_VERIFIED)
        }
    }
}