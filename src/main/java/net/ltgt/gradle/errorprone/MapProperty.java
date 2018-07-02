package net.ltgt.gradle.errorprone;

import java.util.Map;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

public interface MapProperty<K, V> extends Property<Map<K, V>> {
  void put(K key, V value);

  void put(K key, Provider<? extends V> valueProvider);

  void putAll(Provider<? extends Map<K, V>> provider);
}
