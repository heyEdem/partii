package com.theinside.partii.utils.validators;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.theinside.partii.entity.User;
import com.theinside.partii.exception.NotFoundException;
import com.theinside.partii.exception.VerificationFailedException;
import com.theinside.partii.repository.UserRepository;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

import static com.theinside.partii.utils.CustomMessages.*;


/**
 * Utility class for validating email, password, and phone number formats.
 * This class provides static methods to validate email addresses, passwords, and phone numbers
 * according to specified rules and patterns.
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomValidator {

    private final UserRepository userRepository;
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z]{2,})$"
    );

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");
    private static final Pattern HAS_SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?]");
    private static final Pattern HAS_WHITESPACE = Pattern.compile("\\s");

    private static final int MAX_PHONE_LENGTH = 15;
    private static final String PHONE_PATTERN = "^\\+?[0-9]{1,3}?[-.\\s]?\\(?[0-9]{1,4}?\\)?[-.\\s]?[0-9]{1,4}[-.\\s]?[0-9]{1,4}[-.\\s]?[0-9]{1,9}$";

    public static String validateEmail(String value) {
        if (value == null || value.isBlank()) {
            return EMAIL_NOT_BLANK;
        }
        if (value.length() > MAX_EMAIL_LENGTH) {
            return "Email length must not exceed " + MAX_EMAIL_LENGTH + " characters";
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            return "Invalid email format";
        }
        return null;
    }


    public User loadUser(String email, Long id) {
        if (email != null) {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));
        } else if (id != null) {
            return userRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));
        } else {
            throw new IllegalArgumentException("Either email or id must be provided");
        }
    }

    public User loadAccountByEmail(String email) {
        return loadUser(email, null);
    }

    public User loadUserByEmail(Long id) {
        return loadUser(null, id);
    }

    public User validateAccount(User account){

        if (!account.isVerified()) {
            throw new VerificationFailedException(UNVERIFIED_ACCOUNT);
        }

        if(!account.isEnabled()) {
            throw new DisabledException(ACCOUNT_DISABLED);
        }

        log.info("User {} is verified and enabled", account.getEmail());

        return account;
    }

    public String validatePhoneNumber(String phoneNumber) {
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try {
            if ( phoneNumber == null || phoneNumber.isBlank()) {
                return PHONE_NUMBER_NOT_BLANK;
            }
            if (phoneNumber.length() > MAX_PHONE_LENGTH) {
                return "Phone number length must not exceed " + MAX_PHONE_LENGTH + " characters";
            }
//            if (!phoneNumber.matches(PHONE_PATTERN)) {
//                return "Invalid phone number format";
//            }
            // Remove leading zero if present and phone number doesn't start with '+'
            String normalizedNumber = phoneNumber;
            if (!phoneNumber.startsWith("+") && phoneNumber.startsWith("0")) {
                normalizedNumber = phoneNumber.substring(1);
            }

            Phonenumber.PhoneNumber parsedNumber = phoneNumberUtil.parse(normalizedNumber, "GH");

            if (!phoneNumberUtil.isValidNumber(parsedNumber)) {
                throw new ValidationException(INVALID_PHONE_NUMBER);
            }

            return phoneNumberUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164);

        } catch (NumberParseException e) {
            throw new ValidationException(INVALID_PHONE_NUMBER, e);
        }
    }

    public static String validatePassword(String value){
        if (value == null || value.isBlank()) {
            return PASSWORD_NOT_BLANK;
        }
        if (value.length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters";
        }
        if (value.length() > MAX_PASSWORD_LENGTH) {
            return "Password must not exceed " + MAX_PASSWORD_LENGTH + " characters";
        }
        if (!HAS_UPPERCASE.matcher(value).find()) {
            return "Password must contain an uppercase letter";
        }
        if (!HAS_LOWERCASE.matcher(value).find()) {
            return "Password must contain a lowercase letter";
        }
        if (!HAS_DIGIT.matcher(value).find()) {
            return "Password must contain a digit";
        }
        if (!HAS_SPECIAL.matcher(value).find()) {
            return "Password must contain a special character";
        }
        if (HAS_WHITESPACE.matcher(value).find()) {
            return "Password cannot contain whitespace";
        }
        return null;
    }

}
