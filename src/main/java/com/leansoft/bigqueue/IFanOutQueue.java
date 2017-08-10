package com.leansoft.bigqueue;

import java.io.Closeable;
import java.io.IOException;

/**
 * FanOut queue ADT
 * 
 * @author bulldog
 *
 */
public interface IFanOutQueue extends Closeable {
	
	/**
	 * Constant represents earliest timestamp
	 * 常数代表最早的时间戳
	 */
	public static final long EARLIEST = -1;
	/**
	 * Constant represents latest timestamp
	 * 常数代表最新的时间戳
	 */
	public static final long LATEST = -2;
	
	/**
	 * Determines whether a fan out queue is empty
	 * 确定扇出队列是否为空
	 * 
	 * @param fanoutId the fanout identifier 扇出标识符
	 * @return true if empty, false otherwise 如果是空的，则是假的
	 */
	public boolean isEmpty(String fanoutId) throws IOException;
	
	/**
	 * Determines whether the queue is empty
	 * 确定队列是否为空
	 * 
	 * @return true if empty, false otherwise
	 * 如果为空则返回true ，否则返回false
	 *
	 */
	public boolean isEmpty();
	
	/**
	 * Adds an item at the back of the queue
	 * 在队列的后面添加一个项目
	 * 
	 * @param data to be enqueued data 队列的数据
	 * @return index where the item was appended 添加项的索引
	 * @throws IOException exception throws if there is any IO error during enqueue operation.
	 * 如果在队列操作中存在IO错误，则异常抛出异常。
	 */
	public long enqueue(byte[] data)  throws IOException;
	
	/**
	 * Retrieves and removes the front of a fan out queue
	 * 检索和删除扇出队列的前端
	 * 
	 * @param fanoutId the fanout identifier 扇出标识符
	 * @return data at the front of a queue 队列前端的数据
	 * @throws IOException exception throws if there is any IO error during dequeue operation.
	 * 如果在dequeue操作中有任何IO错误，则异常抛出。
	 */
	public byte[] dequeue(String fanoutId) throws IOException;
	
	/**
	 * Peek the item at the front of a fanout queue, without removing it from the queue
	 * 在一个fanout队列的前端看到这个条目，而不从队列中删除它。
	 * 
	 * @param fanoutId the fanout identifier 扇出标识符
	 * @return data at the front of a queue 队列前端的数据
	 * @throws IOException exception throws if there is any IO error during peek operation.
	 * 如果在peek操作中有任何IO错误，则异常抛出异常。
	 */
	public byte[] peek(String fanoutId)  throws IOException;
	
	
	/**
	 * Peek the length of the item at the front of a fan out queue
	 * 在一个扇出队列的前端看到项目的长度
	 * 
	 * @param fanoutId the fanout identifier 扇出标识符
	 * @return data at the front of a queue 队列前端的数据
	 * @throws IOException exception throws if there is any IO error during peek operation.
	 * 如果在peek操作中有任何IO错误，则异常抛出异常。
	 */
	public int peekLength(String fanoutId) throws IOException;
	
	/**
	 * Peek the timestamp of the item at the front of a fan out queue
	 * 在扇出队列的前端看到项目的时间戳
	 *
	 * @param fanoutId the fanout identifier  扇出标识符
	 * @return data at the front of a queue  队列前端的数据
	 * @throws IOException exception throws if there is any IO error during peek operation.
	 * 如果在peek操作中有任何IO错误，则异常抛出异常。
	 */
	public long peekTimestamp(String fanoutId) throws IOException;
	
	/**
	 * Retrieves data item at the specific index of the queue
	 * 在队列的特定索引中检索数据项
	 * 
	 * @param index data item index 数据项指数
	 * @return data at index 数据索引
	 * @throws IOException exception throws if there is any IO error during fetch operation.
	 * 如果在获取操作中存在IO错误，则异常抛出异常。
	 */
	public byte[] get(long index) throws IOException;
	
	
	/**
	 * Get length of data item at specific index of the queue
	 * 在队列的特定索引中获取数据项的长度
	 * 
	 * @param index data item index 数据项指数
	 * @return length of data item 数据项的长度
	 * @throws IOException exception throws if there is any IO error during fetch operation.
	 * 如果在获取操作中存在IO错误，则异常抛出异常。
	 */
	public int getLength(long index) throws IOException;
	
	/**
	 * Get timestamp of data item at specific index of the queue, this is the timestamp when corresponding item was appended into the queue.
	 * 在队列的特定索引中获取数据项的时间戳，这是将相应项附加到队列中的时间戳。
	 * 
	 * @param index data item index 数据项指数
	 * @return timestamp of data item 的时间戳数据项
	 * @throws IOException exception throws if there is any IO error during fetch operation.
	 * 如果在获取操作中存在IO错误，则异常抛出异常。
	 */
	public long getTimestamp(long index) throws IOException;
	
