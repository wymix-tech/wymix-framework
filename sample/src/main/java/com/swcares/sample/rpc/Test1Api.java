package com.swcares.sample.rpc;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "test1")
public interface Test1Api {
}
