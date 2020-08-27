import command.BindAccountCommand;
import command.CreateUserCommand;
import command.GetTokenCommand;
import command.OrderPaidEvent;
import command.TransactionCommand;
import common.Role;
import io.restassured.response.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import response.BalanceResponse;
import response.TokenResponse;
import response.UserResponse;
import utils.AccountHelper;
import utils.RequestHelper;
import utils.ResponseHelper;
import utils.SignedRawDataHelper;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestQueueManila {
    private final static String POINT_BASE_URL_PROD_MANILA = "https://manila.loyalty.blockchain.thoughtworks.cn";
    private final static String CRM_BASE_URL_PROD_MANILA = "https://manila.crm.blockchain.thoughtworks.cn";

    private final String CUSTOMER_ROOT_KEY = "mockRootKeyCUS";
    private final String MERCHANT_ROOT_KEY = "mockRootKeyMER";
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
                .post(POINT_BASE_URL_PROD_MANILA + "/accounts/token");

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

    private void should_bind_customer_account_success() throws IOException {

        String membershipId = RandomStringUtils.randomAlphanumeric(8);
        CreateUserCommand command = CreateUserCommand.builder()
                .membershipId(membershipId)
                .name("test" + membershipId)
                .password("0000")
                .phoneNumber("1234567890")
                .role(Role.CUSTOMER)
                .build();

        String createUserCommand = RequestHelper.getJsonString(command);

        Response response = given()
                .header("Content-Type", "application/json")
                .body(createUserCommand)
                .when()
                .post(CRM_BASE_URL_PROD_MANILA + "/users");

        assertEquals(201, response.getStatusCode());
        UserResponse user = (UserResponse) ResponseHelper.getResponseAsObject(response.asString(), UserResponse.class);

        await().atLeast(5, TimeUnit.SECONDS);

        BindAccountCommand bindAccountCommand = BindAccountCommand.builder()
                .accountId(UUID.randomUUID().toString())
                .address(AccountHelper.generateRandomAddress())
                .keyIndex(1L)
                .rootKey(AccountHelper.generateRandomRootKey())
                .membershipId(user.getMembershipId())
                .password("0000")
                .role(user.getRole())
                .build();

        String accountCommandJson = RequestHelper.getJsonString(bindAccountCommand);

        Response accountResponse = given()
                .header("Content-Type", "application/json")
                .body(accountCommandJson)
                .when()
                .post(POINT_BASE_URL_PROD_MANILA + "/accounts/binding");

        TokenResponse tokenResponse = (TokenResponse) ResponseHelper.getResponseAsObject(accountResponse.asString(), TokenResponse.class);

        assertEquals(200, accountResponse.getStatusCode());
        assertNotNull(tokenResponse.getToken());
    }

}
