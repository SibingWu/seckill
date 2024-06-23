package com.example.seckill.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RedisServiceTest {

    @Autowired
    private RedisService redisService;

    @Test
    void setValue() {
        // Arrange
        String key = "test:1";

        // 运行 redis-cli 去 get test:1 是真的能读到 "100"

        // Act & Assert
        String value = redisService.setValue(key, 100L).getValue(key);
        assertEquals(100L, Long.parseLong(value));
    }

    @Test
    void getValue() {
        /*
         * 共享的 Spring 应用上下文:
         *  在使用 @SpringBootTest 时，Spring 会为所有测试方法共享同一个应用上下文，
         * 这意味着所有的测试方法都会在同一个 Redis 实例上操作。
         * 因此，如果 setValue 测试方法先运行并设置了值，
         * 那么 getValue 测试方法在读取同一个键时会得到预期的值。
         *
         * 数据持久性: Redis 本身是一个持久化存储，
         * 如果一个测试方法在 Redis 中设置了一个值，除非被明确删除，
         * 否则该值会一直存在，因此其他测试方法能够读取到它。
         */
        // Act
        String value = redisService.getValue("test:1");

        // Assert
        assertEquals(100L, Long.parseLong(value));
    }

    @Test
    void stockDeductValidation() {
        // Arrange
        String key = "test:1";

        // Act
        boolean result = redisService.stockDeductValidation(key);
        String value = redisService.getValue(key);

        // Assert
        assertTrue(result);
        assertEquals(99L, Long.parseLong(value));
    }
}