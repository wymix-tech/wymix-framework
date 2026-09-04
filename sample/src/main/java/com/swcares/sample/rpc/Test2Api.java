package com.swcares.sample.rpc;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "test2")
public interface Test2Api {
}
