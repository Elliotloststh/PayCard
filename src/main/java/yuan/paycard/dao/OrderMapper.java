package yuan.paycard.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import yuan.paycard.model.Order;

import java.util.List;

@Mapper
public interface OrderMapper {
    int deleteByPrimaryKey(Long id);

    int insert(Order record);

    int insertSelective(Order record);

    Order selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Order record);

    int updateByPrimaryKey(Order record);

    @Select(("select * from tb_order where actual_payer_id=#{actualPayerId} and update_time>=#{start} and update_time<=#{end} and status=1"))
    List<Order> queryDailyOrder(Long actualPayerId, Long start, Long end);

    @Select("select * from tb_order where actual_payer_id=#{actualPayerId} and status=#{status} order by update_time desc")
    List<Order> findByStatus(Long actualPayerId,Integer status);
}