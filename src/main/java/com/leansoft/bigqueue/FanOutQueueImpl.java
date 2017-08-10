package com.leansoft.bigqueue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.leansoft.bigqueue.page.IMappedPage;
import com.leansoft.bigqueue.page.IMappedPageFactory;
import com.leansoft.bigqueue.page.MappedPageFactoryImpl;
import com.leansoft.bigqueue.utils.FolderNameValidator;


/**
 * A big, fast and persistent queue implementation supporting fan out semantics.
 * 一个大的 快速和持久的队列实现
 *  
 * Main features:
 * 主要特点：
 * 1. FAST : close to the speed of direct memory access, both enqueue and dequeue are close to O(1) memory access.
 * 1。快:接近直接内存访问的速度，队列和dequeue都接近O(1)内存访问。
 * 2. MEMORY-EFFICIENT : automatic paging & swapping algorithm, only most-recently accessed data is kept in memory.
 * 2。内存效率:自动分页和交换算法，只有最近才访问的数据被保存在内存中。
 * 3. THREAD-SAFE : multiple threads can concurrently enqueue and dequeue without data corruption.
 * 3。线程安全:多个线程可以并发地队列和取消队列，而不需要数据损坏。
 * 4. PERSISTENT - all data in queue is persisted on disk, and is crash resistant.
 * 4。持久性——队列中的所有数据都保存在磁盘上，并且具有抗崩溃性。
 * 5. BIG(HUGE) - the total size of the queued data is only limited by the available disk space.
 * 5。大(大)-队列数据的总大小仅受可用磁盘空间的限制。
 * 6. FANOUT - support fan out semantics, multiple consumers can independently consume a single queue without intervention, 
 *                     everyone has its own queue front index.
 * 6. FANOUT-支持扇出语义，多个使用者可以独立地使用单个队列而不需要干预，每个人都有自己的队列前端索引。
 * 7. CLIENT MANAGED INDEX - support access by index and the queue index is managed at client side.
 * 7. 客户端管理索引-通过索引支持访问，并且在客户端管理队列索引。
 * 
 * @author bulldog
 *
 */
public class FanOutQueueImpl implements IFanOutQueue {
	
	final BigArrayImpl innerArray;
	
	// 2 ^ 3 = 8
	final static int QUEUE_FRONT_INDEX_ITEM_LENGTH_BITS = 3;
	// size in bytes of queue front index page
	final static int QUEUE_FRONT_INDEX_PAGE_SIZE = 1 << QUEUE_FRONT_INDEX_ITEM_LENGTH_BITS;
	// only use the first page
	static final long QUEUE_FRONT_PAGE_INDEX = 0;
	
	// folder name prefix for queue front index page
	final static String QUEUE_FRONT_INDEX_PAGE_FOLDER_PREFIX = "front_index_";
	
	final ConcurrentMap<String, QueueFront> queueFrontMap = new ConcurrentHashMap<String, QueueFront>();

	/**
	 * A big, fast and persistent queue implementation with fandout support.
	 * 使用fandout支持的大型、快速和持久的队列实现。
	 * 
	 * @param queueDir  the directory to store queue data
	 *                  存储队列数据的目录
	 * @param queueName the name of the queue, will be appended as last part of the queue directory
	 *                  队列的名称将被追加到队列目录的最后一部分
	 * @param pageSize the back data file size per page in bytes, see minimum allowed {@link BigArrayImpl#MINIMUM_DATA_PAGE_SIZE}
	 *                 每个页面的后数据文件大小以字节为单位，见最小值
	 * @throws IOException exception throws if there is any IO error during queue initialization
	 * 				   如果在队列初始化期间存在IO错误，则异常抛出异常
	 */
	public FanOutQueueImpl(String queueDir, String queueName, int pageSize)
			throws IOException {
		innerArray = new BigArrayImpl(queueDir, queueName, pageSize);
	}

