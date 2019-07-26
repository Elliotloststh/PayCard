package yuan.paycard.interceptor;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import yuan.paycard.exception.UnauthException;
import yuan.paycard.utils.ApiResponse;

@ControllerAdvice
public class GloablExceptionHandler {
    @ResponseBody
    @ExceptionHandler(UnauthException.class)
    public ApiResponse handleException(UnauthException e) {
        String msg = e.getMessage();
        if (msg == null || msg.equals("")) {
            msg = "服务器出错";
        }
        ApiResponse response = ApiResponse.unauthorized();
        response.putDataValue("msg", e.getMessage());
        return response;
    }
}
