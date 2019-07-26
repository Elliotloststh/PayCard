package yuan.paycard.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import yuan.paycard.dao.PayModeMapper;
import yuan.paycard.model.PayMode;

import java.util.List;

@Service
public class PayModeService {
    @Autowired
    PayModeMapper payModeMapper;

    public PayMode selectById(Long id) {
        return payModeMapper.selectByPrimaryKey(id);
    }

    public int insert(PayMode record) {
        return payModeMapper.insertSelective(record);
    }

    public int update(PayMode record) {
        return payModeMapper.updateByPrimaryKeySelective(record);
    }

    public List<PayMode> selectByUserId(Long userId) {
        return payModeMapper.selectByUserId(userId);
    }

    public int deleteById(Long id) {
        return payModeMapper.deleteByPrimaryKey(id);
    }

    public PayMode findOne(PayMode payMode) {
        return payModeMapper.findOne(payMode);
    }

}
