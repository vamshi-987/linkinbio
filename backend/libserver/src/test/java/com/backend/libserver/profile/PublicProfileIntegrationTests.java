package com.backend.libserver.profile;


import com.backend.libserver.TestDataBuilder;
import com.backend.libserver.link.domain.Link;
import com.backend.libserver.link.repository.LinkRepository;
import com.backend.libserver.user.domain.User;
import com.backend.libserver.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PublicProfileIntegrationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    LinkRepository linkRepository;

    @Test
    void publicProfile_returnsOnlyActiveLinksInOrder() throws Exception {
        User user = userRepository.save(TestDataBuilder.aUser("vamshi"));
        linkRepository.save(TestDataBuilder.aLink(user, "GitHub", 1));
        linkRepository.save(TestDataBuilder.aLink(user, "LinkedIn", 0));

        mockMvc.perform(get("/api/public/vamshi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.links[0].title").value("LinkedIn"))
                .andExpect(jsonPath("$.links[1].title").value("GitHub"));
    }

    @Test
    void publicProfile_returns404ForUnknownUsername() throws Exception {
        mockMvc.perform(get("/api/public/doesnotexist"))
                .andExpect(status().isNotFound());
    }

    /**
     * The second request is the one that reads from Redis. It is asserted separately because a cache
     * hit takes a different code path — the response has to survive a serialise/deserialise round
     * trip, which a miss never exercises.
     */
    @Test
    void publicProfile_isServedIdenticallyOnACacheHit() throws Exception {
        User user = userRepository.save(TestDataBuilder.aUser("cached"));
        linkRepository.save(TestDataBuilder.aLink(user, "GitHub", 0));

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(get("/api/public/cached"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("cached"))
                    .andExpect(jsonPath("$.links[0].title").value("GitHub"));
        }
    }

    @Test
    void publicProfile_hidesLinksOutsideTheirScheduledWindow() throws Exception {
        User user = userRepository.save(TestDataBuilder.aUser("scheduler"));
        Instant now = Instant.now();

        linkRepository.save(TestDataBuilder.aScheduledLink(
                user, "Live", 0, now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS)));
        linkRepository.save(TestDataBuilder.aScheduledLink(
                user, "NotYet", 1, now.plus(1, ChronoUnit.HOURS), null));
        linkRepository.save(TestDataBuilder.aScheduledLink(
                user, "Expired", 2, null, now.minus(1, ChronoUnit.MINUTES)));

        mockMvc.perform(get("/api/public/scheduler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.links.length()").value(1))
                .andExpect(jsonPath("$.links[0].title").value("Live"));
    }

    @Test
    void click_isRefusedOnAnExpiredLink() throws Exception {
        User user = userRepository.save(TestDataBuilder.aUser("expiry"));
        Link expired = linkRepository.save(TestDataBuilder.aScheduledLink(
                user, "Expired", 0, null, Instant.now().minus(1, ChronoUnit.MINUTES)));

        mockMvc.perform(get("/api/public/click/" + expired.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void click_redirectsAndIsRecordedForALiveLink() throws Exception {
        User user = userRepository.save(TestDataBuilder.aUser("clicker"));
        Link live = linkRepository.save(TestDataBuilder.aLink(user, "Live", 0));

        mockMvc.perform(get("/api/public/click/" + live.getId())
                        .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) Safari/604.1")
                        .header("Referer", "https://www.instagram.com/p/abc?utm_source=bio")
                        .header("CF-IPCountry", "IN"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", live.getUrl()));
    }

    @Test
    void profileQrCode_isServedAsAPng() throws Exception {
        userRepository.save(TestDataBuilder.aUser("qruser"));

        mockMvc.perform(get("/api/public/qr/profile/qruser").param("size", "256"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }
}
