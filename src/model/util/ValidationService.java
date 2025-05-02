package model.util;

import java.util.regex.Pattern;

/**
 * Utility-klasse med metoder til validering af input.
 */
public class ValidationService {
    // Regex patterns
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern VIA_ID_PATTERN =
            Pattern.compile("^[0-9]{4,8}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0-9]{8,12}$");

    /**
     * Validerer en email-adresse.
     *
     * @param email Email-adressen der skal valideres
     * @return true hvis valid, ellers false
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validerer et VIA ID.
     *
     * @param viaId VIA ID som streng
     * @return true hvis valid, ellers false
     */
    public static boolean isValidViaId(String viaId) {
        if (viaId == null || viaId.trim().isEmpty()) {
            return false;
        }
        return VIA_ID_PATTERN.matcher(viaId).matches();
    }

    /**
     * Validerer et VIA ID.
     *
     * @param viaId VIA ID som tal
     * @return true hvis valid, ellers false
     */
    public static boolean isValidViaId(int viaId) {
        return isValidViaId(String.valueOf(viaId));
    }

    /**
     * Validerer et telefonnummer.
     *
     * @param phoneNumber Telefonnummeret som streng
     * @return true hvis valid, ellers false
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phoneNumber).matches();
    }

    /**
     * Validerer et telefonnummer.
     *
     * @param phoneNumber Telefonnummeret som tal
     * @return true hvis valid, ellers false
     */
    public static boolean isValidPhoneNumber(int phoneNumber) {
        return isValidPhoneNumber(String.valueOf(phoneNumber));
    }

    /**
     * Validerer et laptopnavn (brand/model).
     *
     * @param name Navnet der skal valideres
     * @return true hvis valid, ellers false
     */
    public static boolean isValidLaptopName(String name) {
        return name != null && !name.trim().isEmpty() && name.length() <= 100;
    }

    /**
     * Validerer et gigabyte-tal.
     *
     * @param gigabyte Gigabyte-værdien
     * @return true hvis valid, ellers false
     */
    public static boolean isValidGigabyte(int gigabyte) {
        return gigabyte > 0 && gigabyte <= 4000;  // Max 4TB
    }

    /**
     * Validerer en RAM-værdi.
     *
     * @param ram RAM-værdien i GB
     * @return true hvis valid, ellers false
     */
    public static boolean isValidRam(int ram) {
        return ram > 0 && ram <= 128;  // Max 128GB
    }

    /**
     * Validerer et student-/personannavn.
     *
     * @param name Navnet der skal valideres
     * @return true hvis valid, ellers false
     */
    public static boolean isValidPersonName(String name) {
        return name != null && !name.trim().isEmpty() && name.length() <= 100;
    }

    /**
     * Validerer et uddannelsesnavn.
     *
     * @param degreeTitle Uddannelsesnavnet
     * @return true hvis valid, ellers false
     */
    public static boolean isValidDegreeTitle(String degreeTitle) {
        return degreeTitle != null && !degreeTitle.trim().isEmpty() && degreeTitle.length() <= 100;
    }
}