	/**
     * A big, fast and persistent queue implementation with fanout support,
     * use default back data page size, see {@link BigArrayImpl#DEFAULT_DATA_PAGE_SIZE}
	 * 带有fanout支持的大型、快速和持久队列实现，使用默认的返回数据页大小，参见@link BigArrayImpl defaultdatapagesize
	 * 
	 * @param queueDir the directory to store queue data
	 *                 存储队列数据的目录
	 * @param queueName the name of the queue, will be appended as last part of the queue directory
	 *                  队列的名称将被追加到队列目录的最后一部分
	 * @throws IOException exception throws if there is any IO error during queue initialization
	 * 					如果在队列初始化期间存在IO错误，则异常抛出异常
	 */
	public FanOutQueueImpl(String queueDir, String queueName) throws IOException {
		this(queueDir, queueName, BigArrayImpl.DEFAULT_DATA_PAGE_SIZE);
	}
	
	QueueFront getQueueFront(String fanoutId) throws IOException {
		QueueFront qf = this.queueFrontMap.get(fanoutId);
		if (qf == null) { // not in cache, need to create one
			qf = new QueueFront(fanoutId);
			QueueFront found = this.queueFrontMap.putIfAbsent(fanoutId, qf);
			if (found != null) {
				qf.indexPageFactory.releaseCachedPages();
				qf = found;
			}
		}
		
		return qf;
	}

	@Override
	public boolean isEmpty(String fanoutId) throws IOException {
		try {
			this.innerArray.arrayReadLock.lock();
			
			QueueFront qf = this.getQueueFront(fanoutId);
			return qf.index.get() == innerArray.getHeadIndex();
		
		} finally {
			this.innerArray.arrayReadLock.unlock();
		}
	}
	
	@Override
	public boolean isEmpty() {
		return this.innerArray.isEmpty();
	}

	@Override
	public long enqueue(byte[] data) throws IOException {
		return innerArray.append(data);
	}

	@Override
	public byte[] dequeue(String fanoutId) throws IOException {
		try {
			this.innerArray.arrayReadLock.lock();
		
			QueueFront qf = this.getQueueFront(fanoutId);
			try {
				qf.writeLock.lock();
				
				if (qf.index.get() == innerArray.arrayHeadIndex.get()) {
					return null; // empty
				}
				
				byte[] data = innerArray.get(qf.index.get());
				qf.incrementIndex();
				
				return data;
			} catch (IndexOutOfBoundsException ex) {
				ex.printStackTrace();
				qf.resetIndex(); // maybe the back array has been truncated to limit size
				
				byte[] data = innerArray.get(qf.index.get());
				qf.incrementIndex();
				
				return data;
				
			} finally {
				qf.writeLock.unlock();
			}
			
		} finally {
			this.innerArray.arrayReadLock.unlock();
		}
	}

	@Override
	public byte[] peek(String fanoutId) throws IOException {
		try {
			this.innerArray.arrayReadLock.lock();
		
			QueueFront qf = this.getQueueFront(fanoutId);
			if (qf.index.get() == innerArray.getHeadIndex()) {
				return null; // empty
			}
			
			return innerArray.get(qf.index.get());
		
		} finally {
			this.innerArray.arrayReadLock.unlock();
		}
	}

	@Override
	public int peekLength(String fanoutId) throws IOException {
		try {
			this.innerArray.arrayReadLock.lock();
		
			QueueFront qf = this.getQueueFront(fanoutId);
			if (qf.index.get() == innerArray.getHeadIndex()) {
				return -1; // empty
			}
			return innerArray.getItemLength(qf.index.get());
		
		} finally {
			this.innerArray.arrayReadLock.unlock();
		}
	}
	
	@Override
	public long peekTimestamp(String fanoutId) throws IOException {
		try {
			this.innerArray.arrayReadLock.lock();
			
			QueueFront qf = this.getQueueFront(fanoutId);
			if (qf.index.get() == innerArray.getHeadIndex()) {
				return -1; // empty
			}
			return innerArray.getTimestamp(qf.index.get());
		
		} finally {
			this.innerArray.arrayReadLock.unlock();
		}
	}
	

