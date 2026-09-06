package com.travelsky.base.repository.api;

/**
 * @deprecated 旧包名兼容别名，请迁移到 {@link com.travelsky.base.repository.mybatis.api.AbstractBaseServiceImpl}，
 *             计划于下个大版本移除。
 */
@Deprecated(since = "3.x", forRemoval = true)
public abstract class AbstractBaseServiceImpl<T, ID extends java.io.Serializable, REPOSITORY extends BaseRepository<T, ID>>
        extends com.travelsky.base.repository.mybatis.api.AbstractBaseServiceImpl<T, ID, REPOSITORY> {

    public AbstractBaseServiceImpl(REPOSITORY repository) {
        super(repository);
    }
}
