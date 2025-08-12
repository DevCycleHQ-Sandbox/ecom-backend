package com.shopper.repository.secondary;

import com.shopper.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SecondaryUserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'ADMIN'")
    long countAdmins();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'USER'")
    long countUsers();
    
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM cart_items WHERE user_id = (SELECT id FROM users WHERE email = :email)", nativeQuery = true)
    void deleteCartItemsByUserEmail(@Param("email") String email);
    
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM cart_items WHERE user_id = (SELECT id FROM users WHERE username = :username)", nativeQuery = true)
    void deleteCartItemsByUserUsername(@Param("username") String username);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM users WHERE email = :email", nativeQuery = true)
    void deleteByEmail(@Param("email") String email);
    
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM users WHERE username = :username", nativeQuery = true)
    void deleteByUsername(@Param("username") String username);
    
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO users (id, username, email, password, role, created_at, updated_at) " +
                   "VALUES (:id, :username, :email, :password, :role, :createdAt, :updatedAt)", 
           nativeQuery = true)
    void saveWithSpecificId(@Param("id") String id,
                          @Param("username") String username,
                          @Param("email") String email,
                          @Param("password") String password,
                          @Param("role") String role,
                          @Param("createdAt") LocalDateTime createdAt,
                          @Param("updatedAt") LocalDateTime updatedAt);
} 