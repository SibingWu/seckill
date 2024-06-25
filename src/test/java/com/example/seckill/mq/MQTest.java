package com.example.seckill.mq;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@SpringBootTest
public class MQTest {

    @Autowired
    private RocketMQService rocketMQService;

    @Test
    public void sendMQTest() throws Exception {
        String topic = "test-example-seckill";
        String body = "Hello, world! " + new Date();
        rocketMQService.sendMessage(topic, body);
    }
}
