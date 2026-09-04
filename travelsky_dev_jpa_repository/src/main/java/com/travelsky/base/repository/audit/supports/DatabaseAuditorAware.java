package com.travelsky.base.repository.audit.supports;

import com.travelsky.base.common.context.Principal;
import com.travelsky.base.common.context.UserContextHolder;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

public class DatabaseAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Principal userContext = UserContextHolder.getUserContext();
        if (userContext == null) {
            return Optional.of("SYSTEM");
        }
        return Optional.ofNullable(userContext.getPrincipal());
    }

}
