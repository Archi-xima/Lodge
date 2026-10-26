package com.lodge.service;

import com.lodge.model.Role;
import com.lodge.model.User;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class AuthServiceBootstrapTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Path temporaryDirectory;

    @Before
    public void setUp() {
        temporaryDirectory = temporaryFolder.getRoot().toPath();
    }

    @Test
    public void removesLegacyDemoAccountsAndKeepsStaffAccounts() {
        DataStore store = new DataStore(temporaryDirectory);
        store.saveUsers(List.of(
                new User("admin", "old-password", "Demo Administrator", Role.ADMIN),
                new User("manager", "old-password", "Demo Manager", Role.MANAGER),
                new User("reception", "old-password", "Demo Reception", Role.RECEPTIONIST),
                new User("accounts", "old-password", "Demo Accounts", Role.ACCOUNTANT),
                new User("student", "old-password", "Demo Student", Role.RECEPTIONIST),
                new User("night-clerk", "private-password", "Night Clerk", Role.RECEPTIONIST)
        ));

        AuthService auth = new AuthService(store);

        assertNull(auth.findByUsername("admin"));
        assertNull(auth.findByUsername("manager"));
        assertNull(auth.findByUsername("reception"));
        assertNull(auth.findByUsername("accounts"));
        assertNull(auth.findByUsername("student"));
        assertNotNull(auth.findByUsername("night-clerk"));
        assertTrue(auth.needsInitialAdministratorSetup());
    }

    @Test
    public void createsAndPersistsTheFirstAdministratorWithoutExposingPassword() throws Exception {
        DataStore store = new DataStore(temporaryDirectory);
        AuthService auth = new AuthService(store);

        assertTrue(auth.needsInitialAdministratorSetup());
        assertFalse(auth.createInitialAdministrator("Achizima", "Achizima", "short"));
        assertTrue(auth.createInitialAdministrator("Achizima", "Achizima", "Strong-Test-Password"));
        assertFalse(auth.needsInitialAdministratorSetup());
        assertNull(auth.getCurrentUser());
        assertTrue(auth.login("Achizima", "Strong-Test-Password"));
        assertEquals(Role.ADMIN, auth.getCurrentUser().getRole());
        auth.logout();
        assertFalse(auth.login("Achizima", "incorrect-password"));
        assertFalse(auth.login("admin", "admin123"));

        String savedUsers = Files.readString(temporaryDirectory.resolve(DataStore.FILE_USERS));
        assertFalse(savedUsers.contains("Strong-Test-Password"));
        assertTrue(savedUsers.contains("passwordHash"));

        AuthService restartedAuth = new AuthService(store);
        assertTrue(restartedAuth.login("Achizima", "Strong-Test-Password"));
    }

    @Test
    public void restrictsStaffAccountChangesToTheSignedInAdministrator() {
        DataStore store = new DataStore(temporaryDirectory);
        AuthService auth = new AuthService(store);
        assertTrue(auth.createInitialAdministrator("Achizima", "Achizima", "Strong-Test-Password"));

        User receptionist = new User("receptionist", "Front-Desk-Password", "Front Desk", Role.RECEPTIONIST);
        assertFalse(auth.register(receptionist.getUsername(), "Front-Desk-Password",
                receptionist.getFullName(), receptionist.getRole()));

        assertTrue(auth.login("Achizima", "Strong-Test-Password"));
        assertFalse(auth.register("not-a-customer", "Customer-Password",
                "Customer", Role.CUSTOMER));
        assertTrue(auth.register(receptionist.getUsername(), "Front-Desk-Password",
                receptionist.getFullName(), receptionist.getRole()));
        assertTrue(auth.getUsers().stream().anyMatch(user -> "receptionist".equals(user.getUsername())));
        assertTrue(auth.register("manager", "Manager-Account-Password",
                "Lodge Manager", Role.MANAGER));
        assertTrue(auth.register("another-admin", "Another-Strong-Password",
                "Another Administrator", Role.ADMIN));

        AuthService restartedAuth = new AuthService(store);
        assertTrue(restartedAuth.login("manager", "Manager-Account-Password"));
    }

    @Test
    public void preservesExistingAdministratorAndDoesNotAllowPublicRegistration() {
        DataStore store = new DataStore(temporaryDirectory);
        store.saveUsers(List.of(
                new User("operations", "private-password", "Operations Lead", Role.ADMIN)
        ));

        AuthService auth = new AuthService(store);

        assertFalse(auth.needsInitialAdministratorSetup());
        assertFalse(auth.register("attacker", "Unsafe-Password", "Untrusted User", Role.ADMIN));
        assertTrue(auth.login("operations", "private-password"));
    }

    @Test
    public void customerAccountsPersistWithLinkedGuestProfilesAndNoStaffPermissions() {
        DataStore store = new DataStore(temporaryDirectory);
        GuestService guests = new GuestService(store);
        AuthService auth = new AuthService(store, guests);

        assertTrue(auth.registerCustomer("traveller", "Customer-Password", "Taylor Guest",
                "taylor@example.com", "+260 977 111 222"));
        assertFalse(auth.registerCustomer("traveller", "Another-Password", "Other Guest",
                "other@example.com", ""));

        User customer = auth.findByUsername("traveller");
        assertNotNull(customer);
        assertEquals(Role.CUSTOMER, customer.getRole());
        assertNotNull(customer.getGuestId());
        assertFalse(customer.can(Role.Permission.MANAGE_USERS));
        assertNotNull(guests.findById(customer.getGuestId()));

        AuthService restartedAuth = new AuthService(store, new GuestService(store));
        assertTrue(restartedAuth.login("traveller", "Customer-Password"));
        assertEquals(customer.getGuestId(), restartedAuth.getCurrentUser().getGuestId());
        assertFalse(restartedAuth.register("staff", "Staff-Password", "Staff Member", Role.RECEPTIONIST));
    }

    @Test
    public void administratorCanDeleteCustomerAccountWithoutRemovingGuestOrBookingHistory() {
        DataStore store = new DataStore(temporaryDirectory);
        GuestService guests = new GuestService(store);
        AuthService auth = new AuthService(store, guests);
        assertTrue(auth.createInitialAdministrator("Achizima", "Achizima", "Admin-Password"));
        assertTrue(auth.login("Achizima", "Admin-Password"));
        assertTrue(auth.registerCustomer("traveller", "Customer-Password", "Taylor Guest",
                "taylor@example.com", "+260 977 111 222"));

        User customer = auth.findByUsername("traveller");
        String guestId = customer.getGuestId();
        assertTrue(auth.removeUser(customer));
        assertNull(auth.findByUsername("traveller"));
        assertNotNull(guests.findById(guestId));

        AuthService restartedAuth = new AuthService(store, new GuestService(store));
        assertNull(restartedAuth.findByUsername("traveller"));
        assertTrue(restartedAuth.login("Achizima", "Admin-Password"));
        assertFalse(restartedAuth.login("traveller", "Customer-Password"));
    }
}
