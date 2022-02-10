package org.railwaystations.rsapi.adapter.web.auth;

import org.apache.commons.codec.digest.DigestUtils;
import org.railwaystations.rsapi.domain.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("PMD.BeanMembersShouldSerialize")
public class TokenGenerator {

    private final String salt;

    public TokenGenerator(@Value("${salt}") final String salt) {
        this.salt = salt;
    }

    public String buildFor(final String email, final Long userSalt) {
        return DigestUtils.sha1Hex(salt + "-" + User.normalizeEmail(email) + "-" + userSalt);
    }

}
