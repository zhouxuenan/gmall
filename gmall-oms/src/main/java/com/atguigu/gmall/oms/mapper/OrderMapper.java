package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author zxn
 * @email zxn@atguigu.com
 * @date 2020-10-28 09:58:50
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
	
}
