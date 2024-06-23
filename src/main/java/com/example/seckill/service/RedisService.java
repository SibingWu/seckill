package com.example.seckill.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Collections;

@Service
public class RedisService {

    @Autowired
    private JedisPool jedisPool;

    public RedisService setValue(String key, Long value) {
        Jedis client = jedisPool.getResource();
        client.set(key, value.toString());
        client.close();

        return this; // 方便链式调用 setValue(...).getValue(...)
    }

    public String getValue(String key) {
        Jedis client = jedisPool.getResource();
        String value = client.get(key);
        client.close();
        return value;
    }

    /**
     * 用 lua 脚本读取 redis 缓存中库存判断和扣减
     * @param key redis key
     * @return redis是否扣减成功
     */
    public boolean stockDeductValidation(String key) {
        try (Jedis client = jedisPool.getResource()) {
            String luaScript = """
                    if redis.call('exists', KEYS[1]) == 1 then
                        local stock = tonumber(redis.call('get', KEYS[1]))
                        if (stock <= 0) then
                            return -1
                        end;

                        redis.call('decr', KEYS[1]);
                        return stock - 1;
                    end;

                    return -1;
                    """;
            // .eval 执行lua脚本
            // Collections.singletonList(key) 将参数传给lua脚本，key对应的是KEYS[1]
            // Collections.emptyList() 代表没有额外参数
            Long stock = (Long) client.eval(luaScript, Collections.singletonList(key), Collections.emptyList());

            if (stock < 0) {
                System.out.println("库存不足");
                return false;
            }
            System.out.println("恭喜，抢购成功！");
            return true;
        } catch (Throwable throwable) {
            System.out.println("库存扣件失败：" + throwable);
            return false;
        }
    }
}
