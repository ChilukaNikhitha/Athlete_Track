package com.athletetrack.performance;

import com.athletetrack.user.UserAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/performance")
@PreAuthorize("hasRole('PLAYER')")
public class PerformanceController {
    private final PerformanceEntryRepository entries;
    public PerformanceController(PerformanceEntryRepository entries){this.entries=entries;}
    @GetMapping public List<PerformanceEntry> list(@AuthenticationPrincipal UserAccount user){return entries.findTop50ByPlayerIdOrderByRecordedAtDescIdDesc(user.getId());}
    @PostMapping public PerformanceEntry create(@AuthenticationPrincipal UserAccount user,@Valid @RequestBody Create request){
        return entries.save(new PerformanceEntry(user.getId(),request.metric().trim(),request.value(),request.unit().trim(),
                request.recordedAt()==null?LocalDate.now():request.recordedAt(),request.notes()));
    }
    public record Create(@NotBlank @Size(max=80) String metric,@NotNull @DecimalMin(value="0.0",inclusive=false) Double value,
                         @NotBlank @Size(max=30) String unit,LocalDate recordedAt,@Size(max=500) String notes){}
}
