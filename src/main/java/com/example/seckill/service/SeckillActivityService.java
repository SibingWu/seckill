package com.example.seckill.service;

import com.alibaba.fastjson.JSON;
import com.example.seckill.db.dao.SeckillActivityDao;
import com.example.seckill.db.po.Order;
import com.example.seckill.db.po.SeckillActivity;
import com.example.seckill.mq.RocketMQService;
import com.example.seckill.util.SnowFlake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SeckillActivityService {

    @Autowired
    private RedisService redisService;

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Autowired
    private RocketMQService rocketMQService;


    /**
     * datacenterId; 数据中心
     * machineId; 机器标识
     * 在分布式环境中可以从机器配置上读取 * 单机开发环境中先写死
     */
    private SnowFlake snowFlake = new SnowFlake(1, 1);

    /**
     * 判断商品是否还有 redis 库存
     *
     * @param activityId 秒杀活动ID
     * @return redis库存是否扣减成功
     */
    public boolean seckillStockValidator(long activityId) {
        String key = "stock:" + activityId;
        return redisService.stockDeductValidation(key);
    }

    /**
     * 创建订单
     *
     * @param seckillActivityId
     * @param userId
     * @return
     * @throws Exception
     */
    public Order createOrder(long seckillActivityId, long userId) throws Exception {
        // 1. 创建订单
        SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);

        Order order = new Order();

        // 采用雪花算法生成订单ID
        order.setOrderNo(String.valueOf(snowFlake.nextId()));
        order.setSeckillActivityId(seckillActivityId);
        order.setUserId(userId);
        order.setOrderAmount(seckillActivity.getSeckillPrice().longValue());

        // 2. 发送“创建订单”消息
        String topic = "seckill_order";
        String body = JSON.toJSONString(order);
        rocketMQService.sendMessage(topic, body);

        return order;
    }
}
