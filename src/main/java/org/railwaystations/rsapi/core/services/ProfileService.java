package org.railwaystations.rsapi.core.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.adapter.out.db.OAuth2AuthorizationDao;
import org.railwaystations.rsapi.adapter.out.db.UserDao;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.in.ManageProfileUseCase;
import org.railwaystations.rsapi.core.ports.out.Mailer;
import org.railwaystations.rsapi.core.ports.out.Monitor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
@Slf4j
public class ProfileService implements ManageProfileUseCase {

    private final Monitor monitor;
    private final Mailer mailer;
    private final UserDao userDao;
    private final OAuth2AuthorizationDao authorizationDao;
    private final String eMailVerificationUrl;
    private final PasswordEncoder passwordEncoder;
    private final MessageSource messageSource;

    public ProfileService(Monitor monitor, Mailer mailer, UserDao userDao, OAuth2AuthorizationDao authorizationDao, @Value("${mailVerificationUrl}") String eMailVerificationUrl, PasswordEncoder passwordEncoder, MessageSource messageSource) {
        this.monitor = monitor;
        this.mailer = mailer;
        this.userDao = userDao;
        this.authorizationDao = authorizationDao;
        this.eMailVerificationUrl = eMailVerificationUrl;
        this.passwordEncoder = passwordEncoder;
        this.messageSource = messageSource;
    }

    @Override
    public void changePassword(User user, String newPassword) {
        log.info("Password change for '{}'", user.getEmail());
        var trimmedPassword = StringUtils.trimToEmpty(newPassword);
        if (trimmedPassword.length() < 8) {
            throw new IllegalArgumentException("Password too short");
        }
        userDao.updateCredentials(user.getId(), passwordEncoder.encode(trimmedPassword));
        authorizationDao.deleteAllByUser(user.getName());
    }

    @Override
    public void resetPassword(String nameOrEmail, String clientInfo) {
        log.info("Password reset requested for '{}'", nameOrEmail);
        var user = userDao.findByEmail(User.normalizeEmail(nameOrEmail))
                .orElse(userDao.findByNormalizedName(User.normalizeName(nameOrEmail)).orElse(null));

        if (user == null) {
            throw new IllegalArgumentException("Can't reset password for unknown user");
        }

        if (StringUtils.isBlank(user.getEmail())) {
            monitor.sendMessage(
                    String.format("Can't reset password for '%s' failed: no email available%nvia %s",
                            nameOrEmail, clientInfo));
            throw new IllegalArgumentException(String.format("Email '%s' is empty", user.getEmail()));
        }

        var newPassword = createNewPassword();
        var key = encryptPassword(newPassword);
        userDao.updateCredentials(user.getId(), key);
        monitor.sendMessage(String.format("Reset Password for '%s', email='%s'", user.getName(), user.getEmail()));

        sendPasswordMail(user.getEmail(), newPassword, user.getLocale());
        if (!user.isEmailVerified()) {
            // if the email is not yet verified, we can verify it with the next login
            userDao.updateEmailVerification(user.getId(), User.EMAIL_VERIFIED_AT_NEXT_LOGIN);
        }
        authorizationDao.deleteAllByUser(user.getName());
    }

    @Override
    public void register(User newUser, String clientInfo) throws ProfileConflictException {
        log.info("New registration for '{}' with '{}'", newUser.getName(), newUser.getEmail());

        if (!newUser.isValidForRegistration()) {
            throw new IllegalArgumentException("Invalid data");
        }

        Optional<User> existingName = userDao.findByNormalizedName(newUser.getNormalizedName());
        if (existingName.isPresent() && !newUser.getEmail().equals(existingName.get().getEmail())) {
            monitor.sendMessage(
                    String.format("Registration for user '%s' with eMail '%s' failed, name is already taken by different eMail '%s'%nvia %s",
                            newUser.getName(), newUser.getEmail(), existingName.get().getEmail(), clientInfo));
            throw new ProfileConflictException();
        }

        if (userDao.countBlockedUsername(newUser.getNormalizedName()) != 0) {
            monitor.sendMessage(
                    String.format("Registration for user '%s' with eMail '%s' failed, name is blocked%nvia %s",
                            newUser.getName(), newUser.getEmail(), clientInfo));
            throw new ProfileConflictException();
        }

        if (userDao.findByEmail(newUser.getEmail()).isPresent()) {
            monitor.sendMessage(
                    String.format("Registration for user '%s' with eMail '%s' failed, eMail is already taken%nvia %s",
                            newUser.getName(), newUser.getEmail(), clientInfo));
            throw new ProfileConflictException();
        }

        var password = newUser.getNewPassword();
        var emailVerificationToken = User.createNewEmailVerificationToken();
        boolean passwordProvided = StringUtils.isNotBlank(password);
        if (!passwordProvided) {
            password = createNewPassword();
            emailVerificationToken = User.EMAIL_VERIFIED_AT_NEXT_LOGIN;
        }

        var key = encryptPassword(password);
        saveRegistration(newUser, key, emailVerificationToken);

        if (passwordProvided) {
            sendEmailVerification(newUser.getEmail(), emailVerificationToken);
        } else {
            sendPasswordMail(newUser.getEmail(), password, newUser.getLocale());
        }

        monitor.sendMessage(
                String.format("New registration{nickname='%s', email='%s'}%nvia %s",
                        newUser.getName(), newUser.getEmail(), clientInfo));
    }

