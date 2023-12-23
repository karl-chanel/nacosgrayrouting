package io.github.karlchanel.nacosgrayrouting.gray;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
@Configuration
@LoadBalancerClients(defaultConfiguration = GrayWeightLoadBalancerConfiguration.class)
public class GrayWeightLoadBalancerConfiguration {
    @Bean
    public ReactorLoadBalancer<ServiceInstance> weightLoadBalancer(Environment environment, ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider) {
        String name = environment.getProperty("loadbalancer.client.name");
        return new VersionGrayWeightLoadBalancer(serviceInstanceListSupplierProvider, name);
    }
}