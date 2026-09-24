package com.example.ceylonestate.repository;

import com.example.ceylonestate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Gives us CRUD methods (save, findById, findAll, delete, etc.) for free.
 * Works across the whole User hierarchy (RegularUser + AdminUser) automatically,
 * since they're all stored in the same table via SINGLE_TABLE inheritance.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByResetToken(String resetToken);
}