	@Override
	public byte[] get(long index) throws IOException {
		return this.innerArray.get(index);
	}

	@Override
	public int getLength(long index) throws IOException {
		return this.innerArray.getItemLength(index);
	}

	@Override
	public long getTimestamp(long index) throws IOException {
		return this.innerArray.getTimestamp(index);
	}

	@Override
	public void removeBefore(long timestamp) throws IOException {
		try {
			this.innerArray.arrayWriteLock.lock();
			
			this.innerArray.removeBefore(timestamp);
			for(QueueFront qf : this.queueFrontMap.values()) {
				try {
					qf.writeLock.lock();
					qf.validateAndAdjustIndex();	
				} finally {
					qf.writeLock.unlock();
				}
			}
		} finally {
			this.innerArray.arrayWriteLock.unlock();
		}
	}

	@Override
	public void limitBackFileSize(long sizeLimit) throws IOException {
		try {
			this.innerArray.arrayWriteLock.lock();
			
			this.innerArray.limitBackFileSize(sizeLimit);
			
			for(QueueFront qf : this.queueFrontMap.values()) {
				try {
					qf.writeLock.lock();
					qf.validateAndAdjustIndex();	
				} finally {
					qf.writeLock.unlock();
				}
			}
		
		} finally {
			this.innerArray.arrayWriteLock.unlock();
		}
	}

	@Override
	public long getBackFileSize() throws IOException {
		return this.innerArray.getBackFileSize();
	}

	@Override
	public long findClosestIndex(long timestamp) throws IOException {
		try {
			this.innerArray.arrayReadLock.lock();
			
			if (timestamp == LATEST) {
				return this.innerArray.getHeadIndex();
			}
			if (timestamp == EARLIEST) {
				return this.innerArray.getTailIndex();
			}
			return this.innerArray.findClosestIndex(timestamp);
		
		} finally {
			this.innerArray.arrayReadLock.unlock();
		}
	}

	@Override
	public void resetQueueFrontIndex(String fanoutId, long index) throws IOException {
		try {
			this.innerArray.arrayReadLock.lock();
		
			QueueFront qf = this.getQueueFront(fanoutId);
			
			try {
				qf.writeLock.lock();
				
				if (index != this.innerArray.getHeadIndex()) { // ok to set index to array head index
					this.innerArray.validateIndex(index);
				}
				
				qf.index.set(index);
				qf.persistIndex();
				
			} finally {
				qf.writeLock.unlock();
			}
		
		} finally {
			this.innerArray.arrayReadLock.unlock();
		}
	}

	@Override
	public long size(String fanoutId) throws IOException {
		try {
			this.innerArray.arrayReadLock.lock();
			
			QueueFront qf = this.getQueueFront(fanoutId);
			long qFront = qf.index.get();
			long qRear = innerArray.getHeadIndex();
			if (qFront <= qRear) {
				return (qRear - qFront);
			} else {
				return Long.MAX_VALUE - qFront + 1 + qRear;
			}
			
		} finally {
			this.innerArray.arrayReadLock.unlock();
		}
	}
	
	@Override
	public long size() {
		return this.innerArray.size();
	}
	
	@Override
	public void flush() {
		try {
			this.innerArray.arrayReadLock.lock();
			
			for(QueueFront qf : this.queueFrontMap.values()) {
				try {
					qf.writeLock.lock();
					qf.indexPageFactory.flush();		
				} finally {
					qf.writeLock.unlock();
				}
			}
			innerArray.flush();
			
		} finally {
			this.innerArray.arrayReadLock.unlock();
		}
	}

	@Override
	public void close() throws IOException {
		try {
			this.innerArray.arrayWriteLock.lock();
			
			for(QueueFront qf : this.queueFrontMap.values()) {
				qf.indexPageFactory.releaseCachedPages();
			}
			
			innerArray.close();
		} finally {
			this.innerArray.arrayWriteLock.unlock();
		}
	}
	
