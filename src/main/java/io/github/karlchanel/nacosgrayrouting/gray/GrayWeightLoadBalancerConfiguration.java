package io.github.karlchanel.nacosgrayrouting.gray;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

@Configuration
@LoadBalancerClients(defaultConfiguration = GrayWeightLoadBalancerConfiguration.class)
public class GrayWeightLoadBalancerConfiguration {
    @Bean(name = "grayWeightLoadBalancer")
    @LoadBalanced
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // 设置restTemplate的interceptor属性
        restTemplate.getInterceptors().add(restTemplateInterceptor());
        return restTemplate;
    }

    @Bean
    public FeignRequestInterceptor feignRequestInterceptor() {
        return new FeignRequestInterceptor();
    }

    @Bean
    public RestTemplateInterceptor restTemplateInterceptor() {
        return new RestTemplateInterceptor();
    }

    // 这里使用自定义的负载均衡策略
    @Bean
    public ReactorLoadBalancer<ServiceInstance> weightLoadBalancer(Environment environment, ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider) {
        String name = environment.getProperty("loadbalancer.client.name");
        return new VersionGrayWeightLoadBalancer(serviceInstanceListSupplierProvider, name);
    }
}