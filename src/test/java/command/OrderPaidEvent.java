package command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaidEvent {
    private String orderId;
    private BigDecimal point;
    private String customerMembershipId;
    private String merchantMembershipId;
    private Instant payTime;
    private Instant createdAt;
}
