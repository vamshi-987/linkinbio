package com.backend.libserver.profile;


import com.backend.libserver.TestDataBuilder;
import com.backend.libserver.link.repository.LinkRepository;
import com.backend.libserver.user.domain.User;
import com.backend.libserver.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


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
}
