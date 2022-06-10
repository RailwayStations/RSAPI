package org.railwaystations.rsapi.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageUtilTest {

    @ParameterizedTest
    @CsvSource({ "123123.jpg, jpg",
            "123123.asdfas.jpeg, jpeg",
            "123.Jpg, jpg",
            "123.PNG, png",
            ",",
            "456.docx, docx"})
    void testGetExtension(String filename, String extension) {
        assertThat(extension).isEqualTo(ImageUtil.getExtension(filename));
    }

    @ParameterizedTest
    @CsvSource({ "image/jpeg, jpg",
            "image/png, png"})
    void testMimeToExtension(String contentType, String extension) {
        assertThat(ImageUtil.mimeToExtension(contentType)).isEqualTo(extension);
    }

    private static Stream<String> invalidContentTypes() {
        return Stream.of("", "   ", null, "image/svg", "application/json");
    }

    @ParameterizedTest
    @MethodSource("invalidContentTypes")
    void testMimeToExtensionException(String contentType) {
        assertThat(ImageUtil.mimeToExtension(contentType)).isNull();
    }

    @ParameterizedTest
    @CsvSource({ "jpg, image/jpeg",
            "jpeg, image/jpeg",
            "png, image/png"})
    void testExtensionToMimeType(String extension, String mimeType) {
        assertThat(mimeType).isEqualTo(ImageUtil.extensionToMimeType(extension));
    }

    private static Stream<String> invalidExtensions() {
        return Stream.of("", "   ", null, "image/svg", "application/json");
    }

    @ParameterizedTest
    @MethodSource("invalidExtensions")
    void testExtensionToMimeTypeException(String extension) {
        assertThatThrownBy(() -> ImageUtil.extensionToMimeType(extension)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({ "test.jpg, 100, 100",
            "test.png, 150, 150",
            "test.jpg,, 200",
            "test.png,, 200"})
    void testScaleImage(String filename, Integer newWidth, int expectedWidth) throws IOException {
        var photo = Path.of("src/test/resources", filename);
        var scaledBytes = ImageUtil.scalePhoto(photo, newWidth);
        var scaledImage = ImageIO.read(new ByteArrayInputStream(scaledBytes));
        assertThat(expectedWidth).isEqualTo(scaledImage.getWidth());
        assertThat(expectedWidth).isEqualTo(scaledImage.getHeight());
    }

}
