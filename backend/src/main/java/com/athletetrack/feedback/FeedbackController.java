package com.athletetrack.feedback;

import com.athletetrack.coach.CoachAssignmentRepository;
import com.athletetrack.user.Role;
import com.athletetrack.user.UserAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    private final AthleteFeedbackRepository feedback;
    private final CoachAssignmentRepository assignments;
    public FeedbackController(AthleteFeedbackRepository feedback,CoachAssignmentRepository assignments){this.feedback=feedback;this.assignments=assignments;}
    @GetMapping @PreAuthorize("hasAnyRole('PLAYER','COACH')")
    public List<AthleteFeedback> list(@AuthenticationPrincipal UserAccount user) {
        return user.getRole()==Role.COACH?feedback.findTop100ByCoachIdOrderByCreatedAtDesc(user.getId())
                :feedback.findTop100ByPlayerIdOrderByCreatedAtDesc(user.getId());
    }
    @PostMapping @PreAuthorize("hasRole('COACH')")
    public AthleteFeedback create(@AuthenticationPrincipal UserAccount coach,@Valid @RequestBody CreateRequest request) {
        if(!assignments.existsByCoachIdAndPlayerId(coach.getId(),request.playerId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You can only send feedback to assigned players.");
        return feedback.save(new AthleteFeedback(request.playerId(),coach.getId(),request.message().trim()));
    }
    public record CreateRequest(@NotNull Long playerId,@NotBlank @Size(max=2000) String message){}
}
