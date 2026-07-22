package com.backend.libserver.storage;

/** A stored blob: {@code key} is the durable identity, {@code url} is how a browser fetches it. */
public record StoredObject(String key, String url) {}
