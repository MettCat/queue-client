package com.leansoft.bigqueue;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.Closeable;
import java.io.IOException;

/**
 * Queue ADT
 * 
 * @author bulldog
 *
 */
public interface 	IBigQueue extends Closeable {

	/**
	 * Determines whether a queue is empty
	 * 确定队列是否为空
	 * 
	 * @return ture if empty, false otherwise 如果是空的则返回ture ，否则反正false
	 */
	public boolean isEmpty();
	
	/**
	 * Adds an item at the back of a queue
	 * 在队列的后面添加一个项目
	 * 
	 * @param data to be enqueued data 队列的数据
	 * @throws IOException exception throws if there is any IO error during enqueue operation.
	 * exception throws if there is any IO error during enqueue operation.
	 */
	public void enqueue(byte[] data)  throws IOException;
	
	/**
	 * Retrieves and removes the front of a queue
	 * 检索和删除队列的前端
	 *
	 * @return data at the front of a queue 队列前端的数据
	 * @throws IOException exception throws if there is any IO error during dequeue operation.
	 * 如果在dequeue操作中有任何IO错误，则异常抛出。
	 */
	public byte[] dequeue() throws IOException;

    /**
     * Retrieves a Future which will complete if new Items where enqued.
	 * 检索Future，如果新项目被填入，将会完成。
     *
     * Use this method to retrieve a future where to register as Listener instead of repeatedly polling the queues state.
	 * 使用该方法来检索将来作为侦听器注册的Future，而不是重复轮询队列状态。
     * On complete this future contains the result of the dequeue operation. Hence the item was automatically removed from the queue.
	 * 完整的这个Future包含了dequeue操作的结果。因此，该项被自动从队列中删除。
     *
     * @return a ListenableFuture which completes with the first entry if items are ready to be dequeued.
	 * 如果项目准备好了，就可以在第一个条目中完成一个列表。
     */
    public ListenableFuture<byte[]> dequeueAsync();

	
	/**
	 * Removes all items of a queue, this will empty the queue and delete all back data files.
	 * 删除队列的所有项，这将清空队列，并删除所有的后数据文件。
	 * 
	 * @throws IOException exception throws if there is any IO error during dequeue operation.
	 * 如果在dequeue操作中有任何IO错误，则异常抛出。
	 */
	public void removeAll() throws IOException;
	
	/**
	 * Retrieves the item at the front of a queue
	 * 在队列的前面检索项目
	 * 
	 * @return data at the front of a queue 队列前端的数据
	 * @throws IOException exception throws if there is any IO error during peek operation.
	 * 如果在peek操作中有任何IO错误，则异常抛出异常。
	 */
	public byte[] peek()  throws IOException;


    /**
     * Retrieves the item at the front of a queue asynchronously.
	 * 以异步方式检索队列前端的项。
     * On complete the value set in this future is the result of the peek operation. Hence the item remains at the front of the list.
	 * 在这个未来的值集里，是peek操作的结果。因此，该项目仍然处于列表的最前面。
     *
     * @return a future containing the first item if available. You may register as listener at this future to be informed if a new item arrives.
	 * 如果有的话，一个包含第一个项目的未来。您可以在将来注册为侦听器，以便在新项目到达时被告知。
     */
    public ListenableFuture<byte[]> peekAsync();

    /**
     * apply an implementation of a ItemIterator interface for each queue item
	 * 为每个队列项应用ItemIterator接口的实现。
	 *
     *
     * @param iterator
     * @throws IOException
     */
    public void applyForEach(ItemIterator iterator) throws IOException;
	
	/**
	 * Delete all used data files to free disk space.
	 * 删除所有使用的数据文件来释放磁盘空间。
	 * 
	 * BigQueue will persist enqueued data in disk files, these data files will remain even after
	 * the data in them has been dequeued later, so your application is responsible to periodically call
	 * this method to delete all used data files and free disk space.
	 * BigQueue将在磁盘文件中保存排队的数据，这些数据文件将保留在以后的数据被删除之后，
	 * 因此您的应用程序负责定期调用该方法来删除所有已使用的数据文件和空闲磁盘空间。
	 * 
	 * @throws IOException exception throws if there is any IO error during gc operation.
	 * 如果在gc操作期间有任何IO错误，则异常抛出异常。
	 */
	public void gc() throws IOException;
	
	/**
	 * Force to persist current state of the queue,
	 * 强制保持队列的当前状态，
	 * 
	 * normally, you don't need to flush explicitly since:
	 * 通常情况下，您不需要显式地刷新:
	 * 1.) BigQueue will automatically flush a cached page when it is replaced out,
	 * 1)。BigQueue将自动刷新缓存页面，当它被替换时，
	 * 2.) BigQueue uses memory mapped file technology internally, and the OS will flush the changes even your process crashes,
	 * call this periodically only if you need transactional reliability and you are aware of the cost to performance.
	 * 2)。BigQueue在内部使用内存映射文件技术，并且操作系统将刷新这些更改，
	 * 即使您的进程崩溃，只要您需要事务的可靠性，并且您知道性能的成本，就会定期调用此操作。
	 *
	 *
	 */
	public void flush();
	
	/**
	 * Total number of items available in the queue.
	 * 队列中可用的项目总数。
	 * @return total number
	 */
	public long size();
	
	/**
	 * Item iterator interface
	 * 项目迭代器接口
	 */
	public static interface ItemIterator {
        /**
         * Method to be executed for each queue item
		 * 为每个队列项执行的方法
         *
         * @param item queue item
         * @throws IOException
         */
        public void forEach(byte[] item) throws IOException;
    }
}
