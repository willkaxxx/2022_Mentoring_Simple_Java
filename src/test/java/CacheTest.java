import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

class CacheTest {
    @Test
    void LFUStrategyTest() {
        Cache<String, String> cache = new Cache<>(3);

        cache.setRemovalListener(
                () -> System.out.println("I would rather use guava...")
        );

        cache.put("A", "A");
        cache.put("B", "B");
        cache.put("C", "C");

        cache.get("A"); // A - 4; B - 5; C - 3 ==> 'C' should bu evicted
        cache.get("A");
        cache.get("B");
        cache.get("B");
        cache.get("B");
        cache.get("B");
        cache.get("B");
        cache.get("A");
        cache.get("A");
        cache.get("C");
        cache.get("C");
        cache.get("C");

        cache.put("D", "D");

        System.out.println(cache.getValues());
        Assertions.assertNull(cache.get("C"));
    }

    @Test
    void getStatisticsTest() {
        Cache<String, String> cache = new Cache<>(100_000);

        Random random = new Random();

        for (int i = 0; i < 10_000_000; i++) {
            String someStr = Integer.toString(random.nextInt(1_000_000));
            cache.put(someStr, someStr);

            while (random.nextBoolean()) {
                cache.get(someStr);
            }
        }

        System.out.printf("Avg put time: %sns%n", cache.getAvgAddTime());
        System.out.printf("Eviction count: %s%n", cache.getEvictionCount());
    }
}