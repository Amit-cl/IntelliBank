package com.intellibank;

import com.intellibank.entity.*;
import com.intellibank.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class JpaSchemaValidationTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Test
    @DisplayName("Verify Hibernate creates tables and handles entity relationships correctly")
    void testEntityPersistenceAndRelationships() {
        // 1. Save Role
        Role role = roleRepository.save(new Role(RoleName.CUSTOMER));
        assertNotNull(role.getId());

        // 2. Save User
        User user = new User();
        user.setUsername("alice_smith");
        user.setPassword("hashedpassword123");
        user.getRoles().add(role);
        User savedUser = userRepository.save(user);
        assertNotNull(savedUser.getId());
        assertNotNull(savedUser.getCreatedAt());

        // 3. Save Customer linked 1:1 to User
        Customer customer = new Customer();
        customer.setUser(savedUser);
        customer.setFirstName("Alice");
        customer.setLastName("Smith");
        customer.setPhoneNumber("+1234567890");
        Customer savedCustomer = customerRepository.save(customer);
        assertNotNull(savedCustomer.getId());

        // 4. Save BankAccount linked 1:N to Customer
        BankAccount account = new BankAccount();
        account.setAccountNumber("ACC-1001-2002");
        account.setCustomer(savedCustomer);
        account.setAccountType(AccountType.SAVINGS);
        account.setBalance(new BigDecimal("5000.00"));
        account.setStatus(AccountStatus.ACTIVE);
        BankAccount savedAccount = bankAccountRepository.save(account);

        assertNotNull(savedAccount.getId());
        assertEquals("ACC-1001-2002", savedAccount.getAccountNumber());
        assertEquals(savedCustomer.getId(), savedAccount.getCustomer().getId());

        // 5. Retrieve & Verify Relationships
        Optional<Customer> foundCustomer = customerRepository.findByUserId(savedUser.getId());
        assertTrue(foundCustomer.isPresent());
        assertEquals("Alice", foundCustomer.get().getFirstName());

        var customerAccounts = bankAccountRepository.findByCustomerId(savedCustomer.getId());
        assertEquals(1, customerAccounts.size());
        assertEquals(new BigDecimal("5000.00"), customerAccounts.get(0).getBalance());
    }
}
