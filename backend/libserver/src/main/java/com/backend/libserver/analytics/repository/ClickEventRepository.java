package com.backend.libserver.analytics.repository;

import com.backend.libserver.analytics.domain.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent,Long> {
}
