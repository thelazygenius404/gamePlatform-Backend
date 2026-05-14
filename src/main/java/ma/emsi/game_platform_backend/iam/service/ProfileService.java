package ma.emsi.game_platform_backend.iam.service;

import ma.emsi.game_platform_backend.iam.dto.ChangePasswordRequest;
import ma.emsi.game_platform_backend.iam.dto.ProfileDTO;
import ma.emsi.game_platform_backend.iam.dto.UpdateProfileRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface ProfileService {

    ProfileDTO getProfile(String email);

    ProfileDTO updateProfile(String email, UpdateProfileRequest request);

    Map<String, String> uploadAvatar(String email, MultipartFile file);

    Map<String, String> deleteAvatar(String email);

    Map<String, String> changePassword(String email, ChangePasswordRequest request);
}