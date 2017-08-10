package com.leansoft.bigqueue.page;

import java.nio.ByteBuffer;

/**
 * Memory mapped page file ADT
 * 内存映射页面文件ADT
 * 
 * @author bulldog
 *
 */
public interface IMappedPage {
	
	/**
	 * Get a thread local copy of the mapped page buffer
	 * 获取映射页面缓冲区的线程本地副本
	 * 
	 * @param position start position(relative to the start position of source mapped page buffer) of the thread local buffer
	 * 					启动位置(相对于源映射页面缓冲区的起始位置的位置)
	 *
	 * @return a byte buffer with specific position as start position.
	 * 			作为起始位置的特定位置的字节缓冲区。
	 *
	 */
	ByteBuffer getLocal(int position);
	
	/**
	 * Get data from a thread local copy of the mapped page buffer
	 * 从本地映射页面缓冲区的本地副本获取数据
	 * 
	 * @param position start position(relative to the start position of source mapped page buffer) of the thread local buffer
	 *                 启动位置(相对于源映射页面缓冲区的起始位置的位置)
	 * @param length the length to fetch
	 *               获取长度
	 * @return byte data
	 * 			字节数据
	 */
	public byte[] getLocal(int position, int length);
	
	/**
	 * Check if this mapped page has been closed or not
	 * 检查这个映射的页面是否已经关闭
	 * 
	 * @return
	 */
	boolean isClosed();
	
	/**
	 * Set if the mapped page has been changed or not
	 * 设置映射的页面是否已被更改
	 * 
	 * @param dirty
	 */
	void setDirty(boolean dirty);
	
	/**
	 * The back page file name of the mapped page
	 * 映射页面的后页文件名
	 * 
	 * @return
	 */
	String getPageFile();
	
	/**
	 * The index of the mapped page
	 * 映射页面的索引
	 * 
	 * @return the index
	 */
	long getPageIndex();
	
	/**
	 * Persist any changes to disk
	 * 对磁盘进行任何更改
	 */
	public void flush();
}
