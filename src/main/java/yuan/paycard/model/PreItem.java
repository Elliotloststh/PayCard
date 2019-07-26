package yuan.paycard.model;

import lombok.Data;

@Data
public class PreItem {
    private Long id;

    private Long sellerId;

    private String paymentType;

    private Long itemId;

    private Long createTime;
}