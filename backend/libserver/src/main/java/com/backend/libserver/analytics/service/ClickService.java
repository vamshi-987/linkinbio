package com.backend.libserver.analytics.service;

import com.backend.libserver.analytics.ClickContext;

import java.util.UUID;

public interface ClickService {

    /**
     * Records a click off the request thread. The visitor's redirect must not wait on the insert —
     * or on enrichment — so this returns immediately and never propagates a failure to the caller.
     */
    void recordClickAsync(UUID linkId, ClickContext context);

}
