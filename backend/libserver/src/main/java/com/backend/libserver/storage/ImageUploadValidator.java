package com.backend.libserver.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Gatekeeper for every image upload.
 *
 * <p>The declared content type and filename come from the client and are worth nothing on their own:
 * a request can claim {@code image/png} for a PHP script. So the bytes themselves are checked — magic
 * number first, then a real decode — and the extension we store is derived from what was actually
 * found, never from what was uploaded. Dimensions are bounded as well, because a small file can
 * still decode into a very large raster.
 */
@Component
public class ImageUploadValidator {

    private static final int MIN_DIMENSION = 32;

    @Value("${app.upload.max-bytes:2097152}")
    private long maxBytes;

    @Value("${app.upload.max-dimension:4096}")
    private int maxDimension;

    public ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file was uploaded");
        }
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(
                    "Image must be " + (maxBytes / 1024) + " KB or smaller");
        }

        byte[] bytes = read(file);
        Format format = sniff(bytes);
        BufferedImage image = decode(bytes);

        if (image.getWidth() < MIN_DIMENSION || image.getHeight() < MIN_DIMENSION) {
            throw new IllegalArgumentException(
                    "Image must be at least " + MIN_DIMENSION + "×" + MIN_DIMENSION + " pixels");
        }
        if (image.getWidth() > maxDimension || image.getHeight() > maxDimension) {
            throw new IllegalArgumentException(
                    "Image must be at most " + maxDimension + "×" + maxDimension + " pixels");
        }

        return new ValidatedImage(bytes, format.contentType, format.extension,
                image.getWidth(), image.getHeight());
    }

    private byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not read the uploaded file");
        }
    }

    /** Identifies the format from its magic number — PNG and JPEG only, both decodable everywhere. */
    private Format sniff(byte[] b) {
        if (b.length > 8
                && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return new Format("image/png", "png");
        }
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8) {
            return new Format("image/jpeg", "jpg");
        }
        throw new IllegalArgumentException("Only PNG and JPEG images are supported");
    }

    private BufferedImage decode(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) throw new IllegalArgumentException("That file is not a readable image");
            return image;
        } catch (IOException ex) {
            throw new IllegalArgumentException("That file is not a readable image");
        }
    }

    private record Format(String contentType, String extension) {}
}
