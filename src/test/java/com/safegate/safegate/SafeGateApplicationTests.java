package com.safegate.safegate;

import com.SafeGate.SafeGateApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = SafeGateApplication.class)
@ActiveProfiles("test")
class SafeGateApplicationTests {

    @Test
    void contextLoads() {
    }

}
