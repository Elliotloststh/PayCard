package yuan.paycard.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import yuan.paycard.model.PayMode;

import java.util.List;

@Mapper
public interface PayModeMapper {
    int deleteByPrimaryKey(Long id);

    int insert(PayMode record);

    int insertSelective(PayMode record);

    PayMode selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(PayMode record);

    int updateByPrimaryKey(PayMode record);

    @Select("select * from paymode where user_id=#{userId}")
    List<PayMode> selectByUserId(Long userId);

    PayMode findOne(PayMode payMode);

}