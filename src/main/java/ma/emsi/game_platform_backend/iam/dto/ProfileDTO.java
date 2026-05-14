package ma.emsi.game_platform_backend.iam.dto;

import lombok.*;
import ma.emsi.game_platform_backend.shared.enums.Role;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProfileDTO {
    private String id;
    private String pseudo;
    private String email;
    private String avatarUrl;
    private Role   role;
    private int    level;
    private int    points;
}