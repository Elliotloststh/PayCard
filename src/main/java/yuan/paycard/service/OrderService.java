package yuan.paycard.service;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yuan.paycard.dao.OrderMapper;
import yuan.paycard.model.Account;
import yuan.paycard.model.FamilyRelation;
import yuan.paycard.model.Order;
import yuan.paycard.model.PayMode;
import yuan.paycard.utils.CommonUtils;
import yuan.paycard.utils.RedisClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

@Service
public class OrderService {
    @Autowired
    OrderMapper orderMapper;

    @Autowired
    AccountService accountService;

    @Autowired
    PushService pushService;

    @Autowired
    FamilyRelationService familyRelationService;

    @Autowired
    RedisClient redisClient;

    @Autowired
    PayModeService payModeService;

    public Order selectById(Long id) {
        return orderMapper.selectByPrimaryKey(id);
    }

    public int insert(Order order) {
        return orderMapper.insertSelective(order);
    }

    public int update(Order order) {
        return orderMapper.updateByPrimaryKeySelective(order);
    }

//    @Transactional
    public void payOrder(Order order) {
        ExecutorService executor = Executors.newCachedThreadPool();
        FutureTask<PayResult> future = new FutureTask<>(() -> {
            Thread.sleep(1000);
            //更新缓存
            if(order.getSource() == 1) {
                FamilyRelation familyRelation = familyRelationService.findRelation(order.getPayerId());
                if(familyRelation.getSingleLimit() < order.getPayment()) {
                    order.setStatus(6);
                    order.setUrl("3");
                    order.setUpdateTime(System.currentTimeMillis()/1000);
                    update(order);
                    return PayResult.UNCONFIRMED;
                }
                Calendar cal = Calendar.getInstance();
                int month = cal.get(Calendar.MONTH) + 1;
                String key = "month_payment_"+familyRelation.getId()+"_"+month;
                if(redisClient.isExist(key)) {
                    double total =  Double.parseDouble(redisClient.get(key));
                    if(total+order.getPayment() > familyRelation.getMonthLimit()) {
                        order.setStatus(6);
                        order.setUrl("4");
                        order.setUpdateTime(System.currentTimeMillis()/1000);
                        update(order);
                        return PayResult.UNCONFIRMED;
                    }
                    redisClient.set(key,String.valueOf(total+order.getPayment()));
                } else {
                    redisClient.set(key,String.valueOf(order.getPayment()));
                }
            }

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
                        update(order);
                        break;
                    }
                }
            }

            if(!hasPayMode) {
                order.setUpdateTime(System.currentTimeMillis()/1000);
                order.setStatus(5);
                update(order);
                return PayResult.PAYMODE_UNSUPPORT;
            } else if(!hasMoney) {
                order.setUpdateTime(System.currentTimeMillis()/1000);
                order.setStatus(2);
                update(order);
                return PayResult.INSUFFICIENT;
            }

            return PayResult.SUCCESS;
        });

        executor.execute(future);

        PayResult result = null;

        try {
            result = future.get(10000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            future.cancel(true);
        } catch (ExecutionException e) {
            future.cancel(true);
        } catch (TimeoutException e) {
            future.cancel(true);
        } finally {
            executor.shutdown();
        }

        if(result != null && result!=PayResult.UNCONFIRMED && order.getSource()==1) {
            JSONObject contentJson = new JSONObject();
            contentJson.put("code", result.value);
            contentJson.put("msg", result.description);
            pushService.sendMessageToAlias("PayResult",contentJson.toJSONString(),String.valueOf(order.getPayerId()));
            Account family = accountService.selectById(order.getPayerId());
            SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日HH:mm");
            pushService.sendNotificationToAlias("您的亲情账户 "+family.getUsername()+" 于" + sdf.format(new Date())
                            + "消费"+order.getPayment()+"元", String.valueOf(order.getPayerId()));
        } else if(result==PayResult.UNCONFIRMED) {
            //TODO 发超额度消息给iOS
        } else if(order.getSource()==0) {
            //TODO 发支付结果消息给iOS
        }
    }

    public List<Order> queryDailyOrder(Long actualPayerId, Long start, Long end) {
        return orderMapper.queryDailyOrder(actualPayerId, start, end);
    }

    public List<Order> findByStatus(Long actualPayerId, Integer status) {
        return orderMapper.findByStatus(actualPayerId,status);
    }
}

enum PayResult {
    SUCCESS(1,"支付成功"),
    INSUFFICIENT(2,"余额不足"),
    SINGLE_LIMIT(3,"超过单笔额度"),
    MONTH_LIMIT(4,"超过本月额度"),
    PAYMODE_UNSUPPORT(5,"支付方式不支持"),
    UNCONFIRMED(6,"未确认")
    ;

    int value;
    String description;
    PayResult(int value, String description) {
        this.value = value;
        this.description = description;
    }
}
