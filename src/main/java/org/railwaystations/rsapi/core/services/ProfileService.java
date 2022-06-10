package org.railwaystations.rsapi.core.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.adapter.out.db.UserDao;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.in.ManageProfileUseCase;
import org.railwaystations.rsapi.core.ports.out.Mailer;
import org.railwaystations.rsapi.core.ports.out.Monitor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class ProfileService implements ManageProfileUseCase {

    private final Monitor monitor;
    private final Mailer mailer;
    private final UserDao userDao;
    private final String eMailVerificationUrl;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(Monitor monitor, Mailer mailer, UserDao userDao, @Value("${mailVerificationUrl}") String eMailVerificationUrl, PasswordEncoder passwordEncoder) {
        this.monitor = monitor;
        this.mailer = mailer;
        this.userDao = userDao;
        this.eMailVerificationUrl = eMailVerificationUrl;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void changePassword(User user, String newPassword) {
        log.info("Password change for '{}'", user.getEmail());
        var trimmedPassword = StringUtils.trimToEmpty(newPassword);
        if (trimmedPassword.length() < 8 ) {
            throw new IllegalArgumentException("Password too short");
        }
        userDao.updateCredentials(user.getId(), passwordEncoder.encode(trimmedPassword));
    }

    @Override
    public User resetPassword(String nameOrEmail, String clientInfo) {
        log.info("Password reset requested for '{}'", nameOrEmail);
        var user = userDao.findByEmail(User.normalizeEmail(nameOrEmail))
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

        if (userDao.findByEmail(newUser.getEmail()).isPresent()) {
            monitor.sendMessage(
                    String.format("Registration for user '%s' with eMail '%s' failed, eMail is already taken%nvia %s",
                            newUser.getName(), newUser.getEmail(), clientInfo));
            throw new ProfileConflictException();
        }

        boolean passwordProvided = StringUtils.isNotBlank(newUser.getNewPassword());
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

    private void encryptPassword(@NotNull User user) {
        user.setKey(passwordEncoder.encode(user.getNewPassword()));
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

    @Override
    public void resendEmailVerification(User user) {
        log.info("Resend EmailVerification for '{}'", user.getEmail());
        user.setEmailVerificationToken(UUID.randomUUID().toString());
        userDao.updateEmailVerification(user.getId(), user.getEmailVerification());
        sendEmailVerification(user);
    }

    @Override
    public Optional<User> emailVerification(String token) {
        var userByToken = userDao.findByEmailVerification(User.EMAIL_VERIFICATION_TOKEN + token);
        if (userByToken.isPresent()) {
            User user = userByToken.get();
            userDao.updateEmailVerification(user.getId(), User.EMAIL_VERIFIED);
            monitor.sendMessage(
                    String.format("Email verified {nickname='%s', email='%s'}", user.getName(), user.getEmail()));
        }
        return userByToken;
    }

    private void sendPasswordMail(@NotNull User user) {
        var text = String.format("""
                        Hello,
                        
                        your new password is: %1$s
                        
                        Cheers
                        Your Railway-Stations-Team
                        
                        ---
                        Hallo,
                        
                        Dein neues Passwort lautet: %1$s
                        
                        Viele Grüße
                        Dein Bahnhofsfoto-Team""", user.getNewPassword());
        mailer.send(user.getEmail(), "Railway-Stations.org new password", text);
        log.info("Password sent to {}", user.getEmail());
    }

    private void sendEmailVerification(@NotNull User user) {
        var url = eMailVerificationUrl + user.getEmailVerificationToken();
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
        mailer.send(user.getEmail(), "Railway-Stations.org eMail verification", text);
        log.info("Email verification sent to {}", user.getEmail());
    }

    private void saveRegistration(User registration) {
        var id = userDao.insert(registration);
        log.info("User '{}' created with id {}", registration.getName(), id);
    }

}
