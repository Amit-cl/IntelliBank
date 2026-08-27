package com.intellibank.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class EntityMappingTest {

    @Test
    @DisplayName("Should create User entity with default fields")
    void testUserEntity() {
        User user = new User();
        user.setUsername("john_doe");
        user.setPassword("secret123");

        assertEquals("john_doe", user.getUsername());
        assertEquals("secret123", user.getPassword());
        assertTrue(user.isEnabled());
    }

    @Test
    @DisplayName("Should create Customer entity linked to User")
    void testCustomerEntity() {
        User user = new User();
        user.setId(1L);
        user.setUsername("customer_user");

        Customer customer = new Customer();
        customer.setId(10L);
        customer.setUser(user);
        customer.setFirstName("John");
        customer.setLastName("Doe");

        assertNotNull(customer.getUser());
        assertEquals(1L, customer.getUser().getId());
        assertEquals("John", customer.getFirstName());
    }

    @Test
    @DisplayName("Should create BankAccount entity with correct balance and status")
    void testBankAccountEntity() {
        Customer customer = new Customer();
        customer.setId(10L);

        BankAccount account = new BankAccount();
        account.setAccountNumber("ACC12345678");
        account.setCustomer(customer);
        account.setAccountType(AccountType.SAVINGS);
        account.setBalance(new BigDecimal("1000.50"));
        account.setStatus(AccountStatus.ACTIVE);

        assertEquals("ACC12345678", account.getAccountNumber());
        assertEquals(AccountType.SAVINGS, account.getAccountType());
        assertEquals(new BigDecimal("1000.50"), account.getBalance());
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
    }
}
