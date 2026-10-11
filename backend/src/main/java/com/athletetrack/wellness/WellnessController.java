package com.athletetrack.wellness;

import com.athletetrack.user.UserAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wellness")
@PreAuthorize("hasRole('PLAYER')")
public class WellnessController {
    private final WellnessCheckInRepository checkIns;
    public WellnessController(WellnessCheckInRepository checkIns){this.checkIns=checkIns;}
    @GetMapping public List<WellnessCheckIn> list(@AuthenticationPrincipal UserAccount user){return checkIns.findTop30ByPlayerIdOrderByRecordedAtDescIdDesc(user.getId());}
    @PostMapping public WellnessCheckIn create(@AuthenticationPrincipal UserAccount user,@Valid @RequestBody Create request){
        return checkIns.save(new WellnessCheckIn(user.getId(),request.recordedAt()==null?LocalDate.now():request.recordedAt(),
                request.sleepHours(),request.recoveryScore(),request.energyLevel(),request.notes()));
    }
    public record Create(LocalDate recordedAt,@NotNull @DecimalMin("0.0") @DecimalMax("24.0") Double sleepHours,
                         @NotNull @Min(0) @Max(100) Integer recoveryScore,@NotNull @Min(1) @Max(5) Integer energyLevel,@Size(max=500) String notes){}
}
