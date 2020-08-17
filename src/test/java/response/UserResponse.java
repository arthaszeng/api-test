package response;

import common.Role;
import lombok.Getter;

@Getter
public class UserResponse {
    private String membershipId;
    private String password;
    private Role role;
}
