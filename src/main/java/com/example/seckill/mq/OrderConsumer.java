package com.example.seckill.mq;

import com.alibaba.fastjson.JSON;
import com.example.seckill.db.dao.OrderDao;
import com.example.seckill.db.dao.SeckillActivityDao;
import com.example.seckill.db.po.Order;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
@RocketMQMessageListener(topic = "seckill_order", consumerGroup = "seckill_order_group")
public class OrderConsumer implements RocketMQListener<MessageExt> {

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Override
    @Transactional // 用于声明方法或类应该被包含在一个事务中进行执行
    public void onMessage(MessageExt messageExt) {
        // 1. 解析“创建订单”的请求消息
        String message = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        log.info("接收到了创建订单的请求: " + message);
        Order order = JSON.parseObject(message, Order.class);
        order.setCreateTime(new Date());

        // 2. 扣减库存
        boolean lockStockResult = seckillActivityDao.lockStock(order.getSeckillActivityId());
        // 订单状态 0:没有可用库存，无效订单 1:已创建等待付款 2:支付完成
        if (lockStockResult) {
            // 锁定成功
            // 1:已创建等待付款
            order.setOrderStatus(1);
        } else {
            // 0:没有可用库存，无效订单
            order.setOrderStatus(0);
        }

        // 3. 插入订单
        orderDao.insertOrder(order);
    }
}
