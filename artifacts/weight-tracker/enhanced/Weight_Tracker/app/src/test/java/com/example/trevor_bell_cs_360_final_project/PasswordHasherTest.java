package com.example.trevor_bell_cs_360_final_project;

import static org.junit.Assert.*;
import org.junit.Test;

public class PasswordHasherTest {

    @Test
    public void correctPasswordVerifies() {
        assertTrue(PasswordHasher.verify("hunter2hunter2", PasswordHasher.hash("hunter2hunter2")));
    }

    @Test
    public void wrongPasswordFails() {
        assertFalse(PasswordHasher.verify("wrong", PasswordHasher.hash("right-password")));
    }

    @Test
    public void samePasswordGetsDifferentSalts() {
        assertNotEquals(PasswordHasher.hash("same-password"), PasswordHasher.hash("same-password"));
    }

    @Test
    public void malformedStoredValueFailsSafely() {
        assertFalse(PasswordHasher.verify("x", "plaintext"));
        assertFalse(PasswordHasher.verify("x", null));
        assertFalse(PasswordHasher.verify("x", "abc$!!$!!"));
    }
}
