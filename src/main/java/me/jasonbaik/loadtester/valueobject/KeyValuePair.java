package me.jasonbaik.loadtester.valueobject;

public class KeyValuePair<K, V> {

	public KeyValuePair(K key, V value) {
		super();
		this.key = key;
		this.value = value;
	}

	public K key;
	public V value;

}