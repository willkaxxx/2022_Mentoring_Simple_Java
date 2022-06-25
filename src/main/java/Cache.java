import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class Cache<K, V> {

    private Runnable removalListener;
    private final SortedMap<LFUCacheKey<K>, V> storage;
    private final Map<K, LFUCacheKey<K>> keys;
    private final int maxSize;

    private int timesAdded;
    private long totalAddTime;
    private int evictionCount;


    public Cache(int maxSize, Runnable removalListener) {
        storage = new ConcurrentSkipListMap<>((a, b) -> {
            if (a.callCount == b.callCount) {
                return Integer.compare(a.key.hashCode(), b.key.hashCode());
            }
            return Integer.compare(a.callCount, b.callCount);
        });
        keys = new ConcurrentHashMap<>();
        this.maxSize = maxSize;

        timesAdded = 0;
        totalAddTime = 0;
        evictionCount = 0;

        this.removalListener = removalListener;
    }

    public Cache(int maxSize) {
        this(maxSize, () -> {});
    }

    public void put(K key, V value) {
        long putStart = System.nanoTime();
        if (storage.size() >= maxSize) {
            evict();
        }
        LFUCacheKey<K> cacheKey = new LFUCacheKey<>(key);
        keys.put(key, cacheKey);
        storage.put(cacheKey, value);

        updateAvgPutTime(putStart);
    }

    public V get(K key) {
        if (keys.containsKey(key)) {
            LFUCacheKey<K> cacheKey = keys.get(key);
            V val = storage.get(cacheKey);
            reload(cacheKey);
            return val;
        }
        return null;
    }

    public Collection<V> getValues() {
        return storage.values();
    }

    /**
     * in nanoseconds
     */
    public long getTotalAddTime() {
        return totalAddTime;
    }

    public long getAvgAddTime() {
        return totalAddTime / timesAdded;
    }

    public int getEvictionCount() {
        return evictionCount;
    }

    public void setRemovalListener(Runnable removalListener){
        this.removalListener = removalListener;
    }

    private void updateAvgPutTime(long newEntryPutTime) {
        totalAddTime += System.nanoTime() - newEntryPutTime;
        timesAdded++;
    }

    private void reload(LFUCacheKey<K> cacheKey) {
        V v = storage.get(cacheKey);
        storage.remove(cacheKey);
        cacheKey.incrementCallsCounter();
        storage.put(cacheKey, v);
    }

    private void evict(){
        LFUCacheKey<K> remKey = storage.firstKey();
        storage.remove(remKey);
        keys.remove(remKey.key);
        evictionCount++;

        removalListener.run();
    }

    private static class LFUCacheKey<K> {
        private final K key;
        private int callCount;

        public LFUCacheKey(K key) {
            this.key = key;
            callCount = 0;
        }

        public void incrementCallsCounter() {
            callCount++;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LFUCacheKey<?> that = (LFUCacheKey<?>) o;
            return key.equals(that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }
}
