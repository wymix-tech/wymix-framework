package org.wymix.framework.repository.query;

import org.wymix.framework.common.query.DevQueryVO;
import org.wymix.framework.common.query.Page;
import org.wymix.framework.common.query.SortVO;
import org.wymix.framework.utils.StringUtils;
import org.wymix.framework.utils.json.JsonUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public class PageHelper {

    public static <T> Page<T> convert(org.springframework.data.domain.Page<T> page) {
        Page<T> ret = new Page<>();
        ret.setSize(page.getSize());
        ret.setPage(page.getNumber());
        ret.setContent(page.getContent());
        ret.setTotal(page.getTotalElements());
        ret.setTotalPage(page.getTotalPages());
        return ret;
    }

    public static Pageable toPageable(DevQueryVO devQueryVO) {
        Pageable pageable;
        if (StringUtils.isNotBlank(devQueryVO.getSorts())) {
            List<SortVO> sortVOS = JsonUtils.jsonToEntity(ArrayList.class, SortVO.class, devQueryVO.getSorts());
            int i = 0;
            Sort sort = null;
            do {
                SortVO s = sortVOS.get(i++);
                if (sort == null) {
                    sort = Sort.by(s.getDirection() == SortVO.Type.ASC ? Sort.Direction.ASC : Sort.Direction.DESC, s.getColumns());
                } else {
                    sort = sort.and(Sort.by(s.getDirection() == SortVO.Type.ASC ? Sort.Direction.ASC : Sort.Direction.DESC, s.getColumns()));
                }
            } while (i < sortVOS.size());
            pageable = PageRequest.of(devQueryVO.getPage().intValue() - 1, devQueryVO.getLimit().intValue(), sort);
        } else {
            pageable = PageRequest.of(devQueryVO.getPage().intValue() - 1, devQueryVO.getLimit().intValue());
        }
        return pageable;
    }

}
