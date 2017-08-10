package com.leansoft.bigqueue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.leansoft.bigqueue.page.IMappedPage;
import com.leansoft.bigqueue.page.IMappedPageFactory;
import com.leansoft.bigqueue.page.MappedPageFactoryImpl;


/**
 * A big, fast and persistent queue implementation. 一个大型、快速且持久的队列实现。
 * Main features: 主要特点:
 * 1. FAST : close to the speed of direct memory access, both enqueue and dequeue are close to O(1) memory access.
 * 1.快:接近直接内存访问的速度，队列和dequeue都接近O(1)内存访问。
 * 2. MEMORY-EFFICIENT : automatic paging & swapping algorithm, only most-recently accessed data is kept in memory.
 * 2。内存效率:自动分页和交换算法，只有最近才访问的数据被保存在内存中。
 * 3. THREAD-SAFE : multiple threads can concurrently enqueue and dequeue without data corruption.
 * 3。线程安全:多个线程可以并发地队列和取消队列，而不需要数据损坏。
 * 4. PERSISTENT - all data in queue is persisted on disk, and is crash resistant.
 * 4。持久性——队列中的所有数据都保存在磁盘上，并且具有抗崩溃性。
 * 5. BIG(HUGE) - the total size of the queued data is only limited by the available disk space.
 * 5。大(大)-队列数据的总大小仅受可用磁盘空间的限制。
 *
 */
public class BigQueueImpl implements IBigQueue {

    final IBigArray innerArray;

    // 2 ^ 3 = 8
    final static int QUEUE_FRONT_INDEX_ITEM_LENGTH_BITS = 3;
    // size in bytes of queue front index page 队列前索引页的字节数
    final static int QUEUE_FRONT_INDEX_PAGE_SIZE = 1 << QUEUE_FRONT_INDEX_ITEM_LENGTH_BITS;
    // only use the first page 只使用第一个页面
    static final long QUEUE_FRONT_PAGE_INDEX = 0;

    // folder name for queue front index page 队列前端索引页的文件夹名称
    final static String QUEUE_FRONT_INDEX_PAGE_FOLDER = "front_index";

    // front index of the big queue, 大队列的前索引，
    final AtomicLong queueFrontIndex = new AtomicLong();

    // factory for queue front index page management(acquire, release, cache) 用于队列前端索引页面管理的工厂(获取、释放、缓存)
    IMappedPageFactory queueFrontIndexPageFactory;

    // locks for queue front write management 队列前端写管理锁
    final Lock queueFrontWriteLock = new ReentrantLock();

    // lock for dequeueFuture access
    private final Object futureLock = new Object();
    private SettableFuture<byte[]> dequeueFuture;
    private SettableFuture<byte[]> peekFuture;

    /**
     * A big, fast and persistent queue implementation, 一个大的、快速的、持久的队列实现，
     * use default back data page size, see {@link BigArrayImpl#DEFAULT_DATA_PAGE_SIZE}
     * 使用默认的返回数据页大小
     *
     * @param queueDir  the directory to store queue data 存储队列数据的目录
     * @param queueName the name of the queue, will be appended as last part of the queue directory
     *                  队列的名称将被追加到队列目录的最后一部分
     * @throws IOException exception throws if there is any IO error during queue initialization
     *                  如果在队列初始化期间存在IO错误，则异常抛出异常
     */
    public BigQueueImpl(String queueDir, String queueName) throws IOException {
        this(queueDir, queueName, BigArrayImpl.DEFAULT_DATA_PAGE_SIZE);
    }

    /**
     * A big, fast and persistent queue implementation.
     * 一个大型、快速且持久的队列实现。
     *
     * @param queueDir  the directory to store queue data
     *                  存储队列数据的目录
     * @param queueName the name of the queue, will be appended as last part of the queue directory
     *                  队列的名称将被追加到队列目录的最后一部分
     * @param pageSize  the back data file size per page in bytes, see minimum allowed {@link BigArrayImpl#MINIMUM_DATA_PAGE_SIZE}
     *                  每个页面的后数据文件大小以字节为单位，见最小值
     * @throws IOException exception throws if there is any IO error during queue initialization
     *                  如果在队列初始化期间存在IO错误，则异常抛出异常
     */
    public BigQueueImpl(String queueDir, String queueName, int pageSize) throws IOException {
        innerArray = new BigArrayImpl(queueDir, queueName, pageSize);

        // the ttl does not matter here since queue front index page is always cached
        // ttl在这里并不重要，因为队列前端索引页面总是被缓存
        this.queueFrontIndexPageFactory = new MappedPageFactoryImpl(QUEUE_FRONT_INDEX_PAGE_SIZE,
                ((BigArrayImpl) innerArray).getArrayDirectory() + QUEUE_FRONT_INDEX_PAGE_FOLDER,
                10 * 1000/*does not matter*/);
        IMappedPage queueFrontIndexPage = this.queueFrontIndexPageFactory.acquirePage(QUEUE_FRONT_PAGE_INDEX);

        ByteBuffer queueFrontIndexBuffer = queueFrontIndexPage.getLocal(0);
        long front = queueFrontIndexBuffer.getLong();
        queueFrontIndex.set(front);
    }
    //确定队列是否为空 如果为空返回true  否则返回false
    @Override
    public boolean isEmpty() {

        return this.queueFrontIndex.get() == this.innerArray.getHeadIndex();
    }

    @Override
    public void enqueue(byte[] data) throws IOException {
        this.innerArray.append(data);

        this.completeFutures();
    }

