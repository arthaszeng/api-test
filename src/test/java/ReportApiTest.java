import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Ignore
public class ReportApiTest {
    private final static String REPORT_BASE_URL_MACAU = "https://intg.macau.report.blockchain.thoughtworks.cn";
    private final static String TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE1OTcxMzYxMjMsIm1lbWJlcnNoaXBJZCI6InNteTIwMDA1Iiwicm9sZSI6Ik1FUkNIQU5UIn0.TrafIzHrLdJX-DKj4YNsGQh0NelCmOnFHf3FfV5zdbAINm09fe9OM4zutINEWSUWLOBc99nXcqPyvHQMRK-VOZmhHBIBvb5p7X_ld7mDWRvwndIgnuM-UgybitNh5NRCt4Y4XfqPze8HkQhh-z8dkYMJqlJcEv6tb1lNiNJHcevuo63o6Fhi99pLUA7J3nKi29dONy-t_mAmnkjvTg-VLrrrzmNQXfT0yN7aQxrdKk2kdbwCKjVUGD5I_SG6--K6bDPcRFj-vFM8vG0LPz6HlHcr_j33zSn1L1Ih9zArEeOEPwAEGjS_i9gg5YeEAHh96jtbc9uVeOUSY-t4q4JbmQ";
    public static final String ROC_MACAU_ADDRESS_DEV = "0x395E9294991086eDC9fce644197Ac244b30768F9";

    @Test
    void should_query_cash_income_success() {

        given()
                .auth()
                .none()
                .header("Authorization", TOKEN)
                .when()
                .get(REPORT_BASE_URL_MACAU + "/reports/cash/income")
                .then()
                .statusCode(200)
                .body("income", notNullValue());
    }

    //TODO: test method - can't assert accurate result value - test before and after?
    @Test
    void should_query_allocated_points_success() {

        given()
                .auth()
                .none()
                .header("Authorization", TOKEN)
                .when()
                .get(REPORT_BASE_URL_MACAU + "/reports/points/allocated/statistics")
                .then()
                .statusCode(200)
                .body("allocatedAmount", notNullValue());
    }

    @Test
    void should_get_roc_transaction_history_success() {
        given()
                .auth()
                .none()
                .header("Authorization", TOKEN)
                .param("address", ROC_MACAU_ADDRESS_DEV)
                .param("page", 1)
                .param("size", 10)
                .param("transactionTypes", "ALL")
                .when()
                .get(REPORT_BASE_URL_MACAU + "/reports/roc/transactions")
                .then()
                .statusCode(200)
                .body("total", notNullValue())
                .body("size", equalTo(10))
                .body("page", equalTo(1));
    }

    @Test
    void should_get_transactions_success() {
        given()
                .auth()
                .none()
                .header("Authorization", TOKEN)
                .param("address", "0x54a77c99D3A32e54d84D8d05E3c225EAc091da9E")
                .param("page", 1)
                .param("size", 10)
                .when()
                .get(REPORT_BASE_URL_MACAU + "/reports/transactions")
                .then()
                .statusCode(200)
                .body("total", notNullValue())
                .body("size", equalTo(10))
                .body("page", equalTo(1));
    }
}
