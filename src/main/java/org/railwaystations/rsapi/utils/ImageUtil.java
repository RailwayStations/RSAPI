package org.railwaystations.rsapi.utils;

import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

public class ImageUtil {

    public static final String IMAGE_PNG = "image/png";
    public static final String IMAGE_JPEG = "image/jpeg";

    public static final String JPG = "jpg";
    public static final String JPEG = "jpeg";
    public static final String PNG = "png";

    public static byte[] scalePhoto(final Path photo, final Integer width) throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BufferedImage inputImage = ImageIO.read(photo.toFile());
        final String extension = getExtension(photo.getFileName().toString());
        assert extension != null;
        if (width != null && width > 0 && width < inputImage.getWidth()) {
            final double scale = (double) width / (double) inputImage.getWidth();
            final int height = (int) (inputImage.getHeight() * scale);

            // creates output image
            final BufferedImage outputImage = new BufferedImage(width,
                    height, inputImage.getType());

            // scales the input image to the output image
            final Graphics2D g2d = outputImage.createGraphics();
            g2d.drawImage(inputImage, 0, 0, width, height, null);
            g2d.dispose();

            ImageIO.write(outputImage, extension, os);
            return os.toByteArray();
        }

        ImageIO.write(inputImage, extension, os);
        return os.toByteArray();
    }

    public static String getExtension(final String filename) {
        if (StringUtils.isBlank(filename)) {
            return null;
        }
        final int pos = filename.lastIndexOf(".");
        if (pos > -1 && pos < filename.length()) {
            return filename.toLowerCase(Locale.ROOT).substring(pos + 1);
        }
        return null;
    }

    public static String mimeToExtension(final String contentType) {
        if (StringUtils.isBlank(contentType)) {
            return null;
        }
        return switch (contentType) {
            case IMAGE_PNG -> PNG;
            case IMAGE_JPEG -> JPG;
            default -> null;
        };
    }

    public static String extensionToMimeType(final String extension) {
        if (StringUtils.isBlank(extension)) {
            throw new IllegalArgumentException("Unsupported null extension");
        }
        return switch (extension) {
            case PNG -> IMAGE_PNG;
            case JPEG, JPG -> IMAGE_JPEG;
            default -> throw new IllegalArgumentException("Unsupported extension: " + extension);
        };
    }

}