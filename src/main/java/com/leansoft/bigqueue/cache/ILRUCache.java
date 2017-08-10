package com.leansoft.bigqueue.cache;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

/**
 * LRU cache ADT
 * 
 * @author bulldog
 *
 * @param <K> the key
 * @param <V> the value
 */
public interface ILRUCache<K, V extends Closeable> {
	
	/**
	 * Put a keyed resource with specific ttl into the cache
	 * 将带有特定ttl的键控资源放到缓存中
	 * 
	 * This call will increment the reference counter of the keyed resource.
	 * 这个调用将增加键控资源的引用计数器。
	 *
	 * @param key the key of the cached resource 缓存的资源的key
	 * @param value the to be cached resource 缓存的资源的
	 * @param ttlInMilliSeconds time to live in milliseconds 以毫秒计的时间
	 */
	public void put(final K key, final V value, final long ttlInMilliSeconds);
	
	/**
	 * Put a keyed resource with default ttl into the cache 将带有ttl的key放入到cache中
	 * 
	 * This call will increment the reference counter of the keyed resource.这个调用将增加键控资源的引用计数器。
	 * 
	 * @param key the key of the cached resource
	 * @param value the to be cached resource
	 */
	public void put(final K key, final V value);
	
	
	/**
	 * Get a cached resource with specific key
	 * 获取带有特定密钥的缓存资源
	 * 
	 * This call will increment the reference counter of the keyed resource.
	 * 这个调用将增加键控资源的引用计数器。
	 * 
	 * @param key the key of the cached resource
	 * 缓存资源的key
	 * @return cached resource if exists
	 * 缓存资源如果存在 return
	 */
	public V get(final K key);
	
	/**
	 * Release the cached resource with specific key
	 * 使用特定的键释放缓存的资源
	 * 
	 * This call will decrement the reference counter of the keyed resource.
	 * 这个调用将减少键控资源的引用计数器。
	 * 
	 * @param key
	 */
	public void release(final K key);
	
	/**
	 * Remove the resource with specific key from the cache and close it synchronously afterwards.
	 * 从高速缓存中删除资源，然后同步关闭它。
	 * 
	 * @param key the key of the cached resource 缓存资源的key
	 * @return the removed resource if exists 存资源如果存在 return
	 * @throws IOException exception thrown if there is any IO error 如果有任何IO错误则抛出异常
	 */
	public V remove(final K key) throws IOException;
	
	/**
	 * Remove all cached resource from the cache and close them asynchronously afterwards.
	 * 从高速缓存中删除资源，然后同步关闭它。
	 * 
	 * @throws IOException exception thrown if there is any IO error
	 * 如果有任何IO错误则抛出异常
	 */
	public void removeAll() throws IOException;
	
	/**
	 * The size of the cache, equals to current total number of cached resources.
	 * 缓存的大小，等于当前缓存资源的总数量。
	 * 
	 * @return the size of the cache
	 * return缓存的大小
	 */
	public int size();
	
	/**
	 * All values cached
	 * 所以的缓存值
	 * @return a collection
	 * 返回一个集合
	 */
	public Collection<V> getValues();
}
