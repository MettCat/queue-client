package com.leansoft.bigqueue;

import java.io.Closeable;
import java.io.IOException;

/**
 * Append Only Big Array ADT
 * 
 * @author bulldog
 *
 */
public interface IBigArray extends Closeable {
	
	public static final long NOT_FOUND = -1;
	
	/**
	 * Append the data into the head of the array
	 * 将数据附加到数组的头部
	 * 
	 * @param data binary data to append
	 * @return appended index
	 * @throws IOException if there is any IO error
	 */
	long append(byte[] data) throws IOException;
	
	
	/**
	 * Get the data at specific index
	 * 获取特定索引中的数据
	 * 
	 * @param index valid data index
	 * @return binary data if the index is valid
	 * @throws IOException if there is any IO error
	 */
	byte[] get(long index) throws IOException;
	
	/**
	 * Get the timestamp of data at specific index,
	 * 获取特定索引中的数据时间戳，
	 * 
	 * this is the timestamp when the data was appended.
	 * 这是附加数据时的时间戳。
	 * 
	 * @param index valid data index
	 * @return timestamp when the data was appended
	 * @throws IOException if there is any IO error
	 */
	long getTimestamp(long index) throws IOException;

	/**
	 * The total number of items has been appended into the array
	 * 项目的总数量被追加到数组中
	 *
	 * @return total number
	 */
	long size();
	
	
	/**
	 * Get the back data file size per page.
	 * 获取每个页面的后数据文件大小。
	 * 
	 * @return size per page
	 */
	int getDataPageSize();
	
	/**
	 * The head of the array.
	 * 数组的头。
	 * 
	 * This is the next to append index, the index of the last appended data 
	 * is [headIndex - 1] if the array is not empty.
	 * 这是附加索引的下一个索引，最后一个附加数据的索引是头索引——如果数组不是空的。
	 * 
	 * @return an index
	 */
	long getHeadIndex();
	
	/**
	 * The tail of the array. 数组的尾
	 * 
	 * The is the index of the first appended data 这是第一个附加数据的索引
	 * 
	 * @return an index
	 */
	long getTailIndex();
	
	/**
	 * Check if the array is empty or not 检查数组是否为空
	 * 
	 * @return true if empty false otherwise 如果是空的
	 */
	boolean isEmpty();
	
	/**
	 * Check if the ring space of java long type has all been used up.
	 * 检查java long类型的环形空间是否已经耗尽。
	 * 
	 * can always assume false, if true, the world is end:)
	 * 总是可以假设是假的，如果是真的，世界就会结束:)
	 * 
	 * @return array full or not 数组是否满
	 */
	boolean isFull();
	
	/**
	 * Remove all data in this array, this will empty the array and delete all back page files.
	 * 删除这个数组中的所有数据，这将清空数组并删除所有的后页文件。
	 * 
	 */
	void removeAll() throws IOException;
	
	/**
	 * Remove all data before specific index, this will advance the array tail to index and 
	 * delete back page files before index.
	 * 在特定索引之前删除所有数据，这将在索引之前将数组尾部提前到索引并删除页面文件。
	 *
	 * @param index an index
	 * @throws IOException exception thrown if there was any IO error during the removal operation
	 * 如果在删除操作中存在IO错误，则抛出异常
	 */
	void removeBeforeIndex(long index) throws IOException;
	
	/**
	 * Remove all data before specific timestamp, this will advance the array tail and delete back page files
	 * accordingly.
	 * 在特定时间戳之前删除所有数据，这将使数组尾部提前，并相应地删除页面文件。
	 * 
	 * @param timestamp a timestamp
	 * @throws IOException exception thrown if there was any IO error during the removal operation
	 * 如果在删除操作中存在IO错误，则抛出异常
	 */
	void removeBefore(long timestamp) throws IOException;
	
	/**
	 * Force to persist newly appended data,
	 * normally, you don't need to flush explicitly since:
	 * 强制保存新添加的数据，通常情况下，您不需要显式地刷新:
	 * 1.) BigArray will automatically flush a cached page when it is replaced out,
	 * 1)。BigArray会自动刷新缓存页面，当它被替换时，
	 * 2.) BigArray uses memory mapped file technology internally, and the OS will flush the changes even your process crashes,
	 * call this periodically only if you need transactional reliability and you are aware of the cost to performance.
	 * 2)。BigArray在内部使用内存映射文件技术，而操作系统将刷新这些更改，
	 * 即使您的进程崩溃，也要在需要事务可靠性的情况下定期调用该操作，并且您知道性能的成本。
	 */
	void flush();
	
	/**
	 * Find an index closest to the specific timestamp when the corresponding item was appended
	 * 找到一个与特定时间戳相接近的索引，当相应的项被追加时
	 * 
	 * @param timestamp when the corresponding item was appended
	 *                  当相应的项目被追加时
	 * @return an index
	 * @throws IOException exception thrown if there was any IO error during the getClosestIndex operation
	 * 如果在getClosestIndex操作中出现任何IO错误，则抛出异常
	 */
	long findClosestIndex(long timestamp) throws IOException;
	
	
	/**
	 * Get total size of back files(index and data files) of the big array
	 * 获取大数组的返回文件(索引和数据文件)的总大小
	 * 
	 * @return total size of back files
	 * 返回文件的总大小
	 * @throws IOException exception thrown if there was any IO error during the getBackFileSize operation
	 * 如果在getBackFileSize操作期间出现了IO错误，则抛出异常
	 */
	long getBackFileSize() throws IOException;
	
	/**
	 * limit the back file size, truncate back file and advance array tail index accordingly,
	 * Note, this is a best effort call, exact size limit can't be guaranteed
	 * 限制后文件大小，截断文件，并相应地提前进行数组尾部索引，注意，这是一个最好的调用，精确的大小限制是不能保证的
	 * 
	 * @param sizeLimit the size to limit 大小限制
	 * @throws IOException exception thrown if there was any IO error during the limitBackFileSize operation
	 * 如果在限制返回过程中出现了IO错误，则抛出异常
	 */
	void limitBackFileSize(long sizeLimit) throws IOException;
	
	
	/**
	 * Get the data item length at specific index
	 * 获取特定索引中的数据项长度
	 * 
	 * @param index valid data index 有效数据索引
	 * @return the length of binary data if the index is valid
	 * 如果索引有效，则二进制数据的长度
	 * @throws IOException if there is any IO error
	 */
	int getItemLength(long index) throws IOException;
}
