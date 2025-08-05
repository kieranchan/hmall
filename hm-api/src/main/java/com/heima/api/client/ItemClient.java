package com.heima.api.client;

import com.heima.api.config.DefaultFeignConfig;
import com.heima.api.domain.dto.ItemDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

//@FeignClient(value = "item-service", configuration = DefaultFeignConfig.class)
@FeignClient(value = "item-service")
public interface ItemClient {

    @GetMapping("/items")
    List<ItemDTO> queryItemByIds(@RequestParam("ids") Collection<Long> ids);
    
    
}