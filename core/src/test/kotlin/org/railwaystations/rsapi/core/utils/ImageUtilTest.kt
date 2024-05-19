package org.railwaystations.rsapi.core.utils

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.railwaystations.rsapi.core.utils.ImageUtil.extensionToMimeType
import org.railwaystations.rsapi.core.utils.ImageUtil.getExtension
import org.railwaystations.rsapi.core.utils.ImageUtil.mimeToExtension
import org.railwaystations.rsapi.core.utils.ImageUtil.scalePhoto
import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.util.stream.Stream
import javax.imageio.ImageIO

internal class ImageUtilTest {
    @ParameterizedTest
    @CsvSource(
        "123123.jpg, jpg", "123123.asdfas.jpeg, jpeg", "123.Jpg, jpg", "123.PNG, png", ",", "456.docx, docx"
    )
    fun testGetExtension(filename: String?, extension: String?) {
        assertThat(extension).isEqualTo(getExtension(filename))
    }

    @ParameterizedTest
    @CsvSource(
        "image/jpeg, jpg", "image/png, png"
    )
    fun testMimeToExtension(contentType: String, extension: String) {
        assertThat(mimeToExtension(contentType)).isEqualTo(extension)
    }

    @ParameterizedTest
    @MethodSource("invalidContentTypes")
    fun testMimeToExtensionException(contentType: String?) {
        assertThat(mimeToExtension(contentType)).isNull()
    }

    @ParameterizedTest
    @CsvSource(
        "jpg, image/jpeg", "jpeg, image/jpeg", "png, image/png"
    )
    fun testExtensionToMimeType(extension: String, mimeType: String) {
        assertThat(mimeType).isEqualTo(extensionToMimeType(extension))
    }

    @ParameterizedTest
    @MethodSource("invalidExtensions")
    fun testExtensionToMimeTypeException(extension: String?) {
        assertThatThrownBy { extensionToMimeType(extension) }
            .isInstanceOf(
                IllegalArgumentException::class.java
            )
    }

    @ParameterizedTest
    @CsvSource(
        "test.jpg, 100, 100", "test.png, 150, 150", "test.jpg,, 200", "test.png,, 200"
    )
    fun testScaleImage(filename: String, newWidth: Int?, expectedWidth: Int) {
        val photo = Path.of("src/test/resources", filename)
        val scaledBytes = scalePhoto(photo, newWidth)
        val scaledImage = ImageIO.read(ByteArrayInputStream(scaledBytes))
        assertThat(expectedWidth).isEqualTo(scaledImage.width)
        assertThat(expectedWidth).isEqualTo(scaledImage.height)
    }

    companion object {

        @JvmStatic
        private fun invalidContentTypes(): Stream<String> {
            return Stream.of("", "   ", null, "image/svg", "application/json")
        }

        @JvmStatic
        private fun invalidExtensions(): Stream<String> {
            return Stream.of("", "   ", null, "image/svg", "application/json")
        }
    }
}
