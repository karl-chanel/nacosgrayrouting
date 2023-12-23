package io.github.karlchanel.nacosgrayrouting.gray;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.Objects;

/**
 * @author jasper
 * @email jaspersteelxx@gmail.com
 * @create 2023-12-24 04:32
 **/
public class RestTemplateInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (Objects.nonNull(requestAttributes)) {
            HttpServletRequest httpServletRequest = requestAttributes.getRequest();
            String gray = httpServletRequest.getHeader(GrayConstant.GRAY_HEADER);
            // 如果HttpHeader中灰度标记为true，则将灰度标记放到holder中，如果需要就传递下去
            if (gray != null) {
                request.getHeaders().add(GrayConstant.GRAY_HEADER, gray);
            }
        }
        ClientHttpResponse response = execution.execute(request, body);
        // 在返回之前也可以做一些其他的操作，如cookie管理。关于手动管理cookie，后面也会介绍
        return response;
    }
}