	@Override
	public void removeAll() throws IOException {
		try {
			this.innerArray.arrayWriteLock.lock();
			
			for(QueueFront qf : this.queueFrontMap.values()) {
				try {
					qf.writeLock.lock();
					qf.index.set(0L);
					qf.persistIndex();
				} finally {
					qf.writeLock.unlock();
				}
			}
			innerArray.removeAll();
		
		} finally {
			this.innerArray.arrayWriteLock.unlock();
		}
	}
	
	// Queue front wrapper 队列前面包装
	class QueueFront {
		
		// fanout index  扇出指数
		final String fanoutId;
		
		// front index of the fanout queue 扇形队列的前索引
		final AtomicLong index = new AtomicLong();
		
		// factory for queue front index page management(acquire, release, cache)
		//用于队列前端索引页面管理的工厂(获取、释放、缓存)
		final IMappedPageFactory indexPageFactory;
		
		// lock for queue front write management
		//队列前端写管理锁定
		final Lock writeLock = new ReentrantLock();
		
		QueueFront(String fanoutId) throws IOException {
			try {
				FolderNameValidator.validate(fanoutId);
			} catch (IllegalArgumentException ex) {
				throw new IllegalArgumentException("invalid fanout identifier", ex);
			}
			this.fanoutId = fanoutId;
			// the ttl does not matter here since queue front index page is always cached
			//ttl在这里并不重要，因为队列前端索引页面总是被缓存
			this.indexPageFactory = new MappedPageFactoryImpl(QUEUE_FRONT_INDEX_PAGE_SIZE, 
					innerArray.arrayDirectory + QUEUE_FRONT_INDEX_PAGE_FOLDER_PREFIX + fanoutId, 
					10 * 1000/*does not matter*/);
			
			IMappedPage indexPage = this.indexPageFactory.acquirePage(QUEUE_FRONT_PAGE_INDEX);
			
			ByteBuffer indexBuffer = indexPage.getLocal(0);
			index.set(indexBuffer.getLong());
			validateAndAdjustIndex();
		}
		
		void validateAndAdjustIndex() throws IOException {
			if (index.get() != innerArray.arrayHeadIndex.get()) { // ok that index is equal to array head index 好了，这个索引等于数组头索引
				try {
					innerArray.validateIndex(index.get());
				} catch (IndexOutOfBoundsException ex) { // maybe the back array has been truncated to limit size 也许后面的数组被截断了，以限制大小
					resetIndex();
				}
		   }
		}
		
		// reset queue front index to the tail of array 将队列前索引重置为数组的尾部
		void resetIndex() throws IOException {
			index.set(innerArray.arrayTailIndex.get());
			
			this.persistIndex();
		}
		
		void incrementIndex() throws IOException {
			long nextIndex = index.get();
			if (nextIndex == Long.MAX_VALUE) {
				nextIndex = 0L; // wrap
			} else {
				nextIndex++;
			}
			index.set(nextIndex);
			
			this.persistIndex();
		}
		
		void persistIndex() throws IOException {
			// persist index
			IMappedPage indexPage = this.indexPageFactory.acquirePage(QUEUE_FRONT_PAGE_INDEX);
			ByteBuffer indexBuffer = indexPage.getLocal(0);
			indexBuffer.putLong(index.get());
			indexPage.setDirty(true);
		}
	}

	@Override
	public long getFrontIndex() {
		return this.innerArray.getTailIndex();
	}

	@Override
	public long getRearIndex() {
		return this.innerArray.getHeadIndex();
	}

	@Override
	public long getFrontIndex(String fanoutId) throws IOException {
		try {
			this.innerArray.arrayReadLock.lock();
		
			QueueFront qf = this.getQueueFront(fanoutId);
			return qf.index.get();
		
		} finally {
			this.innerArray.arrayReadLock.unlock();
		}
	}
}
