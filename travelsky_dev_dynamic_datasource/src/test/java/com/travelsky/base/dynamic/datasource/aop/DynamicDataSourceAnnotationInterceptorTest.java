package com.travelsky.base.dynamic.datasource.aop;

import com.travelsky.base.dynamic.datasource.annotations.DynamicDatasource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R11 回归：父类逐级向上回退查找 {@code @DynamicDatasource}。
 *
 * <p>修复前：{@code getDefaultDataSourceAttr} 以 {@code attr != null} 判断命中，
 * 而 {@code findDataSourceAttribute} 恒返回非 null 的 {@code Optional}，
 * 循环首轮（无注解父类/子类）即以 {@code Optional.empty()} 提前返回，深层回退失效。
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

    private final DynamicDataSourceAnnotationInterceptor interceptor = new DynamicDataSourceAnnotationInterceptor();

    @Test
    void shouldFallBackToAnnotatedGrandParentMethod() throws Exception {
        Method method = Child.class.getMethod("list");
        // 子类/父类均无注解，须逐级回退到 GrandParent 的 "slave"
        assertEquals("slave", interceptor.findKey(method, new Child(), DynamicDatasource.class));
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
        assertEquals("slave", interceptor.findKey(method, new UnAnnotatedChild(), DynamicDatasource.class));
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
        assertEquals("", interceptor.findKey(method, new PlainChild(), DynamicDatasource.class));
    }
}
