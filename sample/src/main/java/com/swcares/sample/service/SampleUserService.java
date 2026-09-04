package com.swcares.sample.service;

import com.swcares.sample.dao.SampleUserDao;
import com.swcares.sample.entity.SampleUserEntity;
import com.travelsky.base.core.api.BaseService;
import com.travelsky.base.repository.api.AbstractBaseServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class SampleUserService extends AbstractBaseServiceImpl<SampleUserEntity, Long, SampleUserDao> implements BaseService<SampleUserEntity, Long> {

    public SampleUserService(SampleUserDao repository) {
        super(repository);
    }

}
