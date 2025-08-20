package com.example.etcd;

import com.example.etcd.config.EtcdProperties;
import io.etcd.jetcd.Client;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(EtcdProperties.class)
public class EtcdAutoConfiguration {


	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.cloud.etcd.enabled", havingValue = "true")
	public EtcdServiceRegistry etcdServiceRegistry(EtcdProperties etcdProperties) {
		EtcdServiceRegistry registry = new EtcdServiceRegistry(etcdProperties);
		EtcdProperties.EtcdServiceInstance instance = etcdProperties.getInstance();
		registry.register(instance);
		return registry;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.cloud.etcd.enabled", havingValue = "true")
	public EtcdDiscoveryClient etcdDiscoveryClient(EtcdProperties etcdProperties) {
		Client client = Client.builder().endpoints(etcdProperties.getRegistry().getEndpoints().toArray(new String[0])).build();
		return new EtcdDiscoveryClient(client, etcdProperties.getRegistry().getNamespace());
	}
}
