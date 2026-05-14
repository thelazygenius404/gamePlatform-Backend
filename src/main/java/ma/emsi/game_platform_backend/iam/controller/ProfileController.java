package ma.emsi.game_platform_backend.iam.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.emsi.game_platform_backend.iam.dto.ChangePasswordRequest;
import ma.emsi.game_platform_backend.iam.dto.ProfileDTO;
import ma.emsi.game_platform_backend.iam.dto.UpdateProfileRequest;
import ma.emsi.game_platform_backend.iam.service.ProfileService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileDTO> getProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        return ResponseEntity.ok(
                profileService.getProfile(userDetails.getUsername())
        );
    }

    @PutMapping
    public ResponseEntity<ProfileDTO> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        return ResponseEntity.ok(
                profileService.updateProfile(
                        userDetails.getUsername(),
                        request
                )
        );
    }

    @PostMapping(
            value = "/upload-avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        return ResponseEntity.ok(
                profileService.uploadAvatar(
                        userDetails.getUsername(),
                        file
                )
        );
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<Map<String, String>> deleteAvatar(
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        return ResponseEntity.ok(
                profileService.deleteAvatar(
                        userDetails.getUsername()
                )
        );
    }

    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        return ResponseEntity.ok(
                profileService.changePassword(
                        userDetails.getUsername(),
                        request
                )
        );
    }
}