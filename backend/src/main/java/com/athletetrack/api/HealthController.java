package com.athletetrack.api;

import java.time.Instant;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final JdbcTemplate jdbc;
    public HealthController(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @GetMapping public ResponseEntity<HealthResponse> get() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(new HealthResponse("ok", Instant.now()));
        } catch (DataAccessException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new HealthResponse("down", Instant.now()));
        }
    }
    public record HealthResponse(String status, Instant timestamp) {}
}
