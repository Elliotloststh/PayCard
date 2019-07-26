package yuan.paycard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Account {
    private Long id;

    private String username;

    private String password;

    private String phoneNumber;

    private String avatar;

    private Integer status;

    private Integer type;

    private Long createTime;

}