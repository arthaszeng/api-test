package utils;

import io.restassured.response.Response;

import static io.restassured.RestAssured.given;


public class ResourceHelper {

    public static Response get(String url) {
        return given().when().get(url);
    }

    public static Response createWithoutToken(String url, String json) {
        return given()
                .header("Content-Type", "application/json")
                .when()
                .body(json)
                .post(url);
    }

    public static Response createWithToken(String url, String json, String token) {
        return given()
                .header("Content-Type", "application/json")
                .header("Authorization", token)
                .when()
                .body(json)
                .post(url);
    }

}
