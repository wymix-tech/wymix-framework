package org.wymix.framework.repository.datachangelog.supports;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Triple;
import org.hibernate.Session;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Deprecated(since = "3.0", forRemoval = false)
public class DataChangeLogListener implements PostInsertEventListener, PreUpdateEventListener, PostDeleteEventListener {

    private static final Logger log = LoggerFactory.getLogger(DataChangeLogListener.class);

    private static final ThreadLocal<Map<Class<?>, Set<Object>>> MAP_THREAD_LOCAL = ThreadLocal.withInitial(() -> null);
    @Autowired
    private DataChangeLogHandler dataChangeLogHandler;

    public static final void flush() {
        MAP_THREAD_LOCAL.remove();
    }

    public static final void open(String uri, Long requestId, String requestRemoteIp) {
        ConcurrentHashMap<Class<?>, Set<Object>> value = new ConcurrentHashMap<>();
        value.put(Triple.class, Sets.newHashSet(Triple.of(uri, requestId, requestRemoteIp)));
        MAP_THREAD_LOCAL.set(value);
    }

    public static final String getUri() {
        Optional<Object> first = MAP_THREAD_LOCAL.get().getOrDefault(Triple.class, new HashSet<>()).stream().findFirst();
        if (first.isPresent()) {
            return ((Triple<String, Long, String>) first.get()).getLeft();
        }
        return null;
    }

    public static final Long getRequestId() {
        Optional<Object> first = MAP_THREAD_LOCAL.get().getOrDefault(Triple.class, new HashSet<>()).stream().findFirst();
        if (first.isPresent()) {
            return ((Triple<String, Long, String>) first.get()).getMiddle();
        }
        return null;
    }

    public static final String getRequestRemoteIp() {
        Optional<Object> first = MAP_THREAD_LOCAL.get().getOrDefault(Triple.class, new HashSet<>()).stream().findFirst();
        if (first.isPresent()) {
            return ((Triple<String, Long, String>) first.get()).getRight();
        }
        return null;
    }

    public static boolean isWebRequest() {
        return MAP_THREAD_LOCAL.get() != null;
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        if (!isWebRequest()) {
            return;
        }
        dataChangeLogHandler.touchForDelete(event);
        if (log.isDebugEnabled()) {
            log.debug("Data Change Log: Delete {} {}", event.getPersister().getEntityName(), event.getId());
        }
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        if (!isWebRequest()) {
            return;
        }
        dataChangeLogHandler.touchForInsert(event);
        if (log.isDebugEnabled()) {
            log.debug("Data Change Log: Insert {} {}", event.getPersister().getEntityName(), event.getId());
        }
    }

    /**
     * 更新前记录变更日志。
     *
     * <p><b>返回值语义（务必返回 false）</b>：Hibernate {@code PreUpdateEventListener#onPreUpdate}
     * 的返回值表示「是否否决（veto）本次更新」，返回 {@code true} 会让 Hibernate <b>跳过 UPDATE 语句的执行</b>，
     * 表现为「接口返回 200、变更日志也记录了，但数据库没变」。因此所有分支都必须返回 {@code false}。
     *
     * @param event Hibernate 更新前事件
     * @return 固定 false（不否决更新）
     */
    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        if (!isWebRequest()) {
            return false;
        }
        if (MAP_THREAD_LOCAL.get().containsKey(event.getEntity().getClass())) {
            if (MAP_THREAD_LOCAL.get().get(event.getEntity().getClass()).contains(event.getId())) {
                return false;
            }
            MAP_THREAD_LOCAL.get().get(event.getEntity().getClass()).add(event.getId());
        } else {
            MAP_THREAD_LOCAL.get().put(event.getEntity().getClass(), Sets.newHashSet(event.getId()));
        }
        // 临时 Session 仅用于读取更新前的旧值以产出变更日志，用后必须关闭，否则每次 UPDATE 泄漏一个 Session
        Session session = event.getSession().getSessionFactory().openTemporarySession();
        try {
            Object o = session.get(event.getEntity().getClass(), event.getId());
            dataChangeLogHandler.touchForUpdate(event, o);
        } finally {
            session.close();
        }
        if (log.isDebugEnabled()) {
            log.debug("Data Change Log: Update {} {}", event.getPersister().getEntityName(), event.getId());
        }
        return false;
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister entityPersister) {
        return false;
    }
}
