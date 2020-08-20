package response;

import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
public class BalanceResponse {
    int totalBalance;
    List<Account> accounts;

    @Getter
    public static class Account {
        String address;
        int balance;
    }
}
