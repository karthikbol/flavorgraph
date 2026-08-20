package com.flavorgraph.backend.controller;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthControllerTest {
    @Test void reportsServiceHealthWithoutTouchingDatabase() {
        var response=new HealthController().health();
        assertEquals("UP",response.get("status"));
        assertEquals("FlavorGraph Backend",response.get("service"));
    }
}
