package scenario

import spock.lang.Specification

import static io.restassured.RestAssured.given

class DemoTest extends Specification{

    //todo: add token
    def "should get merchant account information"() {
        given: "no given"
        when: "get merchant account"
        given().baseUri("https://dev.macau.loyalty.blockchain.thoughtworks.cn")
                .when().log().all()
                .param("membershipId", membershipId)
                .header("Authorization", token)
                .get("accounts/merchant")
                .then().log().all()
                .assertThat().statusCode(200)
        then:"no then"

        where:
        membershipId | token
        "smy20005" | "eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE1OTcxMzYxMjMsIm1lbWJlcnNoaXBJZCI6InNteTIwMDA1Iiwicm9sZSI6Ik1FUkNIQU5UIn0.TrafIzHrLdJX-DKj4YNsGQh0NelCmOnFHf3FfV5zdbAINm09fe9OM4zutINEWSUWLOBc99nXcqPyvHQMRK-VOZmhHBIBvb5p7X_ld7mDWRvwndIgnuM-UgybitNh5NRCt4Y4XfqPze8HkQhh-z8dkYMJqlJcEv6tb1lNiNJHcevuo63o6Fhi99pLUA7J3nKi29dONy-t_mAmnkjvTg-VLrrrzmNQXfT0yN7aQxrdKk2kdbwCKjVUGD5I_SG6--K6bDPcRFj-vFM8vG0LPz6HlHcr_j33zSn1L1Ih9zArEeOEPwAEGjS_i9gg5YeEAHh96jtbc9uVeOUSY-t4q4JbmQ"
    }


    def "should get show list"() {
        given: "no given"
        when: "get show list"
        given().baseUri("https://dev.macau.crm.blockchain.thoughtworks.cn")
                .when().log().all()
                .get("shows")
                .then().log().all()
                .assertThat().statusCode(200)
        then:"no then"

        where:
        placeHolder1 | placeHolder2
        "" | ""
    }
}
