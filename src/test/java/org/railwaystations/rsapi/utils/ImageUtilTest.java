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

public class ImageUtilTest {

    @ParameterizedTest
    @CsvSource({ "123123.jpg, jpg",
            "123123.asdfas.jpeg, jpeg",
            "123.Jpg, jpg",
            "123.PNG, png",
            ",",
            "456.docx, docx"})
    public void testGetExtension(final String filename, final String extension) {
        assertThat(extension).isEqualTo(ImageUtil.getExtension(filename));
    }

    @ParameterizedTest
    @CsvSource({ "image/jpeg, jpg",
            "image/png, png"})
    public void testMimeToExtension(final String contentType, final String extension) {
        assertThat(ImageUtil.mimeToExtension(contentType)).isEqualTo(extension);
    }

    public static Stream<String> invalidContentTypes() {
        return Stream.of("", "   ", null, "image/svg", "application/json");
    }

    @ParameterizedTest
    @MethodSource("invalidContentTypes")
    public void testMimeToExtensionException(final String contentType) {
        assertThat(ImageUtil.mimeToExtension(contentType)).isNull();
    }

    @ParameterizedTest
    @CsvSource({ "jpg, image/jpeg",
            "jpeg, image/jpeg",
            "png, image/png"})
    public void testExtensionToMimeType(final String extension, final String mimeType) {
        assertThat(mimeType).isEqualTo(ImageUtil.extensionToMimeType(extension));
    }

    public static Stream<String> invalidExtensions() {
        return Stream.of("", "   ", null, "image/svg", "application/json");
    }

    @ParameterizedTest
    @MethodSource("invalidExtensions")
    public void testExtensionToMimeTypeException(final String extension) {
        assertThatThrownBy(() -> ImageUtil.extensionToMimeType(extension)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({ "test.jpg, 100, 100",
            "test.png, 150, 150",
            "test.jpg,, 200",
            "test.png,, 200"})
    public void testScaleImage(final String filename, final Integer newWidth, final int expectedWidth) throws IOException {
        final var photo = Path.of("src/test/resources", filename);
        final var scaledBytes = ImageUtil.scalePhoto(photo, newWidth);
        final var scaledImage = ImageIO.read(new ByteArrayInputStream(scaledBytes));
        assertThat(expectedWidth).isEqualTo(scaledImage.getWidth());
        assertThat(expectedWidth).isEqualTo(scaledImage.getHeight());
    }

}
