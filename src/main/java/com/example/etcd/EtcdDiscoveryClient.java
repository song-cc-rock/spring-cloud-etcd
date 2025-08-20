package com.example.etcd;

import com.example.etcd.config.EtcdProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.options.GetOption;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 服务发现
 */
public class EtcdDiscoveryClient implements DiscoveryClient {

	private final Client client;
	private final String namespace;

	public EtcdDiscoveryClient(Client client, String namespace) {
		this.client = client;
		this.namespace = namespace;
	}

	@Override
	public String description() {
		return "ETCD Discovery Client";
	}

	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
		// 获取服务实例
		List<ServiceInstance> instances;
		try {
			KV kvClient = client.getKVClient();
			instances = kvClient.get(ByteSequence.from(namespace + serviceId, StandardCharsets.UTF_8), GetOption.builder().isPrefix(true).build())
					.thenApply(response -> response.getKvs().stream()
							.map(kv -> {
								try {
									return (ServiceInstance) JsonMapper.builder().build().readValue(kv.getValue().toString(StandardCharsets.UTF_8), EtcdProperties.EtcdServiceInstance.class);
								} catch (JsonProcessingException e) {
									throw new RuntimeException(e);
								}
							})
							.toList()).join();
			return instances;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public List<String> getServices() {
		// 获取命名空间所有服务 {/ns/service-id/host:port → instance metadata}
		try {
			KV kvClient = client.getKVClient();
			return kvClient.get(ByteSequence.from(namespace, StandardCharsets.UTF_8), GetOption.builder().isPrefix(true).build())
					.thenApply(response -> response.getKvs().stream()
							.map(kv -> {
								String key = kv.getKey().toString(StandardCharsets.UTF_8);
								// 截取 service-id
								return key.substring(namespace.length(), key.indexOf('/', namespace.length()));
							})
							.distinct()
							.toList()).join();
		}  catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}
}
