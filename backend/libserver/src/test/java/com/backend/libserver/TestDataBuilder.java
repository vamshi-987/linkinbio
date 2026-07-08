package com.backend.libserver;

import com.backend.libserver.link.domain.Link;
import com.backend.libserver.user.domain.User;

import java.time.Instant;

public class TestDataBuilder {

    public static User aUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setPasswordHash("hashed");
        user.setCreatedAt(Instant.now());
        return user;
    }

    public static Link aLink(User user, String title, int position) {
        Link link = new Link();
        link.setUser(user);
        link.setTitle(title);
        link.setUrl("https://example.com/" + title.toLowerCase());
        link.setPosition(position);
        link.setActive(true);
        return link;
    }
}
