package com.example.seckill;

import com.example.seckill.service.RedisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class RedisDemoTest {

    @Autowired
    private RedisService redisService;

    @Test
    public void stockTest() {
        String value = redisService.setValue("stock:19", 10L).getValue("stock:19");
        System.out.println(value);
    }
}
