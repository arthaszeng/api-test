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

public class TestQueueManila {
    private final static String POINT_BASE_URL_PROD_MANILA = "https://manila.loyalty.blockchain.thoughtworks.cn";
    private final static String CRM_BASE_URL_PROD_MANILA = "https://manila.crm.blockchain.thoughtworks.cn";

    private final String CUSTOMER_ROOT_KEY = "mockRootKeyCUS1";
    private final String CUSTOMER_ADDRESS = "0xad89AE26a8026B14F916FFEa7D9923d4d014Eb0f";
    private final String CUSTOMER_PRIVATE_KEY = "b5d61ea1f19e759413cf8bb0f7d0b2d77cdc66388fd107a1747e6034e48154ac";

    private final String MERCHANT_ADDRESS = "0x0a8C8940e50bCdC2a05d4997610e0A93d9Cb5B30";
    private final String MERCHANT_PRIVATE_KEY = "e46dcea19e6a277b1126d366586e3db00bbee3278d1ba547e6827c9263f98f9d";
    private final String ROC_MANILA_ADDRESS_PROD = "0x5063D554cED7F296315aA49f8d9a02F466696De1";


    @Test
    void user_journey_test_queue() throws IOException {
        //get token
        GetTokenCommand tokenCommand = GetTokenCommand.builder().role(Role.CUSTOMER.name()).rootKey(CUSTOMER_ROOT_KEY).build();
        String getTokenJson = RequestHelper.getJsonString(tokenCommand);
        Response response = given()
                .header("Content-Type", "application/json")
                .body(getTokenJson)
                .when()
                .post(POINT_BASE_URL_PROD_MANILA + "/accounts/token");

        TokenResponse tokenResponse = (TokenResponse) ResponseHelper.getResponseAsObject(response.asString(), TokenResponse.class);
        String token = tokenResponse.getToken();

        //get original balance
        BalanceResponse initBalances = queryBalances(token);
        int initCusBalance = initBalances.getAccounts().get(0).getBalance();
        int initMerBalance = initBalances.getAccounts().get(1).getBalance();

        //points reward
        OrderPaidEvent orderPaidEvent = OrderPaidEvent.builder()
                .customerMembershipId("testCUS2")
                .merchantMembershipId("testMER2")
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
                .post(POINT_BASE_URL_PROD_MANILA + "/test/reward");

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
                .post(POINT_BASE_URL_PROD_MANILA + "/points/spend")
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
        String merchantAddress = "0x7D53836C2310128590D16B67730F3A425AE335B9";
        TransactionCommand redeemCommand = TransactionCommand.builder()
                .fromAddress(merchantAddress)
                .toAddress(ROC_MANILA_ADDRESS_PROD)
                .amount(BigDecimal.TEN)
                .fromPublicKey("publicKey")
                .signedTransactionRawData(SignedRawDataHelper.getRedeemSignedRawTransaction(10, Region.MANILA, MERCHANT_PRIVATE_KEY))
                .build();

        String redeemJson = RequestHelper.getJsonString(redeemCommand);

        given()
                .header("Content-Type", "application/json")
                .auth()
                .none()
                .header("Authorization", token)
                .body(redeemJson)
                .when()
                .post(POINT_BASE_URL_PROD_MANILA + "/points/redemption")
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
                .get(POINT_BASE_URL_PROD_MANILA + "/points");
        return (BalanceResponse) ResponseHelper.getResponseAsObject(initResponse.asString(), BalanceResponse.class);
    }
}
