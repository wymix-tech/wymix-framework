package org.wymix.framework.repository.audit.supports;

import org.wymix.framework.common.context.Principal;
import org.wymix.framework.common.context.UserContextHolder;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

@Deprecated(since = "3.0", forRemoval = false)
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
