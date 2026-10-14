package example;

public class BusinessWeigher implements com.github.benmanes.caffeine.cache.Weigher<Object, Object> {
    @Override
    public int weigh(Object key, Object value) {
        return 1;
    }
}
