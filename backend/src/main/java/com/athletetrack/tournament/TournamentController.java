package com.athletetrack.tournament;

import com.athletetrack.coach.CoachAssignmentRepository;
import com.athletetrack.user.Role;
import com.athletetrack.user.UserAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {
    private final TournamentFixtureRepository fixtures;
    private final CoachAssignmentRepository assignments;

    public TournamentController(TournamentFixtureRepository fixtures, CoachAssignmentRepository assignments) {
        this.fixtures = fixtures;
        this.assignments = assignments;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLAYER','COACH')")
    public List<TournamentFixture> list(@AuthenticationPrincipal UserAccount user) {
        return user.getRole() == Role.COACH
                ? fixtures.findTop100ByCoachIdOrderByScheduledAtAscIdAsc(user.getId())
                : fixtures.findTop100ByPlayerIdOrderByScheduledAtAscIdAsc(user.getId());
    }

    @PostMapping
    @PreAuthorize("hasRole('COACH')")
    public TournamentFixture create(@AuthenticationPrincipal UserAccount coach,
                                    @Valid @RequestBody CreateRequest request) {
        if (!assignments.existsByCoachIdAndPlayerId(coach.getId(), request.playerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only add tournament fixtures for assigned players.");
        }
        return fixtures.save(new TournamentFixture(request.playerId(), coach.getId(),
                request.tournamentName().trim(), request.opponent().trim(), request.scheduledAt(),
                request.venue().trim(), request.stage() == null ? null : request.stage().trim(),
                request.notes() == null ? null : request.notes().trim()));
    }

    public record CreateRequest(@NotNull Long playerId,
                                @NotBlank @Size(max = 120) String tournamentName,
                                @NotBlank @Size(max = 120) String opponent,
                                @NotNull LocalDateTime scheduledAt,
                                @NotBlank @Size(max = 120) String venue,
                                @Size(max = 80) String stage,
                                @Size(max = 500) String notes) {}
}
