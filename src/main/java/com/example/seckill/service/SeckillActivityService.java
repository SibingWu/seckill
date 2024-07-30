package com.example.seckill.service;

import com.alibaba.fastjson.JSON;
import com.example.seckill.db.dao.OrderDao;
import com.example.seckill.db.dao.SeckillActivityDao;
import com.example.seckill.db.dao.SeckillCommodityDao;
import com.example.seckill.db.po.Order;
import com.example.seckill.db.po.SeckillActivity;
import com.example.seckill.db.po.SeckillCommodity;
import com.example.seckill.mq.RocketMQService;
import com.example.seckill.util.SnowFlake;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
public class SeckillActivityService {

    @Autowired
    private RedisService redisService;

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Autowired
    private RocketMQService rocketMQService;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private SeckillCommodityDao seckillCommodityDao;


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
        String seckillOrderTopic = "seckill_order";
        String body = JSON.toJSONString(order);
        rocketMQService.sendMessage(seckillOrderTopic, body);

        /*
         * 3.发送“订单付款状态校验”的延迟消息
         * 开源RocketMQ支持延迟消息，但是不支持秒级精度。默认支持18个level的延迟消息，这是通过broker端的messageDelayLevel配置项确定的，
         * 如下:
         * messageDelayLevel=1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
         */
        String payStatusCheckTopic = "pay_status_check";
        rocketMQService.sendDelayMessage(payStatusCheckTopic, JSON.toJSONString(order), 3);

        return order;
    }

    public void pushSeckillInfoToRedis(long seckillActivityId) {
        SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        redisService.setValue("seckillActivity:" + seckillActivityId, JSON.toJSONString(seckillActivity));

        SeckillCommodity seckillCommodity = seckillCommodityDao.querySeckillCommodityById(seckillActivity.getCommodityId());
        redisService.setValue("seckillCommodity:" + seckillActivity.getCommodityId(), JSON.toJSONString(seckillCommodity));
    }

    /**
     * 订单支付完成处理
     * @param orderNo
     */
    public void payOrderProcess(String orderNo) throws Exception {
        log.info("完成支付订单 订单号：" + orderNo);
        Order order = orderDao.queryOrder(orderNo);

        // 1.1 判断订单是否存在
        if (order == null) {
            log.error("订单号对应订单不存在：" + orderNo);
            return;
        }

        // 1.2 判断订单是否为未支付状态
        // 订单状态 0:没有可用库存，无效订单 1:已创建等待付款 2:支付完成
        if (order.getOrderStatus() != 1) {
            log.error("订单状态无效：" + orderNo);
            return;
        }

        // 2. 订单支付完成
        order.setPayTime(new Date());
        order.setOrderStatus(2);
        orderDao.updateOrder(order);

        // 3. 发送订单付款成功消息
        String payDoneTopic = "pay_done";
        rocketMQService.sendMessage(payDoneTopic, JSON.toJSONString(order));
    }
}
