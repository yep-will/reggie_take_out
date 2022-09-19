package com.itheima.test;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;

public class JedisTest {
    @Test
    public void testReids(){
        Jedis jedis = new Jedis("localhost", 6379);

        jedis.set("username", "will");

        String value = jedis.get("username");
        System.out.println(value);

        jedis.del("username");

        jedis.close();
    }
}
