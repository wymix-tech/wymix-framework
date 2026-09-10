package com.travelsky.base.dynamic.datasource.aop;

import com.travelsky.base.dynamic.datasource.annotations.DynamicDatasource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R11 回归：父类逐级向上回退查找 {@code @DynamicDatasource}。
 *
 * <p>修复前：{@code getDefaultDataSourceAttr} 以 {@code attr != null} 判断命中，
 * 而 {@code findDataSourceAttribute} 恒返回非 null 的 {@code Optional}，
 * 循环首轮（无注解父类/子类）即以 {@code Optional.empty()} 提前返回，深层回退失效。
 *
 * <p>另覆盖 MyBatis Mapper 代理的回退（{@code ENABLE_MYBATIS} 分支）：本模块不依赖 MyBatis，
 * 该静态分支默认关闭（死代码），测试通过反射开启并注入一个「带 {@code mapperInterface} 字段的
 * InvocationHandler」来模拟 {@code MapperProxy}，验证：
 * <ul>
 *   <li>Mapper 接口本身标注类级 {@code @DynamicDatasource} 时能命中；</li>
 *   <li>Mapper 父接口标注时能命中（原实现取 {@code clazz.getSuperclass()} 恒为 null，接口回退从不生效）。</li>
 * </ul>
 */
class DynamicDataSourceAnnotationInterceptorTest {

    static class GrandParent {
        @DynamicDatasource("slave")
        public String list() {
            return "grandParent";
        }
    }

    static class Parent extends GrandParent {
    }

    static class Child extends Parent {
    }

    @Test
    void shouldFallBackToAnnotatedGrandParentMethod() throws Exception {
        Method method = Child.class.getMethod("list");
        // 子类/父类均无注解，须逐级回退到 GrandParent 的 "slave"
        assertEquals("slave", newInterceptor().findKey(method, new Child(), DynamicDatasource.class));
    }

    @Test
    void shouldFallBackToAnnotatedParentClass() throws Exception {
        class AnnotatedParent {
            @DynamicDatasource("slave")
            public String list() {
                return "parent";
            }
        }
        class UnAnnotatedChild extends AnnotatedParent {
            @Override
            public String list() {
                return "child";
            }
        }
        Method method = UnAnnotatedChild.class.getMethod("list");
        assertEquals("slave", newInterceptor().findKey(method, new UnAnnotatedChild(), DynamicDatasource.class));
    }

    @Test
    void shouldReturnEmptyWhenNothingAnnotated() throws Exception {
        class PlainParent {
            public String list() {
                return "plain";
            }
        }
        class PlainChild extends PlainParent {
        }
        Method method = PlainChild.class.getMethod("list");
        assertEquals("", newInterceptor().findKey(method, new PlainChild(), DynamicDatasource.class));
    }

    @Test
    void shouldFallBackWhenSubclassOverridesWithoutAnnotation() throws Exception {
        class ParentWithMethodAnnotation {
            @DynamicDatasource("slave")
            public String list() {
                return "parent";
            }
        }
        // 子类重写了方法但没有注解：依赖 AnnotatedElementUtils.findMergedAnnotation 的查找语义回退
        class ChildOverride extends ParentWithMethodAnnotation {
            @Override
            public String list() {
                return "child";
            }
        }
        Method method = ChildOverride.class.getMethod("list");
        assertEquals("slave", newInterceptor().findKey(method, new ChildOverride(), DynamicDatasource.class));
    }

    /**
     * Mapper 接口自身标注类级注解：覆盖 {@code getDefaultDataSourceAttr} 的 Mapper 分支。
     *
     * <p>代理只实现 {@code UnrelatedMapper}（无注解），注解放在 {@code mapperInterface} 指向的
     * {@code SlaveMapper} 上，因此只能由 Mapper 分支命中，可精确验证该分支（否则会被「遍历实现类接口」
     * 这一步提前覆盖，测不出分支本身的缺陷）。
     */
    @Test
    void shouldReadAnnotationFromMapperInterface() throws Exception {
        Object mapperProxy = newMapperProxy(SlaveMapper.class, UnrelatedMapper.class);
        Method method = UnrelatedMapper.class.getMethod("select");
        assertEquals("slave", newInterceptor().findKey(method, mapperProxy, DynamicDatasource.class));
    }

