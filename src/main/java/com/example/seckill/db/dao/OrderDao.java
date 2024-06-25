package com.example.seckill.db.dao;

import com.example.seckill.db.po.Order;

public interface OrderDao {

    public void insertOrder(Order order);

    public Order queryOrder(String orderNo);

    public void updateOrder(Order order);
}
