package com.ecommerce.authservice.repository;

import com.ecommerce.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.failedAttempt = :failAttempts WHERE u.username = :username")
    void updateFailedAttempts(@Param("failAttempts") int failAttempts, @Param("username") String username);
    
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.accountNonLocked = :accountNonLocked, u.lockTime = :lockTime WHERE u.username = :username")
    void updateAccountLock(@Param("accountNonLocked") boolean accountNonLocked, 
                          @Param("lockTime") java.time.LocalDateTime lockTime, 
                          @Param("username") String username);
    
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.resetPasswordToken = :token WHERE u.email = :email")
    void updateResetPasswordToken(@Param("token") String token, @Param("email") String email);
    
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.password = :password, u.resetPasswordToken = null WHERE u.resetPasswordToken = :token")
    void updatePassword(@Param("password") String password, @Param("token") String token);
    
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.verificationToken = :code WHERE u.email = :email")
    void updateVerificationCode(@Param("code") String code, @Param("email") String email);
    
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.emailVerified = true, u.verificationToken = null WHERE u.verificationToken = :code")
    int verify(@Param("code") String code);
    
    @Query("SELECT u FROM User u WHERE u.verificationToken = :token")
    Optional<User> findByVerificationToken(@Param("token") String token);
    
    @Query("SELECT u FROM User u WHERE u.resetPasswordToken = :token")
    Optional<User> findByResetPasswordToken(@Param("token") String token);
}
