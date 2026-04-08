package com.Grownited.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Grownited.entity.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {

    // ✅ Email ke liye safe method (duplicate handle karega)
    Optional<UserEntity> findByEmail(String email);

    // Agar multiple user same email (rare case)
    List<UserEntity> findAllByEmail(String email);

    List<UserEntity> findByRole(String role);

    long countByRole(String role);

    long countByActive(Boolean active);
}