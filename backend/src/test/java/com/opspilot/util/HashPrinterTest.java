package com.opspilot.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class HashPrinterTest {

    @Test
    void printHashes() {
        var enc = new BCryptPasswordEncoder(12);
        System.out.println("ADMIN HASH: " + enc.encode("admin123"));
        System.out.println("OPERATOR HASH: " + enc.encode("operator123"));
    }
}
