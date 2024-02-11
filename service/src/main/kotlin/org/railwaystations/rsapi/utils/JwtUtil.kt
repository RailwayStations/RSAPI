package org.railwaystations.rsapi.utils

import com.nimbusds.jose.jwk.RSAKey
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.text.ParseException
import java.util.*

object JwtUtil {

    @Throws(IOException::class, ParseException::class)
    @JvmStatic
    fun loadRsaKey(jwkSourceKeyFile: String): RSAKey {
        val path = Path.of(jwkSourceKeyFile)
        val json = Files.readString(path, Charset.defaultCharset())
        return RSAKey.parse(json)
    }

    @Throws(IOException::class)
    @JvmStatic
    fun writeRsaKey(rsaKey: RSAKey, jwkSourceKeyFile: String) {
        val path = Path.of(jwkSourceKeyFile)
        Files.writeString(path, rsaKey.toJSONString(), Charset.defaultCharset())
    }

    @JvmStatic
    fun generateRsaKey(): RSAKey {
        val keyPair = generateRsaKeyPair()
        val publicKey = keyPair.public as RSAPublicKey
        val privateKey = keyPair.private as RSAPrivateKey
        return RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build()
    }

    @JvmStatic
    fun generateRsaKeyPair(): KeyPair {
        val keyPair: KeyPair
        try {
            val keyPairGenerator =
                KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            keyPair = keyPairGenerator.generateKeyPair()
        } catch (ex: Exception) {
            throw IllegalStateException(ex)
        }
        return keyPair
    }
}
