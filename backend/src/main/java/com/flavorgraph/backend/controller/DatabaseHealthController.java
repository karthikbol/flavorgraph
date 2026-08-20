package com.flavorgraph.backend.controller;

import org.neo4j.driver.Driver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class DatabaseHealthController {

    private final Driver driver;

    public DatabaseHealthController(Driver driver) {
        this.driver = driver;
    }

    @GetMapping("/database")
    public ResponseEntity<Map<String, String>> databaseHealth() {

        try {
            driver.verifyConnectivity();

            return ResponseEntity.ok(
                    Map.of(
                            "status", "UP",
                            "database", "CognoDB"
                    )
            );

        } catch (Exception exception) {

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(
                            Map.of(
                                    "status", "DOWN",
                                    "database", "CognoDB"
                            )
                    );
        }
    }
}
