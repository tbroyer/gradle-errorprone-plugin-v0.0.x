package net.ltgt.gradle.errorprone;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.gradle.api.internal.provider.AbstractProvider;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;

class DefaultMapProperty<K, V> extends AbstractProvider<Map<K, V>> implements MapProperty<K, V> {
  @SuppressWarnings("unchecked")
  static <K, V> MapProperty<K, V> create(ObjectFactory objectFactory) {
    return new DefaultMapProperty<K, V>((SetProperty) objectFactory.setProperty(KeyValue.class));
  }

  private final SetProperty<KeyValue<K, V>> entries;

  private DefaultMapProperty(SetProperty<KeyValue<K, V>> entries) {
    this.entries = entries;
  }

  @Override
  public void put(K key, V value) {
    entries.add(new SimpleKeyValue<>(key, value));
  }

  @Override
  public void put(K key, Provider<? extends V> valueProvider) {
    entries.add(new KeyValueFromProvider<>(key, valueProvider));
  }

  @Override
  public void putAll(Provider<? extends Map<K, V>> provider) {
    entries.addAll(provider.map(DefaultMapProperty::toIterable));
  }

  private static <K, V> Iterable<KeyValue<K, V>> toIterable(Map<K, V> map) {
    return map.entrySet()
        .stream()
        .map(e -> new SimpleKeyValue<>(e.getKey(), e.getValue()))
        .collect(Collectors.toList());
  }

  @Nullable
  @Override
  public Class<Map<K, V>> getType() {
    return null;
  }

  @Override
  public void set(@Nullable Map<K, V> value) {
    entries.set(value == null ? null : toIterable(value));
  }

  @Override
  public void set(Provider<? extends Map<K, V>> provider) {
    entries.set(provider.map(DefaultMapProperty::toIterable));
  }

  @Nullable
  @Override
  public Map<K, V> getOrNull() {
    return entries
        .map(
            s ->
                Collections.unmodifiableMap(
                    s.stream().collect(Collectors.toMap(KeyValue::getKey, KeyValue::getValue))))
        .getOrNull();
  }

  private abstract static class KeyValue<K, V> {
    private final K key;

    protected KeyValue(K key) {
      this.key = key;
    }

    final K getKey() {
      return key;
    }

    abstract V getValue();

    @Override
    public final boolean equals(Object obj) {
      if (obj == null || !getClass().equals(obj.getClass())) {
        return false;
      }
      @SuppressWarnings("unchecked")
      KeyValue<K, V> other = (KeyValue<K, V>) obj;
      return Objects.equals(key, other.key);
    }

    @Override
    public int hashCode() {
      return key.hashCode();
    }
  }

  private static class SimpleKeyValue<K, V> extends KeyValue<K, V> {
    private final V value;

    SimpleKeyValue(K key, V value) {
      super(key);
      this.value = value;
    }

    @Override
    public V getValue() {
      return value;
    }
  }

  private static class KeyValueFromProvider<K, V> extends KeyValue<K, V> {
    private final Provider<? extends V> valueProvider;

    KeyValueFromProvider(K key, Provider<? extends V> valueProvider) {
      super(key);
      this.valueProvider = valueProvider;
    }

    @Override
    public V getValue() {
      return valueProvider.get();
    }
  }
}
