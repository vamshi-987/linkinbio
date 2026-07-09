package com.backend.libserver.analytics.service.impl;

import com.backend.libserver.analytics.domain.ClickEvent;
import com.backend.libserver.analytics.repository.ClickEventRepository;
import com.backend.libserver.analytics.service.ClickService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClickServiceImpl implements ClickService {

    private final ClickEventRepository clickEventRepository;

    @Async("clickExecutor")
    public void recordClickAsync(UUID linkId, String referrer) {
        ClickEvent event = new ClickEvent(linkId, Instant.now(), referrer);
        clickEventRepository.save(event);
    }

}
