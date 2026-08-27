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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       CustomerRepository customerRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerCustomer(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException("Username '" + request.getUsername() + "' is already taken.");
        }

        // Fetch CUSTOMER role
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new RoleNotFoundException("Default role CUSTOMER not found."));

        // Create new User entity
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.setRoles(Collections.singleton(customerRole));

        // Save user first so we have the auto-generated ID
        User savedUser = userRepository.save(user);

        // Create associated Customer entity
        Customer customer = new Customer();
        customer.setUser(savedUser);
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setPhoneNumber(request.getPhoneNumber());

        customerRepository.save(customer);

        return savedUser;
    }
}
