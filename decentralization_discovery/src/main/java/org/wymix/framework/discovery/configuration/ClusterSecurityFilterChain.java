package org.wymix.framework.discovery.configuration;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/** Marker allowing business auto-configuration to coexist with the dedicated cluster chain. */
public final class ClusterSecurityFilterChain implements SecurityFilterChain {
    private final SecurityFilterChain delegate;

    public ClusterSecurityFilterChain(SecurityFilterChain delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return delegate.matches(request);
    }

    @Override
    public List<Filter> getFilters() {
        return delegate.getFilters();
    }
}
