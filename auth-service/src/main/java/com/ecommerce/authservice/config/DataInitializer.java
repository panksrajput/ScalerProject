package com.ecommerce.authservice.config;

import com.ecommerce.authservice.entity.User;
import com.ecommerce.authservice.entity.Role;
import com.ecommerce.authservice.repository.UserRepository;
import com.ecommerce.authservice.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createAdminUserIfNotExists();
    }

    private void createAdminUserIfNotExists() {
        // Check if admin user already exists
        if (!userRepository.existsByUsername("admin")) {
            // Create roles if they don't exist
            Role adminRole = roleRepository.findByName(Role.ERole.ROLE_ADMIN)
                .orElseGet(() -> {
                    Role role = new Role(Role.ERole.ROLE_ADMIN);
                    return roleRepository.save(role);
                });

            // Ensure USER role exists
            roleRepository.findByName(Role.ERole.ROLE_USER)
                .orElseGet(() -> {
                    Role role = new Role(Role.ERole.ROLE_USER);
                    return roleRepository.save(role);
                });

            // Create admin user
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@example.com");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setEmailVerified(true);
            admin.setAccountNonLocked(true);
            admin.setRoles(new HashSet<>(Collections.singleton(adminRole)));
            
            // Set enabled status using reflection since the field might be from a superclass
            try {
                Field enabledField = User.class.getSuperclass().getDeclaredField("enabled");
                enabledField.setAccessible(true);
                enabledField.set(admin, true);
            } catch (Exception e) {
                // If reflection fails, try alternative approach
                try {
                    Method setEnabled = User.class.getMethod("setEnabled", boolean.class);
                    setEnabled.invoke(admin, true);
                } catch (Exception ex) {
                    System.err.println("Warning: Could not set enabled status for admin user");
                    ex.printStackTrace();
                }
            }
            
            userRepository.save(admin);
            
            System.out.println("==================================================");
            System.out.println("Admin user created with username: admin");
            System.out.println("Password: Admin@123");
            System.out.println("==================================================");
        }
    }
}
