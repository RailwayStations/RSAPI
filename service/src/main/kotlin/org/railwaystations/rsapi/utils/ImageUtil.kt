package org.railwaystations.rsapi.utils

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.Path
import javax.imageio.ImageIO

object ImageUtil {

    private const val IMAGE_PNG: String = "image/png"
    private const val IMAGE_JPEG: String = "image/jpeg"

    private const val JPG: String = "jpg"
    private const val JPEG: String = "jpeg"
    private const val PNG: String = "png"

    @Throws(IOException::class)
    @JvmStatic
    fun scalePhoto(photo: Path, width: Int?): ByteArray {
        val os = ByteArrayOutputStream()
        val inputImage = ImageIO.read(photo.toFile())
        val extension = getExtension(photo.fileName.toString())!!
        if (width != null && width > 0 && width < inputImage.width) {
            val scale = width.toDouble() / inputImage.width.toDouble()
            val height = (inputImage.height * scale).toInt()

            // creates output image
            val outputImage = BufferedImage(width, height, inputImage.type)

            // scales the input image to the output image
            val g2d = outputImage.createGraphics()
            g2d.drawImage(inputImage, 0, 0, width, height, null)
            g2d.dispose()

            ImageIO.write(outputImage, extension, os)
            return os.toByteArray()
        }

        ImageIO.write(inputImage, extension, os)
        return os.toByteArray()
    }

    @JvmStatic
    fun getExtension(filename: String?): String? {
        if (filename.isNullOrBlank()) {
            return null
        }
        val pos = filename.lastIndexOf(".")
        if (pos > -1) {
            return filename.lowercase().substring(pos + 1)
        }
        return null
    }

    @JvmStatic
    fun mimeToExtension(contentType: String?): String? {
        if (contentType.isNullOrBlank()) {
            return null
        }
        return when (contentType) {
            IMAGE_PNG -> PNG
            IMAGE_JPEG -> JPG
            else -> null
        }
    }

    @JvmStatic
    fun extensionToMimeType(extension: String?): String {
        return when (extension) {
            PNG -> IMAGE_PNG
            JPEG, JPG -> IMAGE_JPEG
            else -> throw IllegalArgumentException("Unsupported extension: $extension")
        }
    }

}