package org.wymix.framework.repository.jpa.api;

import org.wymix.framework.common.exception.BusinessException;
import org.wymix.framework.common.query.DevQueryVO;
import org.wymix.framework.common.query.Page;
import org.wymix.framework.core.api.BaseService;
import org.wymix.framework.repository.query.PageHelper;
import org.wymix.framework.repository.query.SpecificationHelper;
import org.wymix.framework.utils.ReflectUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.Id;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractBaseServiceImpl<T, ID, REPOSITORY extends BaseRepository<T, ID>> implements BaseService<T, ID> {

    protected final REPOSITORY repository;

    public AbstractBaseServiceImpl(REPOSITORY repository) {
        this.repository = repository;
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
        entity = getRepository().saveAndFlush(entity);
        afterSave(entity);
        return entity;
    }

    @Override
    public T update(ID id, T entity) {
        T old = this.getOne(id);
        beforeUpdate(old, entity);
        entity = this.getRepository().saveAndFlush(entity);
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
    public List<T> batchSave(Collection<T> entities) {
        return getRepository().saveAllAndFlush(entities);
    }

    @Override
    public List<T> batchUpdate(Collection<T> entities) {
        return getRepository().saveAllAndFlush(entities);
    }

    @Override
    public void deleteAll(Collection<T> entities) {
        this.getRepository().deleteAll(entities);
    }

    @Override
    public void deleteAllById(Collection<ID> ids) {
        this.getRepository().deleteAllById(ids);
    }

    @Override
    public T getOne(ID id) {
        Optional<T> opt = this.getRepository().findById(id);
        if (opt.isPresent()) {
            return opt.get();
        }
        return null;
    }

    @Override
    public List<T> findAllById(Collection<ID> ids) {
        return this.getRepository().findAllById(ids);
    }

    @Override
    public Page<T> findAll(DevQueryVO devQueryVO) {
        return PageHelper.convert(this.getRepository().findAll((Specification<T>) (root, query, criteriaBuilder) -> SpecificationHelper.parse(devQueryVO, root, query, criteriaBuilder), PageHelper.toPageable(devQueryVO)));
    }

    /**
     * 全量查询，与 MyBatis 侧 {@code AbstractBaseServiceImpl} 保持同一份 {@link BaseService} 契约。
     *
     * <p>通过 {@code Specification} 执行（而非无参 {@code findAll()}），以便 {@code @DataPermission} 上下文
     * （see {@link SpecificationHelper#applyDataPermission}）能像 MyBatis 侧插件一样追加过滤条件；
     * 无权限上下文时行为与普通 {@code findAll()} 一致。
     *
     * <p>注意：不分页且不带条件，数据量大时请改用 {@link #findAll(DevQueryVO)}。
     */
    @Override
    public List<T> findAll() {
        return this.getRepository().findAll((root, query, criteriaBuilder) -> SpecificationHelper.applyDataPermission(root, criteriaBuilder));
    }

    private ID findIdValueInEntity(T entity) {
        Field[] fields = ReflectUtils.findFieldWithAnnotation(entity.getClass(), Id.class);
        if (ArrayUtils.isEmpty(fields)) {
            throw new BusinessException("Miss Column: ID");
        }
        try {
            return (ID) fields[0].get(entity);
        } catch (IllegalAccessException e) {
            return null;
        }

    }
}
