import command.BindAccountCommand;
import command.CreateUserCommand;
import command.GetTokenCommand;
import command.OrderPaidEvent;
import command.TransactionCommand;
import common.Region;
import common.Role;
import io.restassured.response.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Ignore
public class PointApiTest {
    private final static String POINT_BASE_URL_MACAU = "https://dev.macau.loyalty.blockchain.thoughtworks.cn";
    private final static String CRM_BASE_URL_MACAU = "https://dev.macau.crm.blockchain.thoughtworks.cn";
    private final static String POINT_BASE_URL_MANILA = "https://dev.manila.loyalty.blockchain.thoughtworks.cn";

    private final static String ROC_PRIVATE_KEY = "0b50f9b00fb6bde53c87aee17219646359b4a086e70b7e3e82ab39c44ec5cb10";

    private final static String TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE1OTcxMzYxMjMsIm1lbWJlcnNoaXBJZCI6InNteTIwMDA1Iiwicm9sZSI6Ik1FUkNIQU5UIn0.TrafIzHrLdJX-DKj4YNsGQh0NelCmOnFHf3FfV5zdbAINm09fe9OM4zutINEWSUWLOBc99nXcqPyvHQMRK-VOZmhHBIBvb5p7X_ld7mDWRvwndIgnuM-UgybitNh5NRCt4Y4XfqPze8HkQhh-z8dkYMJqlJcEv6tb1lNiNJHcevuo63o6Fhi99pLUA7J3nKi29dONy-t_mAmnkjvTg-VLrrrzmNQXfT0yN7aQxrdKk2kdbwCKjVUGD5I_SG6--K6bDPcRFj-vFM8vG0LPz6HlHcr_j33zSn1L1Ih9zArEeOEPwAEGjS_i9gg5YeEAHh96jtbc9uVeOUSY-t4q4JbmQ";
    private final static String ROC_MACAU_ADDRESS_DEV = "0x395E9294991086eDC9fce644197Ac244b30768F9";


    @Test
    public void test_should_get_account_token_success() throws IOException {
        String rootKey = "d365a878cd6ca408acd99207255c5d964b8b90e5f7d7d49e5ff0e6b8e3725ee8";
        GetTokenCommand command = GetTokenCommand.builder().role(Role.CUSTOMER.name()).rootKey(rootKey).build();
        String jsonCommand = RequestHelper.getJsonString(command);
        Response response = given()
                .header("Content-Type", "application/json")
                .body(jsonCommand)
                .when()
                .post(POINT_BASE_URL_MACAU + "/accounts/token");

        TokenResponse tokenResponse = (TokenResponse) ResponseHelper.getResponseAsObject(response.asString(), TokenResponse.class);

        assertEquals(200, response.getStatusCode());
        assertNotNull(tokenResponse.getToken());
    }

    //TODO: get token from bind account or get token
    @Test
    void should_get_account_roc_success() {
        given()
                .auth()
                .none()
                .header("Authorization", TOKEN)
                .when()
                .get(POINT_BASE_URL_MACAU + "/accounts/roc")
                .then()
                .statusCode(200)
                .body("accountName", equalTo("ROC_MACAU"))
                .body("address", equalTo(ROC_MACAU_ADDRESS_DEV));
    }

    @Test
    void should_get_account_merchant_success() {
        given()
                .auth()
                .none()
                .header("Authorization", TOKEN)
                .param("membershipId", "20000002")
                .when()
                .get(POINT_BASE_URL_MACAU + "/accounts/merchant")
                .then()
                .statusCode(200)
                .body("accountName", equalTo("ffMacauPro"))
                .body("accountId", equalTo("094bfbc0-d168-11ea-976b-99ba67a55bfc"))
                .body("address", equalTo("0xfd90fEaaA706F95e8588b08ad6Ac72a1dA5cB748"));
    }

    //TODO: test case result are not stable
    //TODO: async await?
    @Test
    void should_query_points_by_addresses_success() {
        given()
                .auth()
                .none()
                .header("Authorization", TOKEN)
                .param("addresses", "0xfd90fEaaA706F95e8588b08ad6Ac72a1dA5cB748")
                .param("addresses", "0xFb1B0AE44841B2Ae19199e03eC1B3874291b095c")
                .when()
                .get(POINT_BASE_URL_MACAU + "/points")
                .then()
                .statusCode(200)
                .body("totalBalance", equalTo(300))
                .body("accounts", notNullValue())
                .body("accounts[0].balance", equalTo(300))
                .body("accounts[0].address", equalTo("0xfd90fEaaA706F95e8588b08ad6Ac72a1dA5cB748"))
                .body("accounts[1].balance", equalTo(0))
                .body("accounts[1].address", equalTo("0xFb1B0AE44841B2Ae19199e03eC1B3874291b095c"));
    }

