package yuan.paycard.utils;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ApiResponse {
    private String message;
    private int code;
    private Map<String, Object> data = new HashMap<String, Object>();

    private ApiResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public void putDataValue(String key, Object value) {
        data.put(key, value);
    }

    public static ApiResponse ok() {
        return new ApiResponse(200, "Ok");
    }

    public static ApiResponse notFound() {
        return new ApiResponse(404, "Not Found");
    }

    public static ApiResponse badRequest() {
        return new ApiResponse(400, "Bad Request");
    }

    public static ApiResponse forbidden() {
        return new ApiResponse(403, "Forbidden");
    }

    public static ApiResponse unauthorized() {
        return new ApiResponse(401, "unauthorized");
    }

    public static ApiResponse serverInternalError() {
        return new ApiResponse(500, "Server Internal Error");
    }

    public static ApiResponse customerError() {
        return new ApiResponse(250, "customer Error");
    }
}
