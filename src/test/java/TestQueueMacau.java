import command.GetTokenCommand;
import command.OrderPaidEvent;
import command.TransactionCommand;
import common.Role;
import io.restassured.response.Response;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import response.BalanceResponse;
import response.TokenResponse;
import utils.RequestHelper;
import utils.ResponseHelper;
import utils.SignedRawDataHelper;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@AllArgsConstructor
public class TestQueueMacau {
    private final static String POINT_BASE_URL_MACAU = "https://dev.macau.loyalty.blockchain.thoughtworks.cn";

    private final String CUSTOMER_ROOT_KEY = "mockRootKeyCUS";
    private final String MERCHANT_ROOT_KEY = "mockRootKeyCUS";
    private final String CUSTOMER_ADDRESS = "0x61e9b39dF53744277C430fa9fEA3c77FD2b55e5c";
    private final String MERCHANT_ADDRESS = "0x7D53836C2310128590D16B67730F3A425AE335B9";
    private final String ROC_MACAU_ADDRESS_DEV = "0x395E9294991086eDC9fce644197Ac244b30768F9";


    @Test
    void user_journey_test_queue() throws IOException {
        //get token
        GetTokenCommand tokenCommand = GetTokenCommand.builder().role(Role.CUSTOMER.name()).rootKey(CUSTOMER_ROOT_KEY).build();
        String getTokenJson = RequestHelper.getJsonString(tokenCommand);
        Response response = given()
                .header("Content-Type", "application/json")
                .body(getTokenJson)
                .when()
                .post(POINT_BASE_URL_MACAU + "/accounts/token");

        TokenResponse tokenResponse = (TokenResponse) ResponseHelper.getResponseAsObject(response.asString(), TokenResponse.class);
        String token = tokenResponse.getToken();

        //TODO: get original customer balance response
        //get original balance
        BalanceResponse initBalances = queryBalances(token);
        int initCusBalance = initBalances.getAccounts().get(0).getBalance();
        int initMerBalance = initBalances.getAccounts().get(1).getBalance();

        //points reward
        OrderPaidEvent orderPaidEvent = OrderPaidEvent.builder()
                .customerMembershipId("testCUS1")
                .merchantMembershipId("testMER1")
                .orderId("1")
                .createdAt(Instant.now())
                .payTime(Instant.now())
                .point(BigDecimal.TEN)
                .build();

        String orderPaidJson = RequestHelper.getJsonString(orderPaidEvent);

        given()
                .header("Content-Type", "application/json")
                .auth()
                .none()
                .header("Authorization", token)
                .body(orderPaidJson)
                .when()
                .post(POINT_BASE_URL_MACAU + "/test/reward");

        //TODO: verify customer balance increase
        await().atLeast(5, TimeUnit.SECONDS);

        BalanceResponse balancesAfterReward = queryBalances(token);
        int rewardCusBalance = balancesAfterReward.getAccounts().get(0).getBalance();
        assertEquals(rewardCusBalance, initCusBalance + 10);

        //points spend
        TransactionCommand spendCommand = TransactionCommand.builder()
                .fromAddress(CUSTOMER_ADDRESS)
                .toAddress(MERCHANT_ADDRESS)
                .amount(BigDecimal.valueOf(10))
                .fromPublicKey("publicKey")
                .signedTransactionRawData(SignedRawDataHelper.getSpendSignedRawTransaction(MERCHANT_ADDRESS, 10))
                .build();

        String spendJson = RequestHelper.getJsonString(spendCommand);

        given()
                .header("Content-Type", "application/json")
                .auth()
                .none()
                .header("Authorization", token)
                .body(spendJson)
                .when()
                .post(POINT_BASE_URL_MACAU + "/points/spend")
                .then()
                .assertThat()
                .statusCode(201);

        //TODO: verify customer balance reduce and merchant balance increase

        //points redeem
        String merchantAddress = "0x7D53836C2310128590D16B67730F3A425AE335B9";
        TransactionCommand redeemCommand = TransactionCommand.builder()
                .fromAddress(merchantAddress)
                .toAddress(ROC_MACAU_ADDRESS_DEV)
                .amount(BigDecimal.TEN)
                .fromPublicKey("publicKey")
                .signedTransactionRawData(SignedRawDataHelper.getRedeemSignedRawTransaction(10))
                .build();

        String redeemJson = RequestHelper.getJsonString(redeemCommand);

        given()
                .header("Content-Type", "application/json")
                .auth()
                .none()
                .header("Authorization", token)
                .body(redeemJson)
                .when()
                .post(POINT_BASE_URL_MACAU + "/points/redemption")
                .then()
                .assertThat()
                .statusCode(201);

        //TODO: verify merchant balance reduce and roc balance increase
    }

    private BalanceResponse queryBalances(String token) throws IOException {
        Response initResponse = given()
                .auth()
                .none()
                .header("Authorization", token)
                .param("addresses", CUSTOMER_ADDRESS)
                .param("addresses", MERCHANT_ADDRESS)
                .when()
                .get(POINT_BASE_URL_MACAU + "/points");
        return (BalanceResponse) ResponseHelper.getResponseAsObject(initResponse.asString(), BalanceResponse.class);
    }
}
