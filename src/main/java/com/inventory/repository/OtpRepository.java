package com.inventory.repository;

import com.inventory.model.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Integer> {
    Optional<Otp> findByUserIdAndNewEmail(Integer userId, String newEmail);
    Optional<Otp> findByUserIdAndEmailAndVerificationType(Integer userId, String email, String verificationType);
    Optional<Otp> findByEmailAndVerificationType(String email, String verificationType);
    void deleteByUserIdAndVerificationType(Integer userId, String verificationType);
    void deleteByEmailAndVerificationType(String email, String verificationType);
}
