package command;

import common.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginCommand {

    private String membershipId;

    private String password;

    private Role role;
}
