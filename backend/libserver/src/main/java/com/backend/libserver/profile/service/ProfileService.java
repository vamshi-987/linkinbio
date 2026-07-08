package com.backend.libserver.profile.service;

import com.backend.libserver.profile.dto.PublicProfileResponse;

public interface ProfileService {

     PublicProfileResponse getPublicProfile(String username);
}
