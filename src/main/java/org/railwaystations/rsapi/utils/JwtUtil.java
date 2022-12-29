package org.railwaystations.rsapi.utils;

import com.nimbusds.jose.jwk.RSAKey;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.UUID;

public class JwtUtil {

    public static RSAKey loadRsaKey(String jwkSourceKeyFile) throws IOException, ParseException {
        var path = Path.of(jwkSourceKeyFile);
        var json = Files.readString(path, Charset.defaultCharset());
        return RSAKey.parse(json);
    }

    public static void writeRsaKey(RSAKey rsaKey, String jwkSourceKeyFile) throws IOException {
        var path = Path.of(jwkSourceKeyFile);
        Files.writeString(path, rsaKey.toJSONString(), Charset.defaultCharset());
    }

    public static RSAKey generateRsaKey() {
        var keyPair = generateRsaKeyPair();
        var publicKey = (RSAPublicKey) keyPair.getPublic();
        var privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    public static KeyPair generateRsaKeyPair() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator =
                    KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

}
