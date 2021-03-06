import command.GetTokenCommand;
import command.OrderPaidEvent;
import command.TransactionCommand;
import common.Region;
import common.Role;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import response.BalanceResponse;
import response.TokenResponse;
import utils.RequestHelper;
import utils.ResponseHelper;
import utils.SignedRawDataHelper;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestQueueMacau {
    private final static String POINT_BASE_URL_PROD_MACAU = "https://macau.loyalty.blockchain.thoughtworks.cn";

    private final String ROC_MACAU_ADDRESS_PROD = "0x2259189DEDaceFd6cF3FB7F04445e77684564f2f";

    private final String CUSTOMER_ROOT_KEY = "mockRootKeyCUS1";
    private final String CUSTOMER_ADDRESS = "0x88E463f33B905354dAc5360Fbf0f32Ac2861206E";
    private final String CUSTOMER_PRIVATE_KEY = "1f2363918802c46a89a9cbbb5c730ebc5a3a368e11267fd2ca603974ec9cfd19";

    private final String MERCHANT_ADDRESS = "0x30D757348D75E4F5Ae3ADa9Fe4702a0b7ea1944D";
    private final String MERCHANT_PRIVATE_KEY = "9dc09e2426eadaba114d3904a5e7509af8df7c960c8b694152b53a6636cad051";


    @Test
    void user_journey_test_queue() throws IOException {
        //get token
        GetTokenCommand tokenCommand = GetTokenCommand.builder().role(Role.CUSTOMER.name()).rootKey(CUSTOMER_ROOT_KEY).build();
        String getTokenJson = RequestHelper.getJsonString(tokenCommand);
        Response response = given()
                .header("Content-Type", "application/json")
                .body(getTokenJson)
                .when()
                .post(POINT_BASE_URL_PROD_MACAU + "/accounts/token");

        TokenResponse tokenResponse = (TokenResponse) ResponseHelper.getResponseAsObject(response.asString(), TokenResponse.class);
        String token = tokenResponse.getToken();

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
                .post(POINT_BASE_URL_PROD_MACAU + "/test/reward");

        //verify customer balance increase
        BalanceResponse balancesAfterReward = queryBalances(token);
        int rewardCusBalance = balancesAfterReward.getAccounts().get(0).getBalance();
        assertEquals(rewardCusBalance, initCusBalance + 10);

        //points spend
        TransactionCommand spendCommand = TransactionCommand.builder()
                .fromAddress(CUSTOMER_ADDRESS)
                .toAddress(MERCHANT_ADDRESS)
                .amount(BigDecimal.valueOf(10))
                .fromPublicKey("publicKey")
                .signedTransactionRawData(SignedRawDataHelper.getSpendSignedRawTransaction(MERCHANT_ADDRESS, 10, CUSTOMER_PRIVATE_KEY))
                .build();

        String spendJson = RequestHelper.getJsonString(spendCommand);

        given()
                .header("Content-Type", "application/json")
                .auth()
                .none()
                .header("Authorization", token)
                .body(spendJson)
                .when()
                .post(POINT_BASE_URL_PROD_MACAU + "/points/spend")
                .then()
                .assertThat()
                .statusCode(201);

        //verify customer balance reduce and merchant balance increase
        BalanceResponse balancesAfterSpend = queryBalances(token);
        int spendCusBalance = balancesAfterSpend.getAccounts().get(0).getBalance();
        int spendMerBalance = balancesAfterSpend.getAccounts().get(1).getBalance();
        assertEquals(spendCusBalance, rewardCusBalance - 10);
        assertEquals(spendMerBalance, initMerBalance + 10);

        //points redeem
        TransactionCommand redeemCommand = TransactionCommand.builder()
                .fromAddress(MERCHANT_ADDRESS)
                .toAddress(ROC_MACAU_ADDRESS_PROD)
                .amount(BigDecimal.TEN)
                .fromPublicKey("publicKey")
                .signedTransactionRawData(SignedRawDataHelper.getRedeemSignedRawTransaction(10, Region.MACAU, MERCHANT_PRIVATE_KEY))
                .build();

        String redeemJson = RequestHelper.getJsonString(redeemCommand);

        given()
                .header("Content-Type", "application/json")
                .auth()
                .none()
                .header("Authorization", token)
                .body(redeemJson)
                .when()
                .post(POINT_BASE_URL_PROD_MACAU + "/points/redemption")
                .then()
                .assertThat()
                .statusCode(201);

        //TODO: verify merchant balance reduce
        BalanceResponse balanceAfterRedeem = queryBalances(token);
        int redeemMerBalance = balanceAfterRedeem.getAccounts().get(1).getBalance();
        assertEquals(redeemMerBalance, spendMerBalance - 10);
    }

    private BalanceResponse queryBalances(String token) throws IOException {
        Response initResponse = given()
                .auth()
                .none()
                .header("Authorization", token)
                .param("addresses", CUSTOMER_ADDRESS)
                .param("addresses", MERCHANT_ADDRESS)
                .when()
                .get(POINT_BASE_URL_PROD_MACAU + "/points");
        return (BalanceResponse) ResponseHelper.getResponseAsObject(initResponse.asString(), BalanceResponse.class);
    }
}
