package com.backend.libserver.analytics.service;

import java.util.UUID;

public interface ClickService {

    void recordClickAsync(UUID linkId, String referrer);

}
