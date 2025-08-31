package com.inventory.repository;

import com.inventory.model.ActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing activation tokens
 */
@Repository
public interface ActivationTokenRepository extends JpaRepository<ActivationToken, Integer> {
    Optional<ActivationToken> findByToken(String token);
    Optional<ActivationToken> findByUserId(Integer userId);
    void deleteByToken(String token);
}