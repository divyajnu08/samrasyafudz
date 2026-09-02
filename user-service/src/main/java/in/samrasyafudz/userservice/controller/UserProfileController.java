package in.samrasyafudz.userservice.controller;

import in.samrasyafudz.userservice.dto.UpdateProfileRequest;
import in.samrasyafudz.userservice.dto.UserProfileResponse;
import in.samrasyafudz.commonsecurity.AuthenticatedUser;
import in.samrasyafudz.userservice.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public UserProfileResponse getProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        return userProfileService.getProfile(user.userId());
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(@AuthenticationPrincipal AuthenticatedUser user,
                                                             @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateProfile(user.userId(), request));
    }
}