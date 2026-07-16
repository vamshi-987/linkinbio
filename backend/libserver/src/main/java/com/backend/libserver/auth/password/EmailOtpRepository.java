package com.backend.libserver.auth.password;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailOtpRepository extends JpaRepository<EmailOtp, UUID> {

    Optional<EmailOtp> findTopByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
            UUID userId, OtpPurpose purpose);

    /** Most recent code regardless of state — drives the resend cooldown. */
    Optional<EmailOtp> findTopByUserIdAndPurposeOrderByCreatedAtDesc(UUID userId, OtpPurpose purpose);

    long countByUserIdAndPurposeAndCreatedAtAfter(UUID userId, OtpPurpose purpose, Instant since);

    @Modifying
    @Query("update EmailOtp o set o.consumed = true "
            + "where o.userId = :userId and o.purpose = :purpose and o.consumed = false")
    void consumeAllForUser(@Param("userId") UUID userId, @Param("purpose") OtpPurpose purpose);
}
