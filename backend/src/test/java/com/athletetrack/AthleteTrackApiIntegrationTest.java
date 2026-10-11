package com.athletetrack;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "athletetrack.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWYwMTIzNDU2Nzg=",
                "athletetrack.coach.registration-code=test-coach-invitation"
        })
@ActiveProfiles("h2")
class AthleteTrackApiIntegrationTest {
    @Autowired
    private TestRestTemplate client;

    @Test
    void healthReportsDatabaseReadiness() {
        var response = client.getForEntity("/api/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("ok");
    }

    @Test
    void registersPlayerAndPersistsProfileWithCalculatedBmi() {
        String email = "player-" + UUID.randomUUID() + "@example.test";
        Map<?, ?> registration = client.postForObject("/api/auth/register",
                Map.of("email", email, "password", "AthleteTrack!2026", "displayName", "Test Player"), Map.class);
        assertThat(registration).isNotNull();
        Map<?, ?> account = (Map<?, ?>) registration.get("account");
        assertThat(account.get("role")).isEqualTo("PLAYER");
        String token = (String) registration.get("token");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        var profileBeforeUpdate = client.exchange("/api/profile", HttpMethod.GET,
                new HttpEntity<>(headers), Map.class);
        client.exchange("/api/profile", HttpMethod.PUT,
                new HttpEntity<>(Map.of("age", 24, "heightCm", 175, "weightKg", 70,
                        "sport", "Track", "goal", "Improve endurance"), headers), Map.class);

        Map<?, ?> profile = client.exchange("/api/profile", HttpMethod.GET,
                new HttpEntity<>(headers), Map.class).getBody();
        assertThat(profileBeforeUpdate.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(profileBeforeUpdate.getBody().get("userId")).isEqualTo(account.get("id"));
        assertThat(profile).isNotNull();
        assertThat(((Number) profile.get("bmi")).doubleValue()).isEqualTo(22.9);
        assertThat(profile.get("sport")).isEqualTo("Track");
    }

    @Test
    void playerCannotAccessCoachEndpoints() {
        String email = "player-" + UUID.randomUUID() + "@example.test";
        Map<?, ?> registration = client.postForObject("/api/auth/register",
                Map.of("email", email, "password", "AthleteTrack!2026", "displayName", "Test Player"), Map.class);
        String token = (String) registration.get("token");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        var response = client.exchange("/api/coach/players", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void coachCannotEditPlayerProfile() {
        Map<?, ?> registration = registerCoach("coach-" + UUID.randomUUID() + "@example.test");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth((String) registration.get("token"));
        headers.setContentType(MediaType.APPLICATION_JSON);

        var response = client.exchange("/api/profile", HttpMethod.PUT,
                new HttpEntity<>(Map.of("age", 24), headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void coachCanAssignPlayerByRegisteredEmail() {
        String playerEmail = "player-" + UUID.randomUUID() + "@example.test";
        client.postForObject("/api/auth/register",
                Map.of("email", playerEmail, "password", "AthleteTrack!2026", "displayName", "Email Player"), Map.class);
        Map<?, ?> coachRegistration = registerCoach("coach-" + UUID.randomUUID() + "@example.test");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth((String) coachRegistration.get("token"));
        headers.setContentType(MediaType.APPLICATION_JSON);

        var response = client.postForEntity("/api/coach/players",
                new HttpEntity<>(Map.of("email", playerEmail.toUpperCase()), headers), Map.class);
        var assignedPlayers = client.exchange("/api/coach/players", HttpMethod.GET,
                new HttpEntity<>(headers), java.util.List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("email")).isEqualTo(playerEmail);
        assertThat(assignedPlayers.getBody()).hasSize(1);
    }

    @Test
    void sessionEndpointReportsAuthenticatedAccountRole() {
        String email = "player-" + UUID.randomUUID() + "@example.test";
        Map<?, ?> registration = client.postForObject("/api/auth/register",
                Map.of("email", email, "password", "AthleteTrack!2026", "displayName", "Test Player"), Map.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth((String) registration.get("token"));

        var session = client.exchange("/api/auth/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        var anonymous = client.getForEntity("/api/auth/me", Map.class);

        assertThat(session.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(session.getBody().get("role")).isEqualTo("PLAYER");
        assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginRequiresTheSelectedAccountRole() {
        String email = "role-" + UUID.randomUUID() + "@example.test";
        client.postForObject("/api/auth/register",
                Map.of("email", email, "password", "AthleteTrack!2026", "displayName", "Role Test",
                        "role", "COACH"), Map.class);

        var playerLogin = client.postForEntity("/api/auth/login",
                Map.of("email", email, "password", "AthleteTrack!2026", "role", "PLAYER"), Map.class);
        var coachLogin = client.postForEntity("/api/auth/login",
                Map.of("email", email, "password", "AthleteTrack!2026", "role", "COACH"), Map.class);

        assertThat(playerLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) playerLogin.getBody().get("account")).get("role")).isEqualTo("PLAYER");
        assertThat(coachLogin.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void coachRegistrationRequiresInvitationCode() {
        String email = "coach-" + UUID.randomUUID() + "@example.test";
        Map<String, Object> request = Map.of("email", email, "password", "AthleteTrack!2026",
                "displayName", "Test Coach", "registrationCode", "incorrect-code");
        var rejected = client.postForEntity("/api/auth/register-coach", request, Map.class);
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        var accepted = client.postForEntity("/api/auth/register-coach",
                Map.of("email", email, "password", "AthleteTrack!2026", "displayName", "Test Coach",
                        "registrationCode", "test-coach-invitation"), Map.class);

        assertThat(accepted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) accepted.getBody().get("account")).get("role")).isEqualTo("COACH");
    }

    @Test
    void coachCanReadPerformanceOnlyForAssignedPlayers() {
        Map<?, ?> playerRegistration = client.postForObject("/api/auth/register",
                Map.of("email", "player-" + UUID.randomUUID() + "@example.test",
                        "password", "AthleteTrack!2026", "displayName", "Assigned Player"), Map.class);
        Map<?, ?> coachRegistration = registerCoach("coach-" + UUID.randomUUID() + "@example.test");
        Map<?, ?> otherCoachRegistration = registerCoach("coach-" + UUID.randomUUID() + "@example.test");
        Long playerId = ((Number) ((Map<?, ?>) playerRegistration.get("account")).get("id")).longValue();
        String playerToken = (String) playerRegistration.get("token");
        String coachToken = (String) coachRegistration.get("token");
        String otherCoachToken = (String) otherCoachRegistration.get("token");
        HttpHeaders playerHeaders = new HttpHeaders();
        playerHeaders.setBearerAuth(playerToken);
        playerHeaders.setContentType(MediaType.APPLICATION_JSON);

        var createdEntry = client.postForEntity("/api/performance",
                new HttpEntity<>(Map.of("metric", "Sprint", "value", 12.4, "unit", "sec"), playerHeaders), Map.class);
        assertThat(createdEntry.getStatusCode()).isEqualTo(HttpStatus.OK);
        HttpHeaders coachHeaders = new HttpHeaders();
        coachHeaders.setBearerAuth(coachToken);
        HttpHeaders otherCoachHeaders = new HttpHeaders();
        otherCoachHeaders.setBearerAuth(otherCoachToken);

        var beforeAssignment = client.exchange("/api/coach/players/" + playerId + "/performance",
                HttpMethod.GET, new HttpEntity<>(coachHeaders), String.class);
        assertThat(beforeAssignment.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        var assignment = client.exchange("/api/coach/players", HttpMethod.POST,
                new HttpEntity<>(Map.of("playerId", playerId), coachHeaders), Map.class);
        assertThat(assignment.getStatusCode()).isEqualTo(HttpStatus.OK);
        var assignedPerformance = client.exchange("/api/coach/players/" + playerId + "/performance",
                HttpMethod.GET, new HttpEntity<>(coachHeaders), java.util.List.class);
        var otherCoachPerformance = client.exchange("/api/coach/players/" + playerId + "/performance",
                HttpMethod.GET, new HttpEntity<>(otherCoachHeaders), String.class);

        assertThat(assignedPerformance.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(assignedPerformance.getBody()).hasSize(1);
        assertThat(otherCoachPerformance.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void coachCanCreatePlansAndFeedbackOnlyForAssignedPlayers() {
        Map<?, ?> playerRegistration = client.postForObject("/api/auth/register",
                Map.of("email", "player-" + UUID.randomUUID() + "@example.test",
                        "password", "AthleteTrack!2026", "displayName", "Plan Player"), Map.class);
        Map<?, ?> coachRegistration = registerCoach("coach-" + UUID.randomUUID() + "@example.test");
        Map<?, ?> otherCoachRegistration = registerCoach("coach-" + UUID.randomUUID() + "@example.test");
        Long playerId = ((Number) ((Map<?, ?>) playerRegistration.get("account")).get("id")).longValue();
        HttpHeaders coachHeaders = new HttpHeaders();
        coachHeaders.setBearerAuth((String) coachRegistration.get("token"));
        coachHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpHeaders otherCoachHeaders = new HttpHeaders();
        otherCoachHeaders.setBearerAuth((String) otherCoachRegistration.get("token"));
        otherCoachHeaders.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> plan = Map.of("playerId", playerId, "type", "TRAINING",
                "title", "Speed work", "details", "Acceleration intervals");
        Map<String, Object> feedback = Map.of("playerId", playerId, "message", "Great progress.");
        assertThat(client.postForEntity("/api/plans", new HttpEntity<>(plan, coachHeaders), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(client.postForEntity("/api/feedback", new HttpEntity<>(feedback, coachHeaders), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(client.postForEntity("/api/coach/players",
                new HttpEntity<>(Map.of("playerId", playerId), coachHeaders), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(client.postForEntity("/api/plans", new HttpEntity<>(plan, coachHeaders), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(client.postForEntity("/api/feedback", new HttpEntity<>(feedback, coachHeaders), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(client.postForEntity("/api/plans", new HttpEntity<>(plan, otherCoachHeaders), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(client.postForEntity("/api/feedback", new HttpEntity<>(feedback, otherCoachHeaders), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void coachCanCreateTournamentFixturesOnlyForAssignedPlayersAndPlayerCanViewOwnSchedule() {
        Map<?, ?> playerRegistration = client.postForObject("/api/auth/register",
                Map.of("email", "player-" + UUID.randomUUID() + "@example.test",
                        "password", "AthleteTrack!2026", "displayName", "Tournament Player"), Map.class);
        Map<?, ?> otherPlayerRegistration = client.postForObject("/api/auth/register",
                Map.of("email", "player-" + UUID.randomUUID() + "@example.test",
                        "password", "AthleteTrack!2026", "displayName", "Other Player"), Map.class);
        Map<?, ?> coachRegistration = registerCoach("coach-" + UUID.randomUUID() + "@example.test");
        Long playerId = ((Number) ((Map<?, ?>) playerRegistration.get("account")).get("id")).longValue();
        String playerToken = (String) playerRegistration.get("token");
        HttpHeaders coachHeaders = new HttpHeaders();
        coachHeaders.setBearerAuth((String) coachRegistration.get("token"));
        coachHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpHeaders playerHeaders = new HttpHeaders();
        playerHeaders.setBearerAuth(playerToken);
        HttpHeaders otherPlayerHeaders = new HttpHeaders();
        otherPlayerHeaders.setBearerAuth((String) otherPlayerRegistration.get("token"));
        Map<String, Object> fixture = Map.of("playerId", playerId, "tournamentName", "State Championship",
                "opponent", "Northside", "scheduledAt", "2026-11-02T15:30:00",
                "venue", "Central Stadium", "stage", "Semi-final");

        var denied = client.postForEntity("/api/tournaments",
                new HttpEntity<>(fixture, coachHeaders), String.class);
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(client.postForEntity("/api/coach/players",
                new HttpEntity<>(Map.of("playerId", playerId), coachHeaders), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        var created = client.postForEntity("/api/tournaments",
                new HttpEntity<>(fixture, coachHeaders), Map.class);
        var playerSchedule = client.exchange("/api/tournaments", HttpMethod.GET,
                new HttpEntity<>(playerHeaders), java.util.List.class);
        var otherPlayerSchedule = client.exchange("/api/tournaments", HttpMethod.GET,
                new HttpEntity<>(otherPlayerHeaders), java.util.List.class);
        var coachSchedule = client.exchange("/api/tournaments", HttpMethod.GET,
                new HttpEntity<>(coachHeaders), java.util.List.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(playerSchedule.getBody()).hasSize(1);
        assertThat(otherPlayerSchedule.getBody()).isEmpty();
        assertThat(coachSchedule.getBody()).hasSize(1);
    }

    private Map<?, ?> registerCoach(String email) {
        return client.postForObject("/api/auth/register-coach",
                Map.of("email", email, "password", "AthleteTrack!2026", "displayName", "Test Coach",
                        "registrationCode", "test-coach-invitation"), Map.class);
    }
}
