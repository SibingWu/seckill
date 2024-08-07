package com.example.seckill.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Collections;

@Slf4j
@Service
public class RedisService {

    @Autowired
    private JedisPool jedisPool;

    /**
     * 设置值
     *
     * @param key
     * @param value
     */
    public RedisService setValue(String key, Long value) {
        Jedis client = jedisPool.getResource();
        client.set(key, value.toString());
        client.close();

        return this; // 方便链式调用 setValue(...).getValue(...)
    }

    /**
     * 设置值
     *
     * @param key
     * @param value
     */
    public void setValue(String key, String value) {
        Jedis jedisClient = jedisPool.getResource();
        jedisClient.set(key, value);
        jedisClient.close();
    }

    /**
     * 获取值
     *
     * @param key
     * @return
     */
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

    /**
     * 超时未支付 redis 库存回滚
     * @param key
     */
    public void revertStock(String key) {
        Jedis client = jedisPool.getResource();
        client.incr(key);
        client.close();
    }

    /**
     * 判断是否在限购名单中
     * @param seckillActivityId
     * @param userId
     * @return
     */
    public boolean isInLimitMember(long seckillActivityId, long userId) {
        Jedis client = jedisPool.getResource();
        boolean sismember = client.sismember("seckillActivity_users:" + seckillActivityId, String.valueOf(userId));
        client.close();

        log.info("userId:{} activityId:{} 在已购名单中:{}", userId, seckillActivityId, sismember);
        return sismember;
    }

    /**
     * 添加限购名单
     * @param seckillActivityId
     * @param userId
     */
    public void addLimitMember(long seckillActivityId, long userId) {
        Jedis client = jedisPool.getResource();
        client.sadd("seckillActivity_users:" + seckillActivityId, String.valueOf(userId));
    }

    /**
     * 移除限购名单
     * @param seckillActivityId
     * @param userId
     */
    public void removeLimitMember(Long seckillActivityId, Long userId) {
        Jedis client = jedisPool.getResource();
        client.srem("seckillActivity_users:" + seckillActivityId, String.valueOf(userId));
        client.close();
    }

    /**
     * 获取分布式锁
     * @param lockKey
     * @param requestId
     * @param expireTime
     * @return
     */
    public boolean tryGetDistributedLock(String lockKey, String requestId, int expireTime) {
        Jedis client = jedisPool.getResource();
        String result = client.set(lockKey, requestId, "NX", "PX", expireTime);
        client.close();

        return "OK".equals(result); // 注意不能写 result.equals("OK")，因为 result 可能为 null，而 null 是不能调用.equals() 的
    }

    /**
     * 释放分布式锁
     * @param lockKey   锁
     * @param requestId 请求标识
     * @return  是否释放成功
     */
    public boolean releaseDistributedLock(String lockKey, String requestId) {
        Jedis client = jedisPool.getResource();
        String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long result = (Long) client.eval(luaScript, Collections.singletonList(lockKey), Collections.singletonList(requestId));
        client.close();

        return result == 1L;
    }
}
