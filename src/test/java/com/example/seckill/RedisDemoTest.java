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
    public void setTest(){
        redisService.setValue("age",100L);
    }

    @Test
    public void  getTest(){
        String age =  redisService.getValue("age");
        System.out.println(age);
    }

    @Test
    public void  stockTest(){
        redisService.setValue("stock:19",10L);
    }

    @Test
    public void getStockTest(){
        String stock =  redisService.getValue("stock:19");
        System.out.println(stock);
    }

    @Test
    public void stockDeductValidationTest(){
        boolean result =  redisService.stockDeductValidation("stock:12");
        System.out.println("result:"+result);
        String stock =  redisService.getValue("stock:12");
        System.out.println("stock:"+stock);
    }
}
