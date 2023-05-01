package org.railwaystations.rsapi.core.ports.in;

import org.railwaystations.rsapi.core.model.User;

import java.util.Locale;
import java.util.Optional;

public interface ManageProfileUseCase {

    void changePassword(User user, String newPassword);

    void resetPassword(String nameOrEmail, String clientInfo);

    void register(User newUser, String clientInfo) throws ProfileConflictException;

    void updateProfile(User user, User newProfile, String clientInfo) throws ProfileConflictException;

    void resendEmailVerification(User user);

    Optional<User> emailVerification(String token);

    void deleteProfile(User user, String userAgent);

    void updateLocale(User user, Locale locale);

    class ProfileConflictException extends RuntimeException {

        public ProfileConflictException() {
            super("Name or eMail is already taken");
        }

    }

}