    private String createNewPassword() {
        return RandomStringUtils.randomAlphanumeric(12);
    }

    private String encryptPassword(String password) {
        return passwordEncoder.encode(password);
    }

    @Override
    public void updateProfile(User user, User newProfile, String clientInfo) throws ProfileConflictException {
        log.info("Update profile for '{}'", user.getEmail());

        if (!newProfile.isValid()) {
            log.info("Update Profile failed: User invalid {}", newProfile);
            throw new IllegalArgumentException();
        }

        if (!newProfile.getNormalizedName().equals(user.getNormalizedName())) {
            if (userDao.findByNormalizedName(newProfile.getNormalizedName()).isPresent()) {
                log.info("Name conflict '{}'", newProfile.getName());
                throw new ProfileConflictException();
            }
            monitor.sendMessage(
                    String.format("Update nickname for user '%s' to '%s'%nvia%s",
                            user.getName(), newProfile.getName(), clientInfo));
        }

        if (!newProfile.getEmail().equals(user.getEmail())) {
            if (userDao.findByEmail(newProfile.getEmail()).isPresent()) {
                log.info("Email conflict '{}'", newProfile.getEmail());
                throw new ProfileConflictException();
            }
            monitor.sendMessage(
                    String.format("Update email for user '%s' from email '%s' to '%s'%nvia%s",
                            user.getName(), user.getEmail(), newProfile.getEmail(), clientInfo));
            var emailVerificationToken = User.createNewEmailVerificationToken();
            sendEmailVerification(newProfile.getEmail(), emailVerificationToken);
            userDao.updateEmailVerification(user.getId(), emailVerificationToken);
        }

        userDao.update(user.getId(), newProfile);
    }

    @Override
    public void resendEmailVerification(User user) {
        log.info("Resend EmailVerification for '{}'", user.getEmail());
        var emailVerificationToken = User.createNewEmailVerificationToken();
        userDao.updateEmailVerification(user.getId(), emailVerificationToken);
        sendEmailVerification(user.getEmail(), emailVerificationToken);
    }

    @Override
    public Optional<User> emailVerification(String token) {
        return userDao.findByEmailVerification(token)
                .map(user -> {
                    userDao.updateEmailVerification(user.getId(), User.EMAIL_VERIFIED);
                    monitor.sendMessage(
                            String.format("Email verified {nickname='%s', email='%s'}", user.getName(), user.getEmail()));
                    return user;
                });
    }

    @Override
    public void deleteProfile(User user, String userAgent) {
        var normalizedName = user.getNormalizedName();
        userDao.anonymizeUser(user.getId());
        userDao.addUsernameToBlocklist(normalizedName);
        authorizationDao.deleteAllByUser(user.getName());
        monitor.sendMessage(
                String.format("Closed account %d - %s", user.getId(), user.getName()));
    }

    @Override
    public void updateLocale(User user, Locale locale) {
        userDao.updateLocale(user.getId(), locale.toLanguageTag());
    }

    private void sendPasswordMail(String email, String newPassword, Locale locale) {
        var text = messageSource.getMessage("password_mail", new String[]{newPassword}, locale);
        mailer.send(email, "Railway-Stations.org new password", text);
        log.info("Password sent to {}", email);
    }

    private void sendEmailVerification(String email, String emailVerificationToken) {
        var url = eMailVerificationUrl + emailVerificationToken;
        var text = String.format("""
                Hello,
                                        
                please click on %1$s to verify your eMail-Address.
                                        
                Cheers
                Your Railway-Stations-Team
                                        
                ---
                Hallo,
                                        
                bitte klicke auf %1$s, um Deine eMail-Adresse zu verifizieren.
                                        
                Viele Grüße
                Dein Bahnhofsfoto-Team""", url);
        mailer.send(email, "Railway-Stations.org eMail verification", text);
        log.info("Email verification sent to {}", email);
    }

    private void saveRegistration(User registration, String key, String emailVerification) {
        var id = userDao.insert(registration, key, emailVerification);
        log.info("User '{}' created with id {}", registration.getName(), id);
    }

}
