package com.swcares.sample.web;

import com.swcares.sample.entity.SampleUserEntity;
import com.swcares.sample.service.SampleUserService;
import com.swcares.sample.vo.SampleUserVO;
import org.wymix.framework.web.api.BaseController;
import org.wymix.framework.web.api.impl.AbstractBaseController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("sampleUser")
public class SampleUserController extends AbstractBaseController<SampleUserEntity, SampleUserVO,Long, SampleUserService> implements BaseController<SampleUserEntity,SampleUserVO,Long> {


    public SampleUserController(SampleUserService service) {
        super(service);
    }

}
