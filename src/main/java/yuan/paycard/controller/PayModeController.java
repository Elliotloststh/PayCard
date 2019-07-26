package yuan.paycard.controller;

import io.swagger.annotations.Api;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import yuan.paycard.annotation.UserLoginToken;
import yuan.paycard.model.PayMode;
import yuan.paycard.service.PayModeService;
import yuan.paycard.utils.ApiResponse;
import yuan.paycard.utils.CommonUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/paymode")
@Api(value = "支付方式Controller")
public class PayModeController {

    @Autowired
    PayModeService payModeService;

    @UserLoginToken
    @GetMapping("/getAll")
    public ApiResponse getAll(HttpServletRequest request) {
        long userId = CommonUtils.getUserId(request);
        List<PayMode> payModeList = payModeService.selectByUserId(userId);
        ApiResponse response = ApiResponse.ok();
        response.putDataValue("list", payModeList);
        return response;
    }

    @UserLoginToken
    @PostMapping("addPayMode")
    public ApiResponse addPayMode(@RequestBody addPayModeInput input, HttpServletRequest request) {
        long userId = CommonUtils.getUserId(request);
        //校验
        PayMode query = new PayMode();
        query.setPaymentType(input.getPaymentType());
        query.setBizId(input.getBizId());
        query.setUserId(userId);
        if(payModeService.findOne(query) != null) {
            ApiResponse response = ApiResponse.customerError();
            response.putDataValue("msg","您已与之绑定");
            return response;
        }

        PayMode payMode = new PayMode();
        payMode.setUserId(userId);
        payMode.setCreateTime(System.currentTimeMillis()/1000);
        payMode.setName(input.getName());
        payMode.setPaymentType(input.getPaymentType());
        payMode.setBizId(input.getBizId());
        payModeService.insert(payMode);
        return ApiResponse.ok();
    }

    @UserLoginToken
    @PostMapping("deletePayMode")
    public ApiResponse deletePayMode(@RequestBody deletePayModeInput input, HttpServletRequest request) {
        long userId = CommonUtils.getUserId(request);
        long payModeId = input.getPayModeId();
        PayMode payMode = payModeService.selectById(payModeId);
        if(payMode.getUserId() != userId) {
            return ApiResponse.unauthorized();
        }
        if(payModeService.deleteById(payModeId)==1) {
            return ApiResponse.ok();
        } else {
            return ApiResponse.serverInternalError();
        }

    }

    @Data
    static class addPayModeInput {
        Integer paymentType;
        String name;
        String bizId;
    }

    @Data
    static class deletePayModeInput {
        Long payModeId;
    }
}