    /**
     * Mapper 父接口标注：Mapper 分支原实现取 {@code clazz.getSuperclass()}（接口恒为 {@code null}），
     * 这段回退是死代码；现改为遍历 {@code getInterfaces()}，语义与「父接口回退」一致。
     *
     * <p>注：Spring 的 {@code AnnotatedElementUtils.findMergedAnnotation} 本身会按类型层级搜索接口，
     * 因此父接口注解在多数情况下已能命中；本用例主要保证 Mapper 分支确实可达（ENABLE_MYBATIS 分支被覆盖）。
     */
    @Test
    void shouldReadAnnotationFromMapperSuperInterface() throws Exception {
        Object mapperProxy = newMapperProxy(ChildMapper.class, UnrelatedMapper.class);
        Method method = UnrelatedMapper.class.getMethod("select");
        assertEquals("slave", newInterceptor().findKey(method, mapperProxy, DynamicDatasource.class));
    }

    /**
     * 每个用例新建一个 interceptor：{@code findKey} 以 {@code MethodClassKey} 做缓存，
     * 共用实例会让用例之间互相影响（缓存污染）。
     */
    private static DynamicDataSourceAnnotationInterceptor newInterceptor() {
        return new DynamicDataSourceAnnotationInterceptor();
    }

    @DynamicDatasource("slave")
    interface SlaveMapper {
        String select();
    }

    @DynamicDatasource("slave")
    interface BaseMapper {
        String select();
    }

    /** 子接口未标注：注解只存在于父接口 {@link BaseMapper} 上。 */
    interface ChildMapper extends BaseMapper {
    }

    /** 代理实现用的无关接口：无注解，用于确保注解只能由 Mapper 分支命中。 */
    interface UnrelatedMapper {
        String select();
    }

    /**
     * 构造模拟 MyBatis Mapper 代理的对象：JDK 动态代理 + InvocationHandler 持有 {@code mapperInterface} 字段。
     * 同时通过反射临时打开 {@code ENABLE_MYBATIS} 开关（本模块不含 MyBatis 依赖，该分支默认关闭）。
     *
     * @param mapperInterface  {@code MapperProxy#mapperInterface} 字段值（注解来源）
     * @param proxiedInterface 代理实际实现的接口（无注解）
     */
    private static Object newMapperProxy(Class<?> mapperInterface, Class<?> proxiedInterface) throws Exception {
        InvocationHandler handler = new MapperProxyStub(mapperInterface);
        Object proxy = Proxy.newProxyInstance(
                DynamicDataSourceAnnotationInterceptorTest.class.getClassLoader(),
                new Class<?>[]{proxiedInterface}, handler);
        enableMyBatis(MapperProxyStub.class);
        return proxy;
    }

    /** 模拟 {@code org.apache.ibatis.binding.MapperProxy}：字段名与之一致即可被反射读取。 */
    static class MapperProxyStub implements InvocationHandler {

        @SuppressWarnings("unused")
        private final Class<?> mapperInterface;

        MapperProxyStub(Class<?> mapperInterface) {
            this.mapperInterface = mapperInterface;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return null;
        }
    }

    /**
     * 反射开启 {@code ENABLE_MYBATIS} 并把 {@code mapperInterfaceField} 指向
     * {@link MapperProxyStub#mapperInterface}（模拟 MyBatis MapperProxy 的字段）。
     */
    private static void enableMyBatis(Class<?> mapperProxyClass) throws Exception {
        Field enableField = DynamicDataSourceAnnotationInterceptor.class.getDeclaredField("ENABLE_MYBATIS");
        enableField.setAccessible(true);
        enableField.setBoolean(null, true);

        Field field = DynamicDataSourceAnnotationInterceptor.class.getDeclaredField("mapperInterfaceField");
        field.setAccessible(true);
        Field mapperInterfaceField = mapperProxyClass.getDeclaredField("mapperInterface");
        mapperInterfaceField.setAccessible(true);
        field.set(null, mapperInterfaceField);
    }
}