    //检索和删除队列的前端
    @Override
    public byte[] dequeue() throws IOException {
        long queueFrontIndex = -1L;
        try {
            queueFrontWriteLock.lock();
            if (this.isEmpty()) {
                return null;
            }
            queueFrontIndex = this.queueFrontIndex.get();
            byte[] data = this.innerArray.get(queueFrontIndex);
            long nextQueueFrontIndex = queueFrontIndex;
            if (nextQueueFrontIndex == Long.MAX_VALUE) {
                nextQueueFrontIndex = 0L; // wrap
            } else {
                nextQueueFrontIndex++;
            }
            this.queueFrontIndex.set(nextQueueFrontIndex);
            // persist the queue front
            //坚持队列前面
            IMappedPage queueFrontIndexPage = this.queueFrontIndexPageFactory.acquirePage(QUEUE_FRONT_PAGE_INDEX);
            ByteBuffer queueFrontIndexBuffer = queueFrontIndexPage.getLocal(0);
            queueFrontIndexBuffer.putLong(nextQueueFrontIndex);
            queueFrontIndexPage.setDirty(true);
            return data;
        } finally {
            queueFrontWriteLock.unlock();
        }

    }

    @Override
    public ListenableFuture<byte[]> dequeueAsync() {
        this.initializeDequeueFutureIfNecessary();
        return dequeueFuture;
    }



    @Override
    public void removeAll() throws IOException {
        try {
            queueFrontWriteLock.lock();
            this.innerArray.removeAll();
            this.queueFrontIndex.set(0L);
            IMappedPage queueFrontIndexPage = this.queueFrontIndexPageFactory.acquirePage(QUEUE_FRONT_PAGE_INDEX);
            ByteBuffer queueFrontIndexBuffer = queueFrontIndexPage.getLocal(0);
            queueFrontIndexBuffer.putLong(0L);
            queueFrontIndexPage.setDirty(true);
        } finally {
            queueFrontWriteLock.unlock();
        }
    }

    @Override
    public byte[] peek() throws IOException {
        if (this.isEmpty()) {
            return null;
        }
        byte[] data = this.innerArray.get(this.queueFrontIndex.get());
        return data;
    }

    @Override
    public ListenableFuture<byte[]> peekAsync() {
        this.initializePeekFutureIfNecessary();
        return peekFuture;
    }

    /**
     * apply an implementation of a ItemIterator interface for each queue item
     * 为每个队列项应用一个ItemIterator接口的实现
     *
     * @param iterator
     * @throws IOException
     */
    @Override
    public void applyForEach(ItemIterator iterator) throws IOException {
        try {
            queueFrontWriteLock.lock();
            if (this.isEmpty()) {
                return;
            }

            long index = this.queueFrontIndex.get();
            for (long i = index; i < this.innerArray.size(); i++) {
                iterator.forEach(this.innerArray.get(i));
            }
        } finally {
            queueFrontWriteLock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        if (this.queueFrontIndexPageFactory != null) {
            this.queueFrontIndexPageFactory.releaseCachedPages();
        }


        synchronized (futureLock) {
            /* Cancel the future but don't interrupt running tasks
            because they might perform further work not refering to the queue
            取消future，但不要中断运行任务
            因为他们可能会执行更多的工作而不是引用队列
             */
            if (peekFuture != null) {
                peekFuture.cancel(false);
            }
            if (dequeueFuture != null) {
                dequeueFuture.cancel(false);
            }
        }

        this.innerArray.close();
    }

    @Override
    public void gc() throws IOException {
        long beforeIndex = this.queueFrontIndex.get();
        if (beforeIndex == 0L) { // wrap
            beforeIndex = Long.MAX_VALUE;
        } else {
            beforeIndex--;
        }
        try {
            this.innerArray.removeBeforeIndex(beforeIndex);
        } catch (IndexOutOfBoundsException ex) {
            // ignore
        }
    }

    @Override
    public void flush() {
        try {
            queueFrontWriteLock.lock();
            this.queueFrontIndexPageFactory.flush();
            this.innerArray.flush();
        } finally {
            queueFrontWriteLock.unlock();
        }

    }

    @Override
    public long size() {
        long qFront = this.queueFrontIndex.get();
        long qRear = this.innerArray.getHeadIndex();
        if (qFront <= qRear) {
            return (qRear - qFront);
        } else {
            return Long.MAX_VALUE - qFront + 1 + qRear;
        }
    }


    /**
     * Completes the dequeue future
     * 完成出列的future
     */
    private void completeFutures() {
        synchronized (futureLock) {
            if (peekFuture != null && !peekFuture.isDone()) {
                try {
                    peekFuture.set(this.peek());
                } catch (IOException e) {
                    peekFuture.setException(e);
                }
            }
            if (dequeueFuture != null && !dequeueFuture.isDone()) {
                try {
                    dequeueFuture.set(this.dequeue());
                } catch (IOException e) {
                    dequeueFuture.setException(e);
                }
            }
        }
    }

    /**
     * Initializes the futures if it's null at the moment
     * 如果现在是空，就初始化futures
     */
    private void initializeDequeueFutureIfNecessary() {
        synchronized (futureLock) {
            if (dequeueFuture == null || dequeueFuture.isDone()) {
                dequeueFuture = SettableFuture.create();
            }
            if (!this.isEmpty()) {
                try {
                    dequeueFuture.set(this.dequeue());
                } catch (IOException e) {
                    dequeueFuture.setException(e);
                }
            }
        }
    }

    /**
     * Initializes the futures if it's null at the moment
     * 如果现在是空，就初始化futures
     */
    private void initializePeekFutureIfNecessary() {
        synchronized (futureLock) {
            if (peekFuture == null || peekFuture.isDone()) {
                peekFuture = SettableFuture.create();
            }
            if (!this.isEmpty()) {
                try {
                    peekFuture.set(this.peek());
                } catch (IOException e) {
                    peekFuture.setException(e);
                }
            }
        }
    }
}