    @Test
    void should_query_points_by_issuer_success() {
        given()
                .auth()
                .none()
                .header("Authorization", TOKEN)
                .param("address", ROC_MACAU_ADDRESS_DEV)
                .when()
                .get(POINT_BASE_URL_MACAU + "/points/issuers")
                .then()
                .statusCode(200)
                .body("totalBalance", notNullValue());
    }

    //test in another branch
    @Test
    void should_bind_customer_account_success() throws IOException {

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
                .post(CRM_BASE_URL_MACAU + "/users");

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
                .post(POINT_BASE_URL_MACAU + "/accounts/binding");

        TokenResponse tokenResponse = (TokenResponse) ResponseHelper.getResponseAsObject(accountResponse.asString(), TokenResponse.class);

        assertEquals(200, accountResponse.getStatusCode());
        assertNotNull(tokenResponse.getToken());
    }

    /*Test data
     customer-smy10023 0x54a77c99D3A32e54d84D8d05E3c225EAc091da9E d365a878cd6ca408acd99207255c5d964b8b90e5f7d7d49e5ff0e6b8e3725ee8
     merchant-smy20019 0xA8bA9a639c53839e51f597ca69f34640636d3dC0 27edd586020c3a3e23fe9e1febcc73cd86f6e85b6653ec6f7732a8df0e9fda09
     */
    @Test
    void should_get_expiration_points_success() {
        String address = "0x54a77c99D3A32e54d84D8d05E3c225EAc091da9E";
        given()
                .auth()
                .none()
                .header("Authorization", TOKEN)
                .param("start", "201808")
                .param("end", "201808")
                .when()
                .get(POINT_BASE_URL_MACAU + String.format("/points/%s/expiration", address))
                .then()
                .statusCode(200)
                .body("balance", equalTo(100));
    }

    @Test
    void should_spend_points_success() throws IOException {
        String customerAddress = "0x61e9b39dF53744277C430fa9fEA3c77FD2b55e5c";
        String merchantAddress = "0x7D53836C2310128590D16B67730F3A425AE335B9";

        OrderPaidEvent orderPaidEvent = OrderPaidEvent.builder()
                .customerMembershipId("testCUS1")
                .merchantMembershipId("testMER1")
                .orderId("1")
                .createdAt(Instant.now())
                .payTime(Instant.now().minusSeconds(5))
                .point(BigDecimal.TEN)
                .build();

        String orderPaidJson = RequestHelper.getJsonString(orderPaidEvent);

        given()
                .header("Content-Type", "application/json")
                .auth()
                .none()
                .header("Authorization", TOKEN)
                .body(orderPaidJson)
                .when()
                .post(POINT_BASE_URL_MACAU + "/test/reward")
                .then()
                .statusCode(200);

        TransactionCommand transactionCommand = TransactionCommand.builder()
                .fromAddress(customerAddress)
                .toAddress(merchantAddress)
                .amount(BigDecimal.valueOf(10))
                .fromPublicKey("publicKey")
                .signedTransactionRawData(SignedRawDataHelper.getSpendSignedRawTransaction(merchantAddress, 10))
                .build();

        String transactionJson = RequestHelper.getJsonString(transactionCommand);

        given()
                .header("Content-Type", "application/json")
                .auth()
                .none()
                .header("Authorization", TOKEN)
                .body(transactionJson)
                .when()
                .post(POINT_BASE_URL_MACAU + "/points/spend")
                .then()
                .assertThat()
                .statusCode(201);
    }

    @Test
    void should_redeem_points_for_merchant_success() throws IOException {
        String merchantAddress = "0x7D53836C2310128590D16B67730F3A425AE335B9";
        TransactionCommand transactionCommand = TransactionCommand.builder()
                .fromAddress(merchantAddress)
                .toAddress(ROC_MACAU_ADDRESS_DEV)
                .amount(BigDecimal.ONE)
                .fromPublicKey("publicKey")
                .signedTransactionRawData(SignedRawDataHelper.getRedeemSignedRawTransaction(1, Region.MACAU))
                .build();

        String transactionJson = RequestHelper.getJsonString(transactionCommand);

        given()
                .header("Content-Type", "application/json")
                .auth()
                .none()
                .header("Authorization", TOKEN)
                .body(transactionJson)
                .when()
                .post(POINT_BASE_URL_MACAU + "/points/redemption")
                .then()
                .assertThat()
                .statusCode(201);
    }
}
