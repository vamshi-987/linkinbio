package com.backend.libserver.link.repository;

import com.backend.libserver.link.domain.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LinkRepository extends JpaRepository<Link, UUID> {

    List<Link> findAllByUserIdOrderByPositionAsc(UUID userId);

    @Query("SELECT l FROM Link l WHERE l.user.username = :username AND l.active = true ORDER BY l.position ASC")
    List<Link> findActiveLinksByUsername(@Param("username") String username);

}
