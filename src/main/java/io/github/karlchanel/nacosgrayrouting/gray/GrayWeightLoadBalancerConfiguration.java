package com.example.byterunh2.gray;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * @author jasper
 * @email jaspersteelxx@gmail.com
 * @create 2023-10-30 20:57
 **/
//@Configuration
public class GrayWeightLoadBalancerConfiguration {
    @Bean
    public ReactorLoadBalancer<ServiceInstance> weightLoadBalancer(Environment environment, ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider) {
        String name = environment.getProperty("loadbalancer.client.name");
        return new VersionGrayWeightLoadBalancer(serviceInstanceListSupplierProvider, name);
    }
}