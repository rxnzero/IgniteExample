package com.dhlee.ignite.ttl;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.IgniteCache;

import java.util.concurrent.TimeUnit;

public class CustomTTLExample {
    public static void main(String[] args) throws InterruptedException {
        Ignite ignite = Ignition.start();
        IgniteCache<Integer, CustomObject> cache = ignite.getOrCreateCache("customCache");

        // Put an entry with a custom TTL
        int key = 1;
        CustomObject value = new CustomObject("Hello, Ignite!", System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10));
        cache.put(key, value);

        // Retrieve the entry
        CustomObject retrievedValue = cache.get(key);
        System.out.println("Retrieved from cache: " + retrievedValue.getMessage());

        // Sleep for some time to let the TTL expire
        Thread.sleep(TimeUnit.SECONDS.toMillis(12));

        // Attempt to retrieve the entry after TTL has expired
        retrievedValue = cache.get(key);
        System.out.println("Retrieved after TTL expiration: " + retrievedValue.getMessage()
        		+", expiration : " + retrievedValue.isExpired() );

        // Close Ignite node when done
        ignite.close();
    }

    static class CustomObject {
        private String message;
        private long expirationTime;

        public CustomObject(String message, long expirationTime) {
            this.message = message;
            this.expirationTime = expirationTime;
        }

        public String getMessage() {
            return message;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
}