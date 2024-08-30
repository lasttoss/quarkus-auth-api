package io.zw.auth.api.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;

@ApplicationScoped
public class RedisService {

    @Inject
    RedissonClient redissonClient;

    public void saveWithExpiredTime(String key, Object value, Duration duration) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        bucket.set(value, duration);
    }

    public void saveWithoutExpiredTime(String key, Object value) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        bucket.set(value);
    }

    public boolean checkIfKeyExists(String key) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        return bucket.isExists();
    }

    public Object get(String key) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    public void delete(String key) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        if (bucket.isExists()) {
            bucket.delete();
        }
    }
}
