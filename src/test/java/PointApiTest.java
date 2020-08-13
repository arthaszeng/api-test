import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import command.GetTokenCommand;
import common.Role;
import response.TokenResponse;
import utils.RequestHelper;
import utils.ResponseHelper;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PointApiTest extends BaseApiTest {
    private final static String POINT_BASE_URL_MACAU = "https://dev.macau.loyalty.blockchain.thoughtworks.cn";
    private final static String POINT_BASE_URL_MANILA = "https://dev.manila.loyalty.blockchain.thoughtworks.cn";

    private final static String TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE1OTcxMzYxMjMsIm1lbWJlcnNoaXBJZCI6InNteTIwMDA1Iiwicm9sZSI6Ik1FUkNIQU5UIn0.TrafIzHrLdJX-DKj4YNsGQh0NelCmOnFHf3FfV5zdbAINm09fe9OM4zutINEWSUWLOBc99nXcqPyvHQMRK-VOZmhHBIBvb5p7X_ld7mDWRvwndIgnuM-UgybitNh5NRCt4Y4XfqPze8HkQhh-z8dkYMJqlJcEv6tb1lNiNJHcevuo63o6Fhi99pLUA7J3nKi29dONy-t_mAmnkjvTg-VLrrrzmNQXfT0yN7aQxrdKk2kdbwCKjVUGD5I_SG6--K6bDPcRFj-vFM8vG0LPz6HlHcr_j33zSn1L1Ih9zArEeOEPwAEGjS_i9gg5YeEAHh96jtbc9uVeOUSY-t4q4JbmQ";

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
}
