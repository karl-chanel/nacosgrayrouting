package io.github.karlchanel.nacosgrayrouting.gray;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;


public class FeignRequestInterceptor implements RequestInterceptor {
    //    @Override
//    public void apply(RequestTemplate template) {
//        if (!Objects.isNull(GrayRequestContextHolder.getGrayTag())) {
//            template.header(GrayConstant.GRAY_HEADER, GrayRequestContextHolder.getGrayTag());
//        }
//    }
    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (Objects.nonNull(requestAttributes)) {
            HttpServletRequest request = requestAttributes.getRequest();
            String gray = request.getHeader(GrayConstant.GRAY_HEADER);
            // 如果HttpHeader中灰度标记为true，则将灰度标记放到holder中，如果需要就传递下去
            if (gray != null) {
                template.header(GrayConstant.GRAY_HEADER, gray);
            }
        }
    }

}
