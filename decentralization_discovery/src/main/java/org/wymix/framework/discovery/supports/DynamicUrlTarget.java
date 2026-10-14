/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.supports
 * File Name:DynamicUrlTarget
 * Date:2025/9/4 16:09
 */
package org.wymix.framework.discovery.supports;


import com.alibaba.ttl.TransmittableThreadLocal;
import feign.Request;
import feign.RequestTemplate;
import feign.Target;

import static feign.Util.checkNotNull;
import static feign.Util.emptyToNull;

/**
 * ClassName: DynamicUrlTarget
 * Description: 
 * Date: 2025/9/4 16:09
 *
 * @author wang_yang
 */
public class DynamicUrlTarget<T> extends Target.HardCodedTarget<T> {


    private static final ThreadLocal<String> URL = TransmittableThreadLocal.withInitial(() -> "");
    private final Class<T> type;
    private final String name;

    public DynamicUrlTarget(Class<T> type, String name) {
        super(type, name);
        this.type = checkNotNull(type, "type");
        this.name = checkNotNull(emptyToNull(name), "name");
    }

    public void setUrl(String url) {
        URL.set(url);
    }

    @Override
    public Class<T> type() {
        return type;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String url() {
        String url = "http://" + URL.get();
        URL.remove();
        return url;
    }

    /* no authentication or other special activity. just insert the url. */
    @Override
    public Request apply(RequestTemplate input) {
        input.target(url());
        return input.request();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DynamicUrlTarget) {
            DynamicUrlTarget<?> other = (DynamicUrlTarget) obj;
            return type.equals(other.type)
                    && name.equals(other.name)
                    && URL.get().equals(other.URL.get());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + type.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + URL.get().hashCode();
        return result;
    }

    @Override
    public String toString() {
        if (name.equals(URL.get())) {
            return "DynamicUrlTarget(type=" + type.getSimpleName() + ", url=" + URL.get() + ")";
        }
        return "DynamicUrlTarget(type=" + type.getSimpleName() + ", name=" + name + ", url=" + URL.get()
                + ")";
    }
}
