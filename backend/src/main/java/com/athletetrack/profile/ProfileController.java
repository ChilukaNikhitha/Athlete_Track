package com.athletetrack.profile;

import com.athletetrack.user.UserAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final PlayerProfileRepository profiles;
    public ProfileController(PlayerProfileRepository profiles){this.profiles=profiles;}
    @GetMapping public ProfileResponse get(@AuthenticationPrincipal UserAccount user) {
        return profiles.findByUserId(user.getId()).map(ProfileController::view).orElseGet(()->empty(user));
    }
    @PutMapping public ProfileResponse update(@AuthenticationPrincipal UserAccount user,@Valid @RequestBody Update request) {
        PlayerProfile profile=profiles.findByUserId(user.getId()).orElseGet(()->new PlayerProfile(user));
        profile.update(request.age(),request.heightCm(),request.weightKg(),clean(request.sport()),clean(request.goal()));
        return view(profiles.save(profile));
    }
    private static ProfileResponse view(PlayerProfile p) {
        Double bmi=null;
        if(p.getHeightCm()!=null&&p.getWeightKg()!=null){double h=p.getHeightCm()/100.0;bmi=Math.round(p.getWeightKg()/(h*h)*10.0)/10.0;}
        return new ProfileResponse(p.getUser().getId(),p.getUser().getDisplayName(),p.getUser().getEmail(),
                p.getAge(),p.getHeightCm(),p.getWeightKg(),bmi,p.getSport(),p.getGoal());
    }
    private static ProfileResponse empty(UserAccount u){return new ProfileResponse(u.getId(),u.getDisplayName(),u.getEmail(),null,null,null,null,null,null);}
    private static String clean(String s){return s==null||s.isBlank()?null:s.trim();}
    public record Update(@Min(10) @Max(100) Integer age,@DecimalMin("80.0") @DecimalMax("250.0") Double heightCm,
                         @DecimalMin("20.0") @DecimalMax("350.0") Double weightKg,@Size(max=80) String sport,@Size(max=500) String goal){}
    public record ProfileResponse(Long userId,String displayName,String email,Integer age,Double heightCm,Double weightKg,Double bmi,String sport,String goal){}
}
