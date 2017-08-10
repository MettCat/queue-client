package com.leansoft.thriftqueue.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.thrift.TException;

import com.leansoft.bigqueue.BigQueueImpl;
import com.leansoft.bigqueue.IBigQueue;
import com.leansoft.bigqueue.thrift.BigQueueService;
import com.leansoft.bigqueue.thrift.QueueRequest;
import com.leansoft.bigqueue.thrift.QueueResponse;
import com.leansoft.bigqueue.thrift.ResultCode;

/**
 * Big queue service thrift implementation
 * 
 * @author bulldog
 *
 */
public class ThriftQueueServiceImpl implements BigQueueService.Iface {
	
	
	// topic to queue map
	private Map<String, IBigQueue> queueMap;
	private Object lock = new Object();
	
	private String queueDir;
	
	public ThriftQueueServiceImpl(String queueDir) throws IOException {
		this.queueDir = queueDir;
		this.queueMap = new HashMap<String, IBigQueue>();
	}

	@Override
	public QueueResponse enqueue(String topic, QueueRequest req) throws TException {
		QueueResponse resp = new QueueResponse();
		if (topic == null) {
			resp.setResultCode(ResultCode.FAILURE);
			return resp;
		}
		IBigQueue bigQueue = queueMap.get(topic);
		if (bigQueue == null) {
			synchronized(lock) {
				bigQueue = queueMap.get(topic);
				if (bigQueue == null) {
					try {
						bigQueue = new BigQueueImpl(queueDir, topic);
						queueMap.put(topic, bigQueue);
					} catch (IOException e) {
						resp.setResultCode(ResultCode.FAILURE);
						return resp;
					}
				}
			}
		}
		
		if (req.getData() != null && req.getData().length > 0) {
			try {
				bigQueue.enqueue(req.getData());
			} catch (IOException e) {
				resp.setResultCode(ResultCode.FAILURE);
				return resp;
			}
			resp.setResultCode(ResultCode.SUCCESS);
			return resp;
		} else {
			resp.setResultCode(ResultCode.FAILURE);
			return resp;
		}
	}

	@Override
	public QueueResponse dequeue(String topic) throws TException {
		QueueResponse resp = new QueueResponse();
		byte[] data = null;
		IBigQueue bigQueue = queueMap.get(topic);
		if (bigQueue != null) {
			try {
				data = bigQueue.dequeue();
			} catch (IOException e) {
				resp.setResultCode(ResultCode.FAILURE);
				return resp;
			}
		}
		resp.setResultCode(ResultCode.SUCCESS);
		resp.setData(data);
		return resp;
	}

	@Override
	public QueueResponse peek(String topic) throws TException {
		QueueResponse resp = new QueueResponse();
		byte[] data = null;
		IBigQueue bigQueue = queueMap.get(topic);
		if (bigQueue != null) {
			try {
				data = bigQueue.peek();
			} catch (IOException e) {
				resp.setResultCode(ResultCode.FAILURE);
				return resp;
			}
		}
		resp.setResultCode(ResultCode.SUCCESS);
		resp.setData(data);
		return resp;
	}

	@Override
	public long getSize(String topic) throws TException {
		IBigQueue bigQueue = queueMap.get(topic);
		if (bigQueue != null) return bigQueue.size();
		return 0L;
	}

	@Override
	public boolean isEmpty(String topic) throws TException {
		IBigQueue bigQueue = queueMap.get(topic);
		if (bigQueue != null) return bigQueue.isEmpty();
		return true;
	}

}
