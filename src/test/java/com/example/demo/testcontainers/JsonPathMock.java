package com.example.demo.testcontainers;

public interface JsonPathMock {
    public static final String VALID_USER_REQUEST_JSON_PATH = "json/user/request/valid-user.json";
    public static final String VALID_USER_RESPONSE_JSON_PATH = "json/user/response/created-user.json";

    public static final String SECOND_VALID_USER_REQUEST_JSON_PATH = "json/user/request/second-valid-user.json";

    public static final String BLANK_FIRST_NAME_REQUEST_JSON_PATH = "json/user/request/blank-first-name.json";
    public static final String BLANK_LAST_NAME_REQUEST_JSON_PATH = "json/user/request/blank-last-name.json";
    public static final String BLANK_EMAIL_REQUEST_JSON_PATH = "json/user/request/blank-email.json";
    public static final String INVALID_EMAIL_FORMAT_REQUEST_JSON_PATH = "json/user/request/invalid-email-format.json";
    public static final String NULL_BIRTH_DATE_REQUEST_JSON_PATH = "json/user/request/null-birth-date.json";
    public static final String FUTURE_BIRTH_DATE_REQUEST_JSON_PATH = "json/user/request/future-birth-date.json";

    public static final String MALFORMED_REQUEST_JSON_PATH = "json/user/request/malformed.json";
    public static final String TOO_LONG_LAST_NAME_REQUEST_JSON_PATH = "json/user/request/too-long-last-name.json";
}
