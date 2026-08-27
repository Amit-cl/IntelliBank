package com.intellibank.service;

import com.intellibank.dto.RegisterRequest;
import com.intellibank.entity.Customer;
import com.intellibank.entity.Role;
import com.intellibank.entity.RoleName;
import com.intellibank.entity.User;
import com.intellibank.exception.RoleNotFoundException;
import com.intellibank.exception.UsernameAlreadyExistsException;
import com.intellibank.repository.CustomerRepository;
import com.intellibank.repository.RoleRepository;
import com.intellibank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private RegisterRequest request;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPhoneNumber("1234567890");

        customerRole = new Role(RoleName.CUSTOMER);
        customerRole.setId(1L);
    }

    @Test
    @DisplayName("Should register customer successfully")
    void testRegisterCustomer_Success() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        User userToSave = new User();
        userToSave.setId(10L);
        userToSave.setUsername("testuser");
        userToSave.setPassword("encodedPassword");
        userToSave.setEnabled(true);

        when(userRepository.save(any(User.class))).thenReturn(userToSave);

        // Act
        User result = userService.registerCustomer(request);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertTrue(result.isEnabled());

        verify(userRepository).existsByUsername("testuser");
        verify(roleRepository).findByName(RoleName.CUSTOMER);
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should throw exception if username already exists")
    void testRegisterCustomer_UsernameExists() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // Act & Assert
        assertThrows(UsernameAlreadyExistsException.class, () -> {
            userService.registerCustomer(request);
        });

        verify(userRepository).existsByUsername("testuser");
        verifyNoMoreInteractions(roleRepository, passwordEncoder, userRepository, customerRepository);
    }

    @Test
    @DisplayName("Should throw exception if CUSTOMER role does not exist")
    void testRegisterCustomer_RoleNotFound() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RoleNotFoundException.class, () -> {
            userService.registerCustomer(request);
        });

        verify(userRepository).existsByUsername("testuser");
        verify(roleRepository).findByName(RoleName.CUSTOMER);
        verifyNoMoreInteractions(passwordEncoder, userRepository, customerRepository);
    }
}
