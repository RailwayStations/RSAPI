package org.railwaystations.rsapi.adapter.web.auth

import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.SodiumJava
import com.goterl.lazysodium.exceptions.SodiumException
import com.goterl.lazysodium.interfaces.PwHash
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

private val LAZY_SODIUM = LazySodiumJava(SodiumJava(), StandardCharsets.UTF_8)
private val PW_HASH_LAZY: PwHash.Lazy = LAZY_SODIUM

@Component
class LazySodiumPasswordEncoder : PasswordEncoder {
    override fun encode(rawPassword: CharSequence): String {
        try {
            return PW_HASH_LAZY.cryptoPwHashStr(
                rawPassword.toString(),
                PwHash.OPSLIMIT_INTERACTIVE,
                PwHash.MEMLIMIT_INTERACTIVE
            )
        } catch (ex: SodiumException) {
            throw RuntimeException("Exception encountered in hashPassword()", ex)
        }
    }

    override fun matches(rawPassword: CharSequence, encodedPassword: String): Boolean {
        if (rawPassword.isBlank() || encodedPassword.isBlank()) {
            return false
        }
        return PW_HASH_LAZY.cryptoPwHashStrVerify(encodedPassword, rawPassword.toString())
    }

}
