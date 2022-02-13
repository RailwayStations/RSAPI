package org.railwaystations.rsapi.services;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.adapter.db.UserDao;
import org.railwaystations.rsapi.domain.model.User;
import org.railwaystations.rsapi.domain.port.out.Mailer;
import org.railwaystations.rsapi.domain.port.out.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProfileService {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileService.class);

    private final Monitor monitor;
    private final Mailer mailer;
    private final UserDao userDao;
    private final String eMailVerificationUrl;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(final Monitor monitor, final Mailer mailer, final UserDao userDao, @Value("${mailVerificationUrl}") final String eMailVerificationUrl, final PasswordEncoder passwordEncoder) {
        this.monitor = monitor;
        this.mailer = mailer;
        this.userDao = userDao;
        this.eMailVerificationUrl = eMailVerificationUrl;
        this.passwordEncoder = passwordEncoder;
    }

    public void changePassword(final User user, final String newPassword) {
        LOG.info("Password change for '{}'", user.getEmail());
        final String trimmedPassword = StringUtils.trimToEmpty(newPassword);
        if (trimmedPassword.length() < 8 ) {
            throw new IllegalArgumentException("Password too short");
        }
        userDao.updateCredentials(user.getId(), passwordEncoder.encode(trimmedPassword));
    }

    public User resetPassword(final String nameOrEmail, final String clientInfo) {
        LOG.info("Password reset requested for '{}'", nameOrEmail);
        final User user = userDao.findByEmail(User.normalizeEmail(nameOrEmail))
                .orElse(userDao.findByNormalizedName(User.normalizeName(nameOrEmail)).orElse(null));

        if (user == null) {
            return null;
        }

        if (StringUtils.isBlank(user.getEmail())) {
            monitor.sendMessage(
                    String.format("Can't reset password for '%s' failed: no email available%nvia %s",
                            nameOrEmail, clientInfo));
            throw new IllegalArgumentException(String.format("Email '%s' is empty", user.getEmail()));
        }

        user.setNewPassword(createNewPassword());
        encryptPassword(user);
        userDao.updateCredentials(user.getId(), user.getKey());
        monitor.sendMessage(String.format("Reset Password for '%s', email='%s'", user.getName(), user.getEmail()));

        sendPasswordMail(user);
        if (!user.isEmailVerified()) {
            // if the email is not yet verified, we can verify it with the next login
            userDao.updateEmailVerification(user.getId(), User.EMAIL_VERIFIED_AT_NEXT_LOGIN);
        }
        return user;
    }

    public void register(final User newUser, final String clientInfo) throws ProfileConflictException {
        LOG.info("New registration for '{}' with '{}'", newUser.getName(), newUser.getEmail());

        if (!newUser.isValidForRegistration()) {
            throw new IllegalArgumentException("Invalid data");
        }

        final Optional<User> existingName = userDao.findByNormalizedName(newUser.getNormalizedName());
        if (existingName.isPresent() && !newUser.getEmail().equals(existingName.get().getEmail())) {
            monitor.sendMessage(
                    String.format("Registration for user '%s' with eMail '%s' failed, name is already taken by different eMail '%s'%nvia %s",
                            newUser.getName(), newUser.getEmail(), existingName.get().getEmail(), clientInfo));
            throw new ProfileConflictException("Name is already taken by different eMail");
        }

        if (userDao.findByEmail(newUser.getEmail()).isPresent()) {
            monitor.sendMessage(
                    String.format("Registration for user '%s' with eMail '%s' failed, eMail is already taken%nvia %s",
                            newUser.getName(), newUser.getEmail(), clientInfo));
            throw new ProfileConflictException("eMail is already taken");
        }

        final boolean passwordProvided = StringUtils.isNotBlank(newUser.getNewPassword());
        if (!passwordProvided) {
            newUser.setNewPassword(createNewPassword());
            newUser.setEmailVerification(User.EMAIL_VERIFIED_AT_NEXT_LOGIN);
        } else {
            newUser.setEmailVerificationToken(UUID.randomUUID().toString());
        }

        encryptPassword(newUser);
        saveRegistration(newUser);

        if (passwordProvided) {
            sendEmailVerification(newUser);
        } else {
            sendPasswordMail(newUser);
        }

        monitor.sendMessage(
                String.format("New registration{nickname='%s', email='%s', license='%s', photoOwner=%s, link='%s', anonymous=%s}%nvia %s",
                        newUser.getName(), newUser.getEmail(), newUser.getLicense(), newUser.isOwnPhotos(),
                        newUser.getUrl(), newUser.isAnonymous(), clientInfo));
    }

    private String createNewPassword() {
        return RandomStringUtils.randomAlphanumeric(12);
    }

    private void encryptPassword(@NotNull final User user) {
        user.setKey(passwordEncoder.encode(user.getNewPassword()));
    }

    public void updateProfile(final User user, final User newProfile, final String clientInfo) throws ProfileConflictException {
        LOG.info("Update profile for '{}'", user.getEmail());

        if (!newProfile.isValid()) {
            LOG.info("Update Profile failed: User invalid {}", newProfile);
            throw new IllegalArgumentException();
        }

        if (!newProfile.getNormalizedName().equals(user.getNormalizedName())) {
            if (userDao.findByNormalizedName(newProfile.getNormalizedName()).isPresent()) {
                LOG.info("Name conflict '{}'", newProfile.getName());
                throw new ProfileConflictException("Name conflict");
            }
            monitor.sendMessage(
                    String.format("Update nickname for user '%s' to '%s'%nvia%s",
                            user.getName(), newProfile.getName(), clientInfo));
        }

        if (!newProfile.getEmail().equals(user.getEmail())) {
            if (userDao.findByEmail(newProfile.getEmail()).isPresent()) {
                LOG.info("Email conflict '{}'", newProfile.getEmail());
                throw new ProfileConflictException("Email conflict");
            }
            newProfile.setEmailVerificationToken(UUID.randomUUID().toString());
            monitor.sendMessage(
                    String.format("Update email for user '%s' from email '%s' to '%s'%nvia%s",
                            user.getName(), user.getEmail(), newProfile.getEmail(), clientInfo));
            sendEmailVerification(newProfile);
        } else {
            // keep email verification status
            newProfile.setEmailVerification(user.getEmailVerification());
        }

        newProfile.setId(user.getId());
        userDao.update(newProfile);
    }

    public void resendEmailVerification(final User user) {
        LOG.info("Resend EmailVerification for '{}'", user.getEmail());
        user.setEmailVerificationToken(UUID.randomUUID().toString());
        userDao.updateEmailVerification(user.getId(), user.getEmailVerification());
        sendEmailVerification(user);
    }

    public Optional<User> emailVerification(final String token) {
        final Optional<User> userByToken = userDao.findByEmailVerification(User.EMAIL_VERIFICATION_TOKEN + token);
        if (userByToken.isPresent()) {
            final User user = userByToken.get();
            userDao.updateEmailVerification(user.getId(), User.EMAIL_VERIFIED);
            monitor.sendMessage(
                    String.format("Email verified {nickname='%s', email='%s'}", user.getName(), user.getEmail()));
        }
        return userByToken;
    }

    private void sendPasswordMail(@NotNull final User user) {
        final String text = String.format("Hello,%n%n" +
                        "your new password is: %1$s%n%n" +
                        "Cheers%n" +
                        "Your Railway-Stations-Team%n" +
                        "%n---%n" +
                        "Hallo,%n%n" +
                        "Dein neues Passwort lautet: %1$s%n%n" +
                        "Viele Grüße%n" +
                        "Dein Bahnhofsfoto-Team", user.getNewPassword());
        mailer.send(user.getEmail(), "Railway-Stations.org new password", text);
        LOG.info("Password sent to {}", user.getEmail());
    }

    private void sendEmailVerification(@NotNull final User user) {
        final String url = eMailVerificationUrl + user.getEmailVerificationToken();
        final String text = String.format("Hello,%n%n" +
                        "please click on %1$s to verify your eMail-Address.%n%n" +
                        "Cheers%n" +
                        "Your Railway-Stations-Team%n" +
                        "%n---%n" +
                        "Hallo,%n%n" +
                        "bitte klicke auf %1$s, um Deine eMail-Adresse zu verifizieren%n%n" +
                        "Viele Grüße%n" +
                        "Dein Bahnhofsfoto-Team", url);
        mailer.send(user.getEmail(), "Railway-Stations.org eMail verification", text);
        LOG.info("Email verification sent to {}", user.getEmail());
    }

    private void saveRegistration(final User registration) {
        final Integer id = userDao.insert(registration);
        LOG.info("User '{}' created with id {}", registration.getName(), id);
    }

    public static class ProfileConflictException extends Exception {

        public ProfileConflictException(final String message) {
            super(message);
        }

    }

}
