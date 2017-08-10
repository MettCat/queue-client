package com.leansoft.bigqueue.page;

import java.io.IOException;
import java.util.Set;

/**
 * Memory mapped page management ADT
 * 内存映射页面管理ADT
 * 
 * @author bulldog
 *
 */
public interface IMappedPageFactory {
	
	/**
	 * Acquire a mapped page with specific index from the factory
	 * 从工厂获得一个带有特定索引的映射页面
	 * 
	 * @param index the index of the page
	 *              页面的索引
	 * @return a mapped page
	 * 			一个映射的页面
	 * @throws IOException exception thrown if there was any IO error during the acquire operation
	 * 			如果在获取操作期间存在IO错误，则抛出异常
	 */
	IMappedPage acquirePage(long index) throws IOException;
	
	/**
	 * Return the mapped page to the factory,
	 * 将映射的页面转到factory
	 *
	 * calling thread release the page to inform the factory that it has finished with the page,
	 * 调用线程释放页面，告知工厂已经完成了该页面
	 *
	 * so the factory get a chance to recycle the page to save memory.
	 * 
	 * @param index the index of the page 页面的索引
	 */
	void releasePage(long index);
	
	/**
	 * Current set page size, when creating pages, the factory will
	 * only create pages with this size.
	 * 当前的设置页面大小，在创建页面时，factory只会创建具有这个大小的页面。
	 * 
	 * @return an integer number
	 * 返回一个整型数据
	 */
	int getPageSize();
	
	/**
	 * Current set page directory.
	 * 当前目录设置页面。
	 * 
	 * @return
	 */
	String getPageDir();
	
	/**
	 * delete a mapped page with specific index in this factory,
	 * this call will remove the page from the cache if it is cached and
	 * delete back file.
	 * 在这个工厂中删除带有特定索引的映射页面，如果缓存和删除文件，这个调用将从缓存中删除该页面。
	 * 
	 * @param index the index of the page  页面的索引
	 * @throws IOException exception thrown if there was any IO error during the delete operation.
	 * 如果在删除操作中出现了IO错误，则抛出异常。
	 */
	void deletePage(long index) throws IOException;
	
	/**
	 * delete mapped pages with a set of specific indexes in this factory,
	 * this call will remove the pages from the cache if they ware cached and
	 * delete back files.
	 * 在这个工厂中使用一组特定的索引删除映射的页面，这个调用将从缓存中删除页面，如果它们被缓存并删除了文件。
	 * 
	 * @param indexes the indexes of the pages  页面的索引
	 * @throws IOException
	 */
	void deletePages(Set<Long> indexes) throws IOException;
	
	/**
	 * delete all mapped pages currently available in this factory,
	 * this call will remove all pages from the cache and delete all back files.
	 * 删除该工厂当前可用的所有映射页面，该调用将删除缓存中的所有页面，并删除所有返回的文件。
	 * 
	 * @throws IOException exception thrown if there was any IO error during the delete operation.
	 * 如果在删除操作中出现了IO错误，则抛出异常。
	 */
	void deleteAllPages() throws IOException;
	
	/**
	 * remove all cached pages from the cache and close resources associated with the cached pages.
	 * 从缓存中删除所有缓存页面，并关闭与缓存页面相关联的资源。
	 * 
	 * @throws IOException exception thrown if there was any IO error during the release operation.
	 * 如果在发布操作期间出现了IO错误，则抛出异常。
	 */
	void releaseCachedPages() throws IOException;
	
	/**
	 * Get all indexes of pages with last modified timestamp before the specific timestamp.
	 * 在特定的时间戳之前，获取带有最后修改时间戳的所有页面的索引。
	 * 
	 * @param timestamp the timestamp to check 时间戳检查
	 * @return a set of indexes 一组索引
	 */
	Set<Long> getPageIndexSetBefore(long timestamp);
	
	/**
	 * Delete all pages with last modified timestamp before the specific timestamp.
	 * 在特定时间戳之前删除所有带有最后修改时间戳的页面。
	 * 
	 * @param timestamp the timestamp to check 时间戳检查
	 * @throws IOException exception thrown if there was any IO error during the delete operation.
	 * 如果在删除操作中出现了IO错误，则抛出异常。
	 */
	void deletePagesBefore(long timestamp) throws IOException;

    /**
     * Delete all pages before the specific index
	 * 删除特定索引之前的所有页面
     *
     * @param pageIndex page file index to check 页面文件索引检查
     * @throws IOException exception thrown if there was any IO error during the delete operation.
	 * 如果在删除操作中出现了IO错误，则抛出异常。
     */
    void deletePagesBeforePageIndex(long pageIndex) throws IOException;

	/**
	 * Get last modified timestamp of page file index
	 * 获取页面文件索引的最后修改时间戳
	 * 
	 * @param index page index
	 *              页面索引
	 */
	long getPageFileLastModifiedTime(long index);
	
	/**
	 * Get index of a page file with last modified timestamp closest to specific timestamp.
	 * 获取一个页面文件的索引，使用最后修改的时间戳，最接近于特定的时间戳。
	 * 
	 * @param timestamp the timestamp to check 时间戳检查
	 * @return a page index 一个页面索引
	 */
	long getFirstPageIndexBefore(long timestamp);
	
	/**
	 * For test, get a list of indexes of current existing back files.
	 * 对于测试，获取当前存在的后文件的索引列表。
	 * 
	 * @return a set of indexes
	 */
	Set<Long> getExistingBackFileIndexSet();
	
	/**
	 * For test, get current cache size
	 * 对于测试，获取当前缓存大小
	 * 
	 * @return an integer number
	 */
	int getCacheSize();
	
	/**
	 * Persist any changes in cached mapped pages
	 * 持久化缓存映射页面中的任何更改
	 */
	void flush();
	
	/**
	 * 
	 * A set of back page file names
	 * 一组后页文件名
	 * 
	 * @return file name set 文件名的集合
	 */
	Set<String> getBackPageFileSet();
	
	
	/**
	 * Total size of all page files
	 * 所有页面文件的大小
	 * 
	 * @return total size
	 */
	long getBackPageFileSize();
	
}
