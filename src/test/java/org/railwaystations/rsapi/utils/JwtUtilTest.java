package org.railwaystations.rsapi.utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    @Test
    void generateWriteAndLoadRsaKey() throws IOException, ParseException {
        var temp = Files.createTempDirectory("rsapi");
        var rsaKeyFile = temp.resolve("rsaKeyFile.json");
        var rsaKey = JwtUtil.generateRsaKey();
        System.out.println(rsaKeyFile);
        System.out.println(rsaKey.toJSONString());
        JwtUtil.writeRsaKey(rsaKey, rsaKeyFile.toString());
        assertThat(rsaKeyFile).isNotEmptyFile();
        var loadedRsaKey = JwtUtil.loadRsaKey(rsaKeyFile.toString());
        assertThat(loadedRsaKey).isEqualTo(rsaKey);
    }

}