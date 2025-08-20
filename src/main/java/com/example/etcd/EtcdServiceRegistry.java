package com.example.etcd;

import com.example.etcd.config.EtcdProperties;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.options.PutOption;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 服务注册
 */
public class EtcdServiceRegistry implements ServiceRegistry<EtcdProperties.EtcdServiceInstance> {

	private final EtcdProperties properties;
	private final Client client;
	private final KV kvClient;
	private final Lease leaseClient;
	private long leaseId;
	private final ScheduledExecutorService scheduler;


	public EtcdServiceRegistry(EtcdProperties properties) {
		this.properties = properties;
		this.client = Client.builder()
				.endpoints(properties.getRegistry().getEndpoints().toArray(new String[0]))
				.build();
		this.kvClient = client.getKVClient();
		this.leaseClient = client.getLeaseClient();
		// 创建一个守护线程池，用于定时续租
		scheduler = new ScheduledThreadPoolExecutor(2, new ThreadFactory() {
			final AtomicInteger threadCount = new AtomicInteger(1);
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r, "etcd-lease-" + threadCount.getAndIncrement());
				thread.setDaemon(true);
				return thread;
			}
		});
	}

	@Override
	public void register(EtcdProperties.EtcdServiceInstance instance) {
		try {
			// get lease
			leaseId = leaseClient.grant(properties.getRegistry().getTtl()).get().getID();
			/*
			 * /ns/service-id/host:port → instance metadata
			 */
			String key = "/" + properties.getRegistry().getNamespace() + "/" + instance.getServiceId() + "/" + instance.getHost() + ":" + instance.getPort();
			String value = JsonMapper.builder().build().writeValueAsString(instance);
			kvClient.put(ByteSequence.from(key, StandardCharsets.UTF_8), ByteSequence.from(value, StandardCharsets.UTF_8),
					PutOption.builder().withLeaseId(leaseId).build()).get();
			// keep alive
			scheduler.scheduleAtFixedRate(() -> leaseClient.keepAliveOnce(leaseId), 0, properties.getRegistry().getTtl() / 5, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void deregister(EtcdProperties.EtcdServiceInstance instance) {
		try {
			String key = "/" + properties.getRegistry().getNamespace() + "/" + instance.getServiceId() + "/" + instance.getHost() + ":" + instance.getPort();
			kvClient.delete(ByteSequence.from(key, StandardCharsets.UTF_8)).get();
			leaseClient.revoke(leaseId).get();
			scheduler.shutdown();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() {
		client.close();
		scheduler.shutdown();
	}

	@Override
	public void setStatus(EtcdProperties.EtcdServiceInstance registration, String status) {

	}

	@Override
	public <T> T getStatus(EtcdProperties.EtcdServiceInstance registration) {
		return null;
	}
}
