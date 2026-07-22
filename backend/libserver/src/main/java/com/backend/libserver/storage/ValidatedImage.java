package com.backend.libserver.storage;

/** An upload that has been proven to be a real image of an acceptable type and size. */
public record ValidatedImage(byte[] bytes, String contentType, String extension, int width, int height) {}