	/**
	 * Total number of items remaining in the fan out queue
	 * 在扇出队列中剩余的项目总数
	 * 
	 * @param fanoutId the fanout identifier 扇出标识符
	 * @return total number
	 */
	public long size(String fanoutId) throws IOException ;
	
	
	/**
	 * Total number of items remaining in the queue.
	 * 队列中剩余的项目总数。
	 *  
	 * @return total number
	 */
	public long size();
	
	/**
	 * Force to persist current state of the queue,
	 * 强制保持队列的当前状态，
	 * 
	 * normally, you don't need to flush explicitly since:
	 * 通常情况下，您不需要显式地刷新:
	 * 1.) FanOutQueue will automatically flush a cached page when it is replaced out,
	 * 1)。当它被替换时，FanOutQueue将自动刷新缓存页面，
	 * 2.) FanOutQueue uses memory mapped file technology internally, and the OS will flush the changes even your process crashes,
	 * 
	 * call this periodically only if you need transactional reliability and you are aware of the cost to performance.
	 * 2)。FanOutQueue在内部使用内存映射文件技术，而且操作系统将刷新这些更改，
	 * 即使您的进程崩溃，也要在需要事务可靠性的情况下定期调用此操作，并且您知道性能的成本。
	 */
	public void flush();
	
	/**
	 * Remove all data before specific timestamp, truncate back files and advance the queue front if necessary.
	 * 在特定的时间戳之前删除所有数据，截断回文件，并在必要时提前对队列进行预先处理。
	 * 
	 * @param timestamp a timestamp 一个时间戳
	 * @throws IOException exception thrown if there was any IO error during the removal operation
	 * 如果在删除操作中存在IO错误，则抛出异常
	 */
	void removeBefore(long timestamp) throws IOException;
	
	/**
	 * Limit the back file size of this queue, truncate back files and advance the queue front if necessary.
	 * 限制该队列的后文件大小，截断后文件，并在必要时提前将队列提前。
	 * 
	 * Note, this is a best effort call, exact size limit can't be guaranteed
	 * 注意，这是一个最好的调用，精确的大小限制是不能保证的
	 * 
	 * @param sizeLmit size limit
	 * @throws IOException exception thrown if there was any IO error during the operation
	 * 如果在操作过程中出现了IO错误，则抛出异常
	 */
	void limitBackFileSize(long sizeLmit) throws IOException;
	
	/**
	 * Current total size of the back files of this queue
	 * 当前队列的后文件的总大小
	 * 
	 * @return total back file size 回总文件大小
	 * @throws IOException exception thrown if there was any IO error during the operation
	 * 如果在操作过程中出现了IO错误，则抛出异常
	 */
	long getBackFileSize() throws IOException;
	
	
    /**
     * Find an index closest to the specific timestamp when the corresponding item was enqueued.
	 * 当相应的项被排队时，找到一个与特定时间戳最接近的索引。
     * to find latest index, use {@link #LATEST} as timestamp.
	 * 要找到最新的索引，请使用@link最新的时间戳。
     * to find earliest index, use {@link #EARLIEST} as timestamp.
	 * 要找到最早的索引，请使用@link最早的时间戳。
     * 
     * @param timestamp when the corresponding item was appended
	 *                  当相应的项目被追加时
     * @return an index 一个索引
     * @throws IOException exception thrown during the operation
	 * 在操作期间抛出的异常
     */
    long findClosestIndex(long timestamp) throws IOException;
    
    /**
     * Reset the front index of a fanout queue.
	 * 重置一个扇形队列的前索引。
     * 
     * @param fandoutId fanout identifier 扇出标识符
     * @param index target index 目标指数
     * @throws IOException exception thrown during the operation
	 * 在操作期间抛出的异常
     */
    void resetQueueFrontIndex(String fanoutId, long index) throws IOException;
    
	/**
	 * Removes all items of a queue, this will empty the queue and delete all back data files.
	 * 删除队列的所有项，这将清空队列，并删除所有的后数据文件。
	 * 
	 * @throws IOException exception throws if there is any IO error during dequeue operation.
	 * 如果在dequeue操作中有任何IO错误，则异常抛出。
	 */
	public void removeAll() throws IOException;
	
	/**
	 * Get the queue front index, this is the earliest appended index
	 * 获取队列前索引，这是最早的附加索引
	 * 
	 * @return an index
	 */
	public long getFrontIndex();
	
	/**
	 * Get front index of specific fanout queue
	 * 获取特定的扇出队列的前端索引
	 * 
	 * @param fanoutId fanout identifier 扇出标识
	 * @return an index 一个索引
	 */
	public long getFrontIndex(String fanoutId) throws IOException;
	
	/**
	 * Get the queue rear index, this is the next to be appended index
	 * 获取队列后索引，这是附加索引的下一个索引
	 * 
	 * @return an index
	 */
	public long getRearIndex();

}
