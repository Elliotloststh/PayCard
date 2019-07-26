package yuan.paycard.controller;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.Api;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import yuan.paycard.annotation.UserLoginToken;
import yuan.paycard.model.*;
import yuan.paycard.service.*;
import yuan.paycard.utils.ApiResponse;
import yuan.paycard.utils.CommonUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/order")
@Api(value="订单Controller")
public class OrderController {
    @Autowired
    AccountService accountService;

    @Autowired
    OrderService orderService;

    @Autowired
    PayModeService payModeService;

    @Autowired
    ItemService itemService;

    @Autowired
    FamilyRelationService familyRelationService;

    @Autowired
    PushService pushService;

    private final HashMap<Integer, String> statusMap = new HashMap<Integer, String>() {{
        put(0, "未支付");
        put(1, "支付成功");
        put(2, "余额不足");
        put(3,"超过单笔额度");
        put(4, "超过本月额度");
        put(5,"支付方式不支持");
        put(6,"待确认");
    }};

    private final HashMap<Long, String> paymentTypeMap = new HashMap<Long, String>() {{
        put(0L, "支付宝");
        put(1L, "微信");
        put(2L, "银行卡");
    }};

    @UserLoginToken
    @PostMapping("/createOrder")
    public ApiResponse createOrder(@RequestBody CreateOrderInput input, HttpServletRequest request) {
        long userId = CommonUtils.getUserId(request);
        Account payer = accountService.selectById(userId);
        Order order = new Order();

        order.setItemId(convertListToString(input.getItemIds(), ','));
        order.setPayerId(userId);
        order.setSellerId(input.getSellerId());
        order.setPayment(input.getPayment());
        order.setPaymentType(input.getPaymentType());
        order.setStatus(0);
        order.setCreateTime(System.currentTimeMillis()/1000);
        order.setUpdateTime(System.currentTimeMillis()/1000);
        if(payer.getType() == 0) { //正常订单
            order.setActualPayerId(userId);
            order.setSource(0);

        } else {    //代付订单
            FamilyRelation familyRelation = familyRelationService.findRelation(userId);
            if(familyRelation == null) {
                ApiResponse response = ApiResponse.customerError();
                response.setMessage("您尚未未绑定亲情账号");
                return response;
            } else if(familyRelation.getStatus() == 2) {
                ApiResponse response = ApiResponse.unauthorized();
                response.setMessage("您的账户已被冻结");
                return response;
            }
            order.setActualPayerId(familyRelation.getGuardianId());
            order.setSource(1);
        }

        if(orderService.insert(order) != 1) {
            ApiResponse response = ApiResponse.serverInternalError();
            response.setMessage("创建订单失败");
            return response;
        } else {
            ApiResponse response = ApiResponse.ok();
            response.putDataValue("orderId", order.getId());

            //异步支付订单
            new Thread(() -> {
                orderService.payOrder(order);
            }).start();
            return response;
        }
    }

    @UserLoginToken
    @GetMapping("/queryOrders")
    public ApiResponse queryOrders(@RequestParam("status") Integer status, HttpServletRequest request) {
        long userId = CommonUtils.getUserId(request);
        List<Order> orders = orderService.findByStatus(userId, status);
        List<QueryOrderOutput> outputs = new ArrayList<>();
        orders.stream().forEach(order -> {
            QueryOrderOutput orderOutput = new QueryOrderOutput();
            orderOutput.setId(order.getId());
            orderOutput.setStatusStr(statusMap.get(order.getStatus()));
            orderOutput.setPayment(order.getPayment());
            List<String> paymentTypes = converStringToList(order.getPaymentType()).stream().map(paymentTypeMap::get).collect(Collectors.toList());
            orderOutput.setPaymentType(convertListToString(paymentTypes, '/'));
            List<Item> items = converStringToList(order.getItemId()).stream().map(itemService::selectById).collect(Collectors.toList());
            orderOutput.setItems(items);
            orderOutput.setBuyer(accountService.selectById(order.getPayerId()).getUsername());
            orderOutput.setTime(order.getUpdateTime());
            outputs.add(orderOutput);
        });
        ApiResponse response = ApiResponse.ok();
        response.putDataValue("result", outputs);
        return response;
    }

