package command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCommand {
    private String fromAddress;
    private String toAddress;
    private BigDecimal amount;
    private String fromPublicKey;
    private String signedTransactionRawData;
}
