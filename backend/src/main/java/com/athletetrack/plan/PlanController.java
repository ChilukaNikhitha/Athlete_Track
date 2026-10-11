package com.athletetrack.plan;

import com.athletetrack.coach.CoachAssignmentRepository;
import com.athletetrack.user.Role;
import com.athletetrack.user.UserAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/plans")
public class PlanController {
    private final WellnessPlanRepository plans;
    private final CoachAssignmentRepository assignments;
    public PlanController(WellnessPlanRepository plans,CoachAssignmentRepository assignments){this.plans=plans;this.assignments=assignments;}
    @GetMapping @PreAuthorize("hasAnyRole('PLAYER','COACH')")
    public List<WellnessPlan> list(@AuthenticationPrincipal UserAccount user) {
        return user.getRole()==Role.COACH?plans.findTop100ByCoachIdOrderByCreatedAtDesc(user.getId())
                :plans.findTop100ByPlayerIdOrderByCreatedAtDesc(user.getId());
    }
    @PostMapping @PreAuthorize("hasRole('COACH')")
    public WellnessPlan create(@AuthenticationPrincipal UserAccount coach,@Valid @RequestBody CreateRequest request) {
        if(!assignments.existsByCoachIdAndPlayerId(coach.getId(),request.playerId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You can only create plans for assigned players.");
        return plans.save(new WellnessPlan(request.playerId(),coach.getId(),request.type(),request.title().trim(),
                request.details().trim(),request.scheduledFor()));
    }
    public record CreateRequest(@NotNull Long playerId,@NotNull PlanType type,@NotBlank @Size(max=120) String title,
                                @NotBlank @Size(max=2000) String details,LocalDate scheduledFor){}
}
