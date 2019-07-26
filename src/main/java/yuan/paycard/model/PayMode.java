package yuan.paycard.model;

import lombok.Data;

@Data
public class PayMode {
    private Long id;

    private Long userId;

    private Integer paymentType;

    private String name;

    private Double amount;

    private String bizId;

    private Long createTime;

}