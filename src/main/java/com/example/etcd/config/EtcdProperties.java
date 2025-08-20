package com.example.etcd.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.serviceregistry.Registration;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "spring.cloud.etcd")
public class EtcdProperties {

	/**
	 * 是否启用etcd注册配置
	 */
	private boolean enabled;

	/**
	 * 注册中心配置
	 */
	private EtcdRegistry registry;

	/**
	 * 服务实例配置
	 */
	private EtcdServiceInstance instance;

	@Data
	public static class EtcdRegistry{

		/**
		 * 节点地址
		 */
		private List<String> endpoints;
		/**
		 * 服务名
		 */
		private String namespace;
		/**
		 * 租约
		 */
		private long ttl;
	}

	@Data
	public static class EtcdServiceInstance implements Registration {

		/**
		 * 服务ID
		 */
		private String serviceId;
		/**
		 * IP
		 */
		private String ip;
		/**
		 * 端口
		 */
		private int port;
		/**
		 * 元数据信息
		 */
		private Map<String, String> metadata;


		@Override
		public String getHost() {
			return ip;
		}

		@Override
		public boolean isSecure() {
			// 支持TLS, 返回true
			return false;
		}

		@Override
		public URI getUri() {
			return URI.create((isSecure() ? "https" : "http") + "://" + ip + ":" + port);
		}
	}
}
