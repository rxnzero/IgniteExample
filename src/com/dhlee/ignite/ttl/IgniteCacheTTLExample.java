package com.dhlee.ignite.ttl;

import java.util.concurrent.TimeUnit;

import javax.cache.configuration.FactoryBuilder;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;

public class IgniteCacheTTLExample {
    public static void main(String[] args) {
        Ignite ignite = Ignition.start();

        CacheConfiguration<Integer, String> cacheCfg = new CacheConfiguration<>();
        cacheCfg.setName("myCache");
        cacheCfg.setExpiryPolicyFactory(FactoryBuilder.factoryOf(new CreatedExpiryPolicy(new Duration(TimeUnit.SECONDS, 60))));
        
        // Additional cache configuration settings...
        
        ignite.getOrCreateCache(cacheCfg);

        // Use the cache...
        
        ignite.close();
    }
}
