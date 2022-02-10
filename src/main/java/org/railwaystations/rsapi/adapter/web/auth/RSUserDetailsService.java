package org.railwaystations.rsapi.adapter.web.auth;

import org.railwaystations.rsapi.adapter.db.UserDao;
import org.railwaystations.rsapi.domain.model.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Service
@SuppressWarnings("PMD.BeanMembersShouldSerialize")
public class RSUserDetailsService implements UserDetailsService {

    private final UserDao userDao;

    public RSUserDetailsService(final UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public AuthUser loadUserByUsername(final String username) throws UsernameNotFoundException {
        final User user = userDao.findByEmail(User.normalizeEmail(username))
                .orElse(userDao.findByNormalizedName(User.normalizeName(username)).orElse(null));
        if (user == null) {
            throw new UsernameNotFoundException(String.format("User '%s' not found", username));
        }
        return new AuthUser(user, user.getRoles().stream().map(SimpleGrantedAuthority::new).collect(toList()));
    }

    public Optional<User> findById(final int id) {
        return userDao.findById(id);
    }

    public void updateEmailVerification(final User user) {
        if (user.isEmailVerifiedWithNextLogin()) {
            userDao.updateEmailVerification(user.getId(), User.EMAIL_VERIFIED);
        }
    }

}