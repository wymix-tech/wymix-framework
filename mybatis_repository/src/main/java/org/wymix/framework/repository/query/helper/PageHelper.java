package org.wymix.framework.repository.query.helper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.wymix.framework.common.query.DevQueryVO;
import org.wymix.framework.common.query.Page;

public class PageHelper {

    public static <T> Page<T> convert(IPage<T> page) {
        Page<T> ret = new Page<>();
        ret.setSize(page.getSize());
        ret.setPage(page.getCurrent());
        ret.setContent(page.getRecords());
        ret.setTotal(page.getTotal());
        ret.setTotalPage(page.getPages());
        return ret;
    }

    public static <T> IPage<T> toPageable(DevQueryVO devQueryVO) {
        return com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(devQueryVO.getPage(), devQueryVO.getLimit());
    }

}
