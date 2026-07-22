package com.backend.libserver.link;

import com.backend.libserver.link.domain.Link;
import com.backend.libserver.link.repository.LinkRepository;
import com.backend.libserver.link.service.impl.LinkServiceImpl;
import com.backend.libserver.profile.service.ProfileService;
import com.backend.libserver.storage.ImageUploadValidator;
import com.backend.libserver.storage.MediaUrlResolver;
import com.backend.libserver.storage.StorageService;
import com.backend.libserver.user.domain.User;
import com.backend.libserver.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static com.backend.libserver.TestDataBuilder.aLink;
import static com.backend.libserver.TestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reorder has to accept exactly a permutation of the caller's own links. Anything else would leave
 * two links sharing a position, which makes the order the public page renders non-deterministic.
 */
@ExtendWith(MockitoExtension.class)
class LinkReorderTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private LinkRepository linkRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProfileService profileService;
    @Mock private StorageService storageService;
    @Mock private MediaUrlResolver mediaUrlResolver;
    @Mock private ImageUploadValidator imageUploadValidator;

    @InjectMocks private LinkServiceImpl linkService;

    private Link first;
    private Link second;

    @BeforeEach
    void setUp() {
        User user = aUser("owner");
        first = aLink(user, "First", 0);
        first.setId(UUID.randomUUID());
        second = aLink(user, "Second", 1);
        second.setId(UUID.randomUUID());

        when(linkRepository.findAllByUserIdOrderByPositionAsc(USER_ID))
                .thenReturn(List.of(first, second));
    }

    @Test
    void permutationIsApplied() {
        linkService.reorder(USER_ID, List.of(second.getId(), first.getId()));

        assertThat(second.getPosition()).isZero();
        assertThat(first.getPosition()).isEqualTo(1);
    }

    /** The size check alone passed this: same count, but one link repeated and the other dropped. */
    @Test
    void repeatedIdIsRejected() {
        assertThatThrownBy(() -> linkService.reorder(USER_ID, List.of(second.getId(), second.getId())))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(first.getPosition()).isZero();
        assertThat(second.getPosition()).isEqualTo(1);
        verify(linkRepository, never()).saveAll(any());
    }

    @Test
    void idFromAnotherUserIsRejected() {
        assertThatThrownBy(() -> linkService.reorder(USER_ID, List.of(first.getId(), UUID.randomUUID())))
                .isInstanceOf(IllegalArgumentException.class);

        verify(linkRepository, never()).saveAll(any());
    }

    @Test
    void wrongLengthIsRejected() {
        assertThatThrownBy(() -> linkService.reorder(USER_ID, List.of(first.getId())))
                .isInstanceOf(IllegalArgumentException.class);

        verify(linkRepository, never()).saveAll(any());
    }
}
