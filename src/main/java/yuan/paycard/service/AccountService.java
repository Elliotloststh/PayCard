package yuan.paycard.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import yuan.paycard.dao.AccountMapper;
import yuan.paycard.model.Account;

@Service
public class AccountService {
    @Autowired
    AccountMapper accountMapper;

    public int register(Account account) {
        return accountMapper.insertSelective(account);
    }

    public Account findOne(Account query) {
        return accountMapper.findOne(query);
    }

    public String getToken(Account account) {
        String token="";
        token= JWT.create().withAudience(String.valueOf(account.getId()))
                .sign(Algorithm.HMAC256(account.getPassword()));
        return token;
    }

    public Account selectById(Long id) {
        return accountMapper.selectByPrimaryKey(id);
    }

    public int update(Account record) {
        return accountMapper.updateByPrimaryKeySelective(record);
    }

//    @Data
//    @AllArgsConstructor
//    @NoArgsConstructor
//    public static class  {
//        private int type; // 1:seller   2:buyer
//        private String username;
//        private String password;
//    }
}
