package com.swcares.sample;

import com.swcares.sample.container.SampleMySqlContainer;
import com.swcares.sample.dao.SampleUserDao;
import com.swcares.sample.entity.SampleUserEntity;
import com.travelsky.base.test.annotations.DataSet;
import com.travelsky.base.test.configuration.JunitConfiguration;
import com.travelsky.base.test.enums.Operation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * travelsky_dev_test 示例：容器化数据库 + {@code @DataSet} 数据编排。
 *
 * <p>{@link DataSet} 由 {@link JunitConfiguration} 中的 {@code DataSetSupportListener} 解析，
 * setup 在测试前造数、teardown 在测试后清理。
 *
 * <p>默认禁用：本用例需要本地可用的 Docker 环境，确认环境就绪后删除 {@link Disabled} 即可运行。
 */
@SpringBootTest
@Disabled("需要本地 Docker 环境，确认就绪后移除该注解即可运行")
class SampleUserDaoTest extends SampleMySqlContainer {

    @Autowired
    private SampleUserDao sampleUserDao;

    @Test
    @DataSet(values = {"SAMPLE_USER"}, setup = Operation.INSERT, teardown = Operation.DELETE)
    void testSelectById() {
        SampleUserEntity entity = new SampleUserEntity();
        entity.setUsername("admin");
        entity.setPassword("Abc@123");
        sampleUserDao.insert(entity);

        SampleUserEntity loaded = sampleUserDao.selectById(entity.getId());
        Assertions.assertEquals("admin", loaded.getUsername());
    }
}
