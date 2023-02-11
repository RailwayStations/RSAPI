package org.railwaystations.rsapi.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Value
@Builder
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {


    public static final String EMAIL_VERIFIED = "VERIFIED";
    public static final String EMAIL_VERIFIED_AT_NEXT_LOGIN = "NEXT_LOGIN";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ANONYM = "Anonym";

    int id;

    @EqualsAndHashCode.Include
    String name;

    String url;

    License license;

    String email;

    boolean ownPhotos;

    boolean anonymous;

    String key;

    boolean admin;

    String emailVerification;

    String newPassword;

    boolean sendNotifications;

    public String getNormalizedName() {
        return normalizeName(name);
    }

    public String getEmail() {
        return normalizeEmail(email);
    }

    public static String normalizeName(String name) {
        return StringUtils.trimToEmpty(name).toLowerCase(Locale.ENGLISH).replaceAll("[^a-z\\d]", "");
    }

    public static String normalizeEmail(String email) {
        var trimmedEmail = StringUtils.trimToNull(email);
        return trimmedEmail != null ? trimmedEmail.toLowerCase(Locale.ENGLISH) : null;
    }

    public String getDisplayUrl() {
        return anonymous || StringUtils.isBlank(url) ? "https://railway-stations.org" : url;
    }

    public String getDisplayName() {
        return anonymous ? ANONYM : getName();
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
            URL validatedUrl;
            try {
                validatedUrl = new URL(url);
            } catch (MalformedURLException e) {
                return false;
            }
            return validatedUrl.getProtocol().matches("https?");
        }

        return true;
    }

    public boolean isEligibleToUploadPhoto() {
        return isValid() && isEmailVerified() && ownPhotos && License.CC0_10.equals(license);
    }

    public boolean isEligibleToReportProblem() {
        return isEmailVerified() && isValid();
    }

    public boolean isEmailVerified() {
        return EMAIL_VERIFIED.equals(emailVerification);
    }

    public boolean isEmailVerifiedWithNextLogin() {
        return EMAIL_VERIFIED_AT_NEXT_LOGIN.equals(emailVerification);
    }

    public static String createNewEmailVerificationToken() {
        return UUID.randomUUID().toString();
    }

    public Set<String> getRoles() {
        var roles = new HashSet<String>();
        roles.add(ROLE_USER);
        if (isAdmin()) {
            roles.add(ROLE_ADMIN);
        }
        return roles;
    }

}
