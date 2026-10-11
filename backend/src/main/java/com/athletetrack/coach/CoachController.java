package com.athletetrack.coach;

import com.athletetrack.user.Role;
import com.athletetrack.user.UserAccount;
import com.athletetrack.user.UserAccountRepository;
import com.athletetrack.performance.PerformanceEntry;
import com.athletetrack.performance.PerformanceEntryRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/coach/players")
@PreAuthorize("hasRole('COACH')")
public class CoachController {
    private final CoachAssignmentRepository assignments;
    private final UserAccountRepository users;
    private final PerformanceEntryRepository performance;
    public CoachController(CoachAssignmentRepository assignments,UserAccountRepository users,
                           PerformanceEntryRepository performance){
        this.assignments=assignments;this.users=users;this.performance=performance;
    }
    @GetMapping public List<PlayerView> list(@AuthenticationPrincipal UserAccount coach) {
        return assignments.findAllByCoachIdOrderByAssignedAtDesc(coach.getId()).stream().map(a -> {
            UserAccount player=users.findById(a.getPlayerId()).orElseThrow(()->
                    new ResponseStatusException(HttpStatus.CONFLICT,"An assigned player account no longer exists."));
            return new PlayerView(player.getId(),player.getDisplayName(),player.getEmail(),a.getAssignedAt());
        }).toList();
    }
    @PostMapping public PlayerView assign(@AuthenticationPrincipal UserAccount coach,@Valid @RequestBody AssignRequest request) {
        UserAccount player;
        if (request.email() != null && !request.email().isBlank()) {
            player = users.findByEmailIgnoreCase(request.email().trim()).filter(u -> u.getRole() == Role.PLAYER)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "No player account was found with that email."));
        } else if (request.playerId() != null) {
            player = users.findById(request.playerId()).filter(u -> u.getRole() == Role.PLAYER)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player account not found."));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter the email used by the player's account.");
        }
        if(assignments.existsByCoachIdAndPlayerId(coach.getId(),player.getId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Player is already assigned.");
        CoachAssignment assignment=assignments.save(new CoachAssignment(coach.getId(),player.getId()));
        return new PlayerView(player.getId(),player.getDisplayName(),player.getEmail(),assignment.getAssignedAt());
    }
    @GetMapping("/{playerId}/performance")
    public List<PerformanceEntry> performance(@AuthenticationPrincipal UserAccount coach,@PathVariable Long playerId) {
        if(!assignments.existsByCoachIdAndPlayerId(coach.getId(),playerId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You can only view performance for assigned players.");
        return performance.findTop50ByPlayerIdOrderByRecordedAtDescIdDesc(playerId);
    }
    public record AssignRequest(Long playerId, @Email @Size(max=254) String email){}
    public record PlayerView(Long id,String displayName,String email,Instant assignedAt){}
}
