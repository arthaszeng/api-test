
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Ignore
public class CrmApiTest {
    private final static String CRM_BASE_URL_MACAU = "https://dev.macau.crm.blockchain.thoughtworks.cn";

    @Test
    void should_get_show_list_success() {
        given()
                .when()
                .get(CRM_BASE_URL_MACAU + "/shows") // This will get the data from the given URL
                .then()
                .statusCode(200) // It verify the actual response code with the given code
                .body("[0].id", equalTo("3")) // Checking whether value is Not_Null or not
                .body("[0].membershipId", equalTo("20000002"))
                .body("[0].unit", equalTo("POINTS")); // Checking status is equal to "sold"
    }
}
