package org.wymix.framework.repository.mybatis.api;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.wymix.framework.common.exception.BusinessException;
import org.wymix.framework.common.query.DevQueryVO;
import org.wymix.framework.common.query.Page;
import org.wymix.framework.core.api.BaseService;
import org.wymix.framework.repository.query.helper.PageHelper;
import org.wymix.framework.repository.query.helper.QueryParseHelper;
import org.wymix.framework.utils.ReflectUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractBaseServiceImpl<T, ID extends Serializable, REPOSITORY extends BaseRepository<T, ID>> implements BaseService<T, ID> {

    protected final REPOSITORY repository;

    private final Class<T> entityClass;

    public AbstractBaseServiceImpl(REPOSITORY repository) {
        this.repository = repository;
        this.entityClass = ReflectUtils.getSuperClassGenericType(this.getClass(), AbstractBaseServiceImpl.class, 0);
    }

    protected REPOSITORY getRepository() {
        return this.repository;
    }

    protected void afterSave(T entity) {
    }

    protected void beforeSave(T entity) {
    }

    protected void afterUpdate(T entity) {
    }

    protected void beforeUpdate(T old, T cur) {
    }

    @Override
    public T save(T entity) {
        beforeSave(entity);
        getRepository().insert(entity);
        afterSave(entity);
        return entity;
    }

    /**
     * 按主键更新。
     *
     * <p><b>必须先把 {@code id} 写回实体</b>：MyBatis-Plus 的 {@code updateById} 只认实体上的主键值，
     * 若请求体未带 id，会退化成「更新 0 行」并返回成功，造成「调用成功但数据没变」。
     */
    @Override
    public T update(ID id, T entity) {
        T old = this.getOne(id);
        beforeUpdate(old, entity);
        applyIdToEntity(entity, id);
        this.getRepository().updateById(entity);
        afterUpdate(entity);
        return entity;
    }

    @Override
    public void delete(ID id) {
        this.getRepository().deleteById(id);
    }

    @Override
    public List<T> save(Collection<T> entities) {
        return entities.stream().map(this::save).toList();
    }

    @Override
    public List<T> update(Collection<T> entities) {
        return entities.stream().map(entity -> this.update(findIdValueInEntity(entity), entity)).toList();
    }

    @Override
    public Collection<T> batchSave(Collection<T> entities) {
        getRepository().insert(entities, 500);
        return entities;
    }

    @Override
    public Collection<T> batchUpdate(Collection<T> entities) {
        getRepository().updateById(entities, 500);
        return entities;
    }

    @Override
    public void deleteAll(Collection<T> entities) {
        this.getRepository().deleteByIds(entities.stream().map(this::findIdValueInEntity).toList());
    }

    @Override
    public void deleteAllById(Collection<ID> ids) {
        this.getRepository().deleteByIds(ids);
    }

    @Override
    public T getOne(ID id) {
        return this.getRepository().selectById(id);
    }

    @Override
    public List<T> findAllById(Collection<ID> ids) {
        return this.getRepository().selectByIds(ids);
    }

    @Override
    public Page<T> findAll(DevQueryVO devQueryVO) {
        return PageHelper.convert(this.getRepository().selectPage(PageHelper.toPageable(devQueryVO), QueryParseHelper.parseToQueryWrapper(new QueryWrapper<T>(), devQueryVO, entityClass)));
    }

    private ID findIdValueInEntity(T entity) {
        Field[] fields = ReflectUtils.findFieldWithAnnotation(entity.getClass(), TableId.class);
        if (ArrayUtils.isEmpty(fields)) {
            throw new BusinessException("Miss Column: ID");
        }
        try {
            fields[0].setAccessible(true);
            return (ID) fields[0].get(entity);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /**
     * 把主键值写入实体的 {@code @TableId} 字段（{@code update(ID, T)} 之前调用）。
     */
    private void applyIdToEntity(T entity, ID id) {
        if (entity == null || id == null) {
            return;
        }
        Field[] fields = ReflectUtils.findFieldWithAnnotation(entity.getClass(), TableId.class);
        if (ArrayUtils.isEmpty(fields)) {
            throw new BusinessException("Miss Column: ID");
        }
        try {
            fields[0].setAccessible(true);
            fields[0].set(entity, id);
        } catch (IllegalAccessException e) {
            throw new BusinessException("Miss Column: ID");
        }
    }

    @Override
    public List<T> findAll() {
        return this.getRepository().selectList(new QueryWrapper<T>());
    }
}
