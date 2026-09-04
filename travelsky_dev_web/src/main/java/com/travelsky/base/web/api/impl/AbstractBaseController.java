package com.travelsky.base.web.api.impl;


import com.travelsky.base.common.query.DevQueryVO;
import com.travelsky.base.common.query.Page;
import com.travelsky.base.core.api.BaseService;
import com.travelsky.base.utils.ReflectionUtils;
import com.travelsky.base.web.api.BaseController;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collection;
import java.util.stream.Collectors;

public abstract class AbstractBaseController<T, V, ID, SERVICE extends BaseService<T, ID>> implements BaseController<T, V, ID> {


    private final SERVICE service;
    private Class<?> entityClass;
    private Class<?> voClass;

    public AbstractBaseController(SERVICE service) {
        this.service = service;
        initializer();
    }

    private void initializer() {
        entityClass = ReflectionUtils.getSuperClassGenericType(this.getClass(), 0);
        voClass = ReflectionUtils.getSuperClassGenericType(this.getClass(), 1);
    }

    protected SERVICE getService() {
        return service;
    }

    @Override
    public V getOne(@PathVariable ID id) {
        return this.mappedToVO(this.getService().getOne(id));
    }

    @Override
    public Page<V> findAll(DevQueryVO devQueryVO) {
        return this.getService().findAll(devQueryVO).map(this::mappedToVO);
    }

    @Override
    public V save(@RequestBody V v) {
        return this.mappedToVO(this.getService().save(this.mappedToPO(v)));
    }


    @Override
    public V update(@PathVariable ID id,@RequestBody V v) {
        return this.mappedToVO(this.getService().update(id, this.mappedToPO(v)));
    }

    @Override
    public void delete(@PathVariable ID id) {
        this.getService().delete(id);
    }

    @Override
    public Collection<V> saveBatch(@RequestBody Collection<V> vs) {
        return this.getService().batchSave(vs.stream().map(this::mappedToPO).toList()).stream().map(this::mappedToVO).toList();
    }

    @Override
    public Collection<V> updateBatch(@RequestBody Collection<V> vs) {
        return this.getService().batchUpdate(vs.stream().map(this::mappedToPO).toList()).stream().map(this::mappedToVO).toList();
    }

    @Override
    public void deleteBatch(@RequestBody Collection<ID> ids) {
        this.getService().deleteAllById(ids);
    }

    protected V mappedToVO(T t) {
        if (t == null) {
            return null;
        }
        V v = (V) ReflectionUtils.newInstance(this.getClass(), 1);
        BeanUtils.copyProperties(t, v);
        return v;
    }

    protected T mappedToPO(V v) {
        if (v == null) {
            return null;
        }
        T t = (T) ReflectionUtils.newInstance(this.getClass(), 0);
        BeanUtils.copyProperties(v, t);
        return t;
    }

}
