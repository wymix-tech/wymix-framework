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

@Deprecated(since = "3.0", forRemoval = false)
public class PageHelper {

    /**
     * Spring Data 页为 0-based，框架 {@link Page} 为 1-based（{@code Page#isFirst()} 判定 {@code page == 1}），
     * 此处必须 +1 才能与 MyBatis 侧 {@code IPage#getCurrent()} 的口径一致，
     * 否则 {@code page / hasPrev / hasNext} 会整体错位。
     */
    public static <T> Page<T> convert(org.springframework.data.domain.Page<T> page) {
        Page<T> ret = new Page<>();
        ret.setSize(page.getSize());
        ret.setPage(page.getNumber() + 1);
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
