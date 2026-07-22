package com.backend.libserver.qr.impl;

import com.backend.libserver.qr.QrCodeCache;
import com.backend.libserver.qr.QrCodeService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QrCodeServiceImpl implements QrCodeService {

    private final QrCodeCache cache;

    @Override
    public byte[] pngFor(String cacheKey, String content, int size) {
        int px = clamp(size);
        String key = cacheKey + ":" + px;

        byte[] cached = cache.get(key);
        if (cached != null) return cached;

        byte[] png = render(content, px);
        cache.put(key, png);
        return png;
    }

    private byte[] render(String content, int size) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        // Medium correction recovers ~15% damage — enough for a code printed on a flyer or shown on
        // a screen, without inflating the module count the way H would.
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        // ZXing's default quiet zone is 4 modules, which at these sizes wastes a lot of the image.
        hints.put(EncodeHintType.MARGIN, 1);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("Could not generate QR code", ex);
        }
    }

    /** Clamped rather than rejected: the size is a cosmetic query parameter, not a correctness one. */
    private int clamp(int size) {
        if (size < MIN_SIZE) return MIN_SIZE;
        return Math.min(size, MAX_SIZE);
    }
}
