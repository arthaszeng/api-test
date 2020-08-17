package command;

import common.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BindAccountCommand {

    private String accountId;

    private String address;

    private Long keyIndex;

    private String rootKey;

    private String membershipId;

    private String password;

    private Role role;
}