    @UserLoginToken
    @PostMapping("/confirm")
    public ApiResponse confirmOrder(@RequestBody ConfirmOrderInput input, HttpServletRequest request) {
        long userId = CommonUtils.getUserId(request);
        Order order = orderService.selectById(input.getId());
        if(order.getActualPayerId()!=userId) {
            return ApiResponse.unauthorized();
        }
        if(input.getOperator() == 0) {
            //扣除账户
            String[] paymentTypeStr = order.getPaymentType().split(",");
            ArrayList<Integer> paymentType = new ArrayList<>();
            for(String s:paymentTypeStr) {
                paymentType.add(Integer.parseInt(s));
            }
            List<PayMode> payModeList = payModeService.selectByUserId(order.getActualPayerId());
            boolean hasMoney = false;
            boolean hasPayMode = false;

            for(PayMode payMode:payModeList) {
                if(paymentType.contains(payMode.getPaymentType())) {
                    hasPayMode = true;
                    if(payMode.getAmount()>=order.getPayment()) {
                        hasMoney = true;
                        payMode.setAmount(CommonUtils.round(payMode.getAmount()-order.getPayment(), 4));
                        order.setUpdateTime(System.currentTimeMillis()/1000);
                        order.setStatus(1);
                        order.setPaymentType(String.valueOf(payMode.getPaymentType()));
                        payModeService.update(payMode);
                        orderService.update(order);
                        JSONObject contentJson = new JSONObject();
                        contentJson.put("code", 1);
                        contentJson.put("msg", "支付成功");
                        pushService.sendMessageToAlias("PayResult",contentJson.toJSONString(),String.valueOf(order.getPayerId()));
                        break;
                    }
                }
            }

            if(!hasPayMode) {
                order.setUpdateTime(System.currentTimeMillis()/1000);
                order.setStatus(5);
                orderService.update(order);
                JSONObject contentJson = new JSONObject();
                contentJson.put("code", 5);
                contentJson.put("msg", "支付方式不支持");
                pushService.sendMessageToAlias("PayResult",contentJson.toJSONString(),String.valueOf(order.getPayerId()));
                ApiResponse response = ApiResponse.customerError();
                response.setMessage("支付方式不支持");
                return response;
            } else if(!hasMoney) {
                order.setUpdateTime(System.currentTimeMillis()/1000);
                order.setStatus(2);
                orderService.update(order);
                JSONObject contentJson = new JSONObject();
                contentJson.put("code", 2);
                contentJson.put("msg", "余额不足");
                pushService.sendMessageToAlias("PayResult",contentJson.toJSONString(),String.valueOf(order.getPayerId()));
                ApiResponse response = ApiResponse.customerError();
                response.setMessage("余额不足");
                return response;
            } else {
                ApiResponse response = ApiResponse.ok();
                response.setMessage("支付成功");
                return response;
            }
        } else {
            order.setUpdateTime(System.currentTimeMillis()/1000);
            order.setStatus(Integer.parseInt(order.getUrl()));
            orderService.update(order);
            JSONObject contentJson = new JSONObject();
            contentJson.put("code", order.getStatus());
            contentJson.put("msg", order.getStatus()==3?"超过单笔额度且监护人拒绝了这笔支付":"超过本月额度且监护人拒绝了这笔支付");
            pushService.sendMessageToAlias("PayResult",contentJson.toJSONString(),String.valueOf(order.getPayerId()));
            return ApiResponse.ok();
        }
    }

    @UserLoginToken
    @GetMapping("/queryStatus/{orderId}")
    public ApiResponse queryStatus(@PathVariable("orderId") Long orderId, HttpServletRequest request) {
        Order order = orderService.selectById(orderId);
        long userId = CommonUtils.getUserId(request);
        if(order==null) {
            ApiResponse response = ApiResponse.customerError();
            response.setMessage("订单不存在");
            return response;
        }
        if(order.getPayerId()!=userId && order.getActualPayerId()!=userId) {
            return ApiResponse.unauthorized();
        }
        ApiResponse response = ApiResponse.ok();
        response.putDataValue("status", order.getStatus());
        switch (order.getStatus()) {
            case 0: response.setMessage("未支付");break;
            case 1: response.setMessage("支付成功");break;
            case 2: response.setMessage("余额不足");break;
            case 3: response.setMessage("超过单笔额度");break;
            case 4: response.setMessage("超过本月额度");break;
            case 5: response.setMessage("支付方式不支持");break;
        }
        return response;
    }

    private <T> String convertListToString(List<T> strlist, char c){
        StringBuffer sb = new StringBuffer();
        if(strlist!=null){
            for (int i=0;i<strlist.size();i++) {
                if(i==0){
                    sb.append(strlist.get(i));
                }else{
                    sb.append(c).append(strlist.get(i));
                }
            }
        }
        return sb.toString();
    }

    private List<Long> converStringToList(String strs) {
        if (strs!=null) {
            String[] idStrs = strs.trim().split(",");
            if (idStrs.length > 0) {
                List<Long> strsList = new ArrayList<>();
                for (String str : idStrs) {
                    if (str!=null) {
                        strsList.add(Long.parseLong(str.trim()));
                    }
                }
                if (strsList.size() > 0) {
                    return strsList;
                }
            }
        }
        return null;
    }

    @Data
    private static class CreateOrderInput {
        private List<Long> itemIds;
        private Long sellerId;
        private Double payment;
        private String paymentType;
//        private Long time;
    }

    @Data
    private static class QueryOrderOutput {
        Long id;
        String statusStr;
        Double payment;
        String paymentType;
        List<Item> items;
        String buyer;
        Long time;
    }

    @Data
    private static class ConfirmOrderInput {
        Long id;
        Integer operator; //0为确认，1为拒绝
    }
}
