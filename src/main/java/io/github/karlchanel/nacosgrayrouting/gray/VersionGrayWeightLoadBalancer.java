package io.github.karlchanel.nacosgrayrouting.gray;



import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpHeaders;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Slf4j
public class VersionGrayWeightLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;
    String serviceId;
    AtomicInteger position;
    public VersionGrayWeightLoadBalancer() {
    }

    public VersionGrayWeightLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider, String serviceId) {
        this(serviceInstanceListSupplierProvider, serviceId, new Random().nextInt(1000));
    }

    public VersionGrayWeightLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider, String serviceId, int seedPosition) {
        this.serviceId = serviceId;
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
        this.position = new AtomicInteger(seedPosition);
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = (ServiceInstanceListSupplier) this.serviceInstanceListSupplierProvider.getIfAvailable(NoopServiceInstanceListSupplier::new);
        DefaultRequest req = (DefaultRequest) request;
        RequestDataContext context = (RequestDataContext) req.getContext();
        log.warn("serviceId:{} position:{}", serviceId, position.get());
        RequestData requestData = context.getClientRequest();
        HttpHeaders headers = requestData.getHeaders();
        return supplier.get(request).next().map(list -> processInstanceResponse((List<ServiceInstance>) list, headers));
    }
    //思路是没有带version标签的instance为base版本，当请求没有携带version header时，返回base版本
    //当请求有version header时，返回version header对应的版本，如果没有对应的版本的instance存在，则返回base版本
    //无论那种选择，都会在灰度选择后，再从符合的instance中进行一次权重选择，实现先进行灰度选择，在进行权重选择
    private Response<ServiceInstance> processInstanceResponse(List<ServiceInstance> instances, HttpHeaders headers) {

        if (instances.isEmpty()) {
            return new EmptyResponse();
        } else {

            String reqVersion = headers.getFirst("version");
            if (Objects.isNull(reqVersion)) {
                List<ServiceInstance> versiond_without_header = instances.stream()
                        .filter(instance -> {
                            String s = instance.getMetadata().get("version");
                            return !Objects.isNull(s);
                        })
                        .collect(Collectors.toList());
                HashSet<ServiceInstance> all = new HashSet<>(instances);
                HashSet<ServiceInstance> versionset = new HashSet<>(versiond_without_header);
                all.removeAll(versionset);
                ArrayList<ServiceInstance> fbase = new ArrayList<>();
                fbase.addAll(all);
                if (!fbase.isEmpty()) {
                    return getServiceInstanceResponseWithWeight(fbase);
                } else {
                    return new EmptyResponse();
                }
            } else {
                List<ServiceInstance> versiond_with_header = instances.stream()
                        .filter(instance -> reqVersion.equals(instance.getMetadata().get("version")))
                        .collect(Collectors.toList());
                List<ServiceInstance> versiond_without_header_all = instances.stream()
                        .filter(instance -> {
                            String s = instance.getMetadata().get("version");
                            return !Objects.isNull(s);
                        })
                        .collect(Collectors.toList());
                HashSet<ServiceInstance> serviceInstances = new HashSet<>(instances);
                HashSet<ServiceInstance> filterdset = new HashSet<>(versiond_without_header_all);
                serviceInstances.removeAll(filterdset);
                ArrayList<ServiceInstance> base = new ArrayList<>();
                base.addAll(serviceInstances);
                if (!versiond_with_header.isEmpty()) {
                    return getServiceInstanceResponseWithWeight(versiond_with_header);
                } else {
                    if (!base.isEmpty()) {
                        return getServiceInstanceResponseWithWeight(base);
                    } else {
                        return new EmptyResponse();
                    }
                }
            }
        }
    }
   //根据version选择后再进行权重路由
    private Response<ServiceInstance> getServiceInstanceResponseWithWeight(List<ServiceInstance> instances) {
        Map<ServiceInstance, Integer> weightMap = new HashMap<>();
        for (ServiceInstance instance : instances) {
            Map<String, String> metadata = instance.getMetadata();
            String s = metadata.get("nacos.weight");
            double w = Double.parseDouble(s)*100;
            int i = (int) w;
            weightMap.put(instance, i);
        }
        WeightMeta<ServiceInstance> weightMeta = WeightRandomUtils.buildWeightMeta(weightMap);
        if (ObjectUtils.isEmpty(weightMeta)) {
            return getServiceInstanceEmptyResponse();
        }
        ServiceInstance serviceInstance = weightMeta.random();
        if (ObjectUtils.isEmpty(serviceInstance)) {
            return getServiceInstanceEmptyResponse();
        }
        Map<String, String> metadata = serviceInstance.getMetadata();
        metadata.forEach((k, v) -> log.info("metadata: {}={}", k, v));

        return new DefaultResponse(serviceInstance);
    }
    private Response<ServiceInstance> getServiceInstanceEmptyResponse() {
        log.warn("No servers available for service: " + this.serviceId);
        return new EmptyResponse();
    }

    private Response<ServiceInstance> processRibbonInstanceResponse(List<ServiceInstance> instances) {
        int pos = this.position.incrementAndGet() & Integer.MAX_VALUE;
        ServiceInstance instance = (ServiceInstance) instances.get(pos % instances.size());
        return new DefaultResponse(instance);
    }
}