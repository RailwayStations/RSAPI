package org.railwaystations.rsapi.core.ports.in;

import org.railwaystations.rsapi.core.model.PasswordChangeCommand;
import org.railwaystations.rsapi.core.model.User;

import java.util.Optional;

public interface ManageProfileUseCase {

    void changePassword(User user, PasswordChangeCommand newPassword);

    User resetPassword(String nameOrEmail, String clientInfo);

    void register(User newUser, String clientInfo) throws ProfileConflictException;

    void updateProfile(User user, User newProfile, String clientInfo) throws ProfileConflictException;

    void resendEmailVerification(User user);

    Optional<User> emailVerification(String token);

    class ProfileConflictException extends Exception {

        public ProfileConflictException(final String message) {
            super(message);
        }

    }

}