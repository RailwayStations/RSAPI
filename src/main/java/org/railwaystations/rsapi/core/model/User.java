package org.railwaystations.rsapi.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {


    /** the one and only valid license */
    public static final String CC0 = "CC0 1.0 Universell (CC0 1.0)";

    public static final String EMAIL_VERIFIED = "VERIFIED";
    public static final String EMAIL_VERIFIED_AT_NEXT_LOGIN = "NEXT_LOGIN";
    public static final String EMAIL_VERIFICATION_TOKEN = "TOKEN:";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";

    private int id;

    @EqualsAndHashCode.Include
    private final String name;

    private final String url;

    private final String license;

    private final String email;

    private final boolean ownPhotos;

    private final boolean anonymous;

    private String key;

    private final boolean admin;

    private String emailVerification;

    private String emailVerificationToken;

    private String newPassword;

    private boolean sendNotifications;

    public String getNormalizedName() {
        return normalizeName(name);
    }

    public String getEmail() {
        return normalizeEmail(email);
    }

    public static String normalizeName(final String name) {
        return StringUtils.trimToEmpty(name).toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]","");
    }

    public static String normalizeEmail(final String email) {
        final String trimmedEmail = StringUtils.trimToNull(email);
        return trimmedEmail != null ? trimmedEmail.toLowerCase(Locale.ENGLISH) : null;
    }

    public String getDisplayUrl() {
        return anonymous || StringUtils.isBlank(url) ? "https://railway-stations.org" : url;
    }

    public String getDisplayName() {
        return anonymous ? "Anonym" : getName();
    }

    /**
     * Checks if we have got a name and valid email for registration.
     */
    public boolean isValidForRegistration() {
        return StringUtils.isNotBlank(name) &&
                StringUtils.isNotBlank(email) &&
                new EmailValidator().isValid(email, null);
    }

    public boolean isValid() {
        if (!isValidForRegistration()) {
            return false;
        }
        if (StringUtils.isNotBlank(url)) {
            final URL validatedUrl;
            try {
                validatedUrl = new URL( url );
            } catch (final MalformedURLException e) {
                return false;
            }
            if (!validatedUrl.getProtocol().matches("https?")) {
                return false;
            }
        }

        if (!ownPhotos) {
            return false;
        }

        return CC0.equals(license);
    }

    public boolean isEmailVerified() {
        return EMAIL_VERIFIED.equals(emailVerification);
    }

    public boolean isEmailVerifiedWithNextLogin() {
        return EMAIL_VERIFIED_AT_NEXT_LOGIN.equals(emailVerification);
    }

    public void setEmailVerificationToken(final String emailVerificationToken) {
        this.emailVerificationToken = emailVerificationToken;
        this.emailVerification = EMAIL_VERIFICATION_TOKEN + emailVerificationToken;
    }

    public Set<String> getRoles() {
        final HashSet<String> roles = new HashSet<>();
        roles.add(ROLE_USER);
        if (isAdmin()) {
            roles.add(ROLE_ADMIN);
        }
        return roles;
    }

}
