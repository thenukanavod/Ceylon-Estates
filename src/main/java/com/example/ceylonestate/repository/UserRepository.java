package com.example.ceylonestate.repository;

import com.example.ceylonestate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    List<User> findAllByOrderByUsernameAsc();

    /** How many administrator accounts exist (used so the last admin can never be removed). */
    @Query("select count(u) from AdminUser u")
    long countAdmins();

    /**
     * Turns a RegularUser into an AdminUser (or back) by changing the discriminator column.
     * JPA cannot change an entity's class once it is loaded, so this is a direct update.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE app_user SET user_type = :type WHERE id = :id", nativeQuery = true)
    int changeType(@Param("id") Long id, @Param("type") String type);
}
