package org.railwaystations.rsapi.adapter.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.railwaystations.rsapi.adapter.web.JwtUtil.generateRsaKey
import org.railwaystations.rsapi.adapter.web.JwtUtil.loadRsaKey
import org.railwaystations.rsapi.adapter.web.JwtUtil.writeRsaKey
import java.nio.file.Files

internal class JwtUtilTest {
    @Test
    fun generateWriteAndLoadRsaKey() {
        val temp = Files.createTempDirectory("rsapi")
        val rsaKeyFile = temp.resolve("rsaKeyFile.json")
        val rsaKey = generateRsaKey()
        println(rsaKeyFile)
        println(rsaKey.toJSONString())
        writeRsaKey(rsaKey, rsaKeyFile.toString())
        assertThat(rsaKeyFile).isNotEmptyFile()
        val loadedRsaKey = loadRsaKey(rsaKeyFile.toString())
        assertThat(loadedRsaKey).isEqualTo(rsaKey)
    }
}