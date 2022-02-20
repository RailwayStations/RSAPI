package org.railwaystations.rsapi.app.auth;

import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.core.model.User;
import org.springframework.security.core.GrantedAuthority;

import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.util.Collection;

public class AuthUser extends org.springframework.security.core.userdetails.User {

    @Serial
    private static final long serialVersionUID = 1L;

    private final User user;

    public AuthUser(@NotNull final User user, final Collection<? extends GrantedAuthority> authorities) {
        super(user.getDisplayName(), StringUtils.defaultString(user.getKey()), authorities);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
