package org.wymix.framework.core.api;

import org.wymix.framework.common.query.DevQueryVO;
import org.wymix.framework.common.query.Page;

import java.util.Collection;
import java.util.List;

public interface BaseService<T, ID> {

    T save(T entity);

    T update(ID id, T entity);

    void delete(ID id);

    List<T> save(Collection<T> entities);

    List<T> update(Collection<T> entities);

    Collection<T> batchSave(Collection<T> entities);

    Collection<T> batchUpdate(Collection<T> entities);

    void deleteAll(Collection<T> entities);

    void deleteAllById(Collection<ID> ids);

    T getOne(ID id);

    List<T> findAllById(Collection<ID> ids);

    Page<T> findAll(DevQueryVO devQueryVO);

    List<T> findAll();
}
