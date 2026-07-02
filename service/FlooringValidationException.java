/*
 * =============================================================================
 * CLASS: FlooringValidationException
 * PACKAGE: service
 * =============================================================================
 * WHAT: Checked exception representing a business-rule violation (validation failure).
 *
 * WHY: Separates "user/data rule problems" from persistence failures
 *      (FlooringPersistenceException in DAO package). Controller can catch both and
 *      display messages without crashing the menu loop.
 *
 * PATTERN: Custom checked exception — common in layered enterprise-style Java taught
 *          in GenAI Java courses before moving to unchecked domain exceptions.
 *
 * INTERVIEW EXPLANATION:
 * "Validation exceptions are expected failures — bad date, unknown state, area under 100.
 *  I use a dedicated type so the controller knows not to blame the file system."
 * =============================================================================
 */
package service;

/**
 * Thrown when order input or state violates Flooring Mastery business rules.
 * Extends {@link Exception} (checked) so callers must handle or declare it.
 */
public class FlooringValidationException extends Exception {

    /**
     * Creates a validation exception with a user-facing message.
     *
     * @param message explanation shown in the view error banner
     */
    public FlooringValidationException(String message) {
        super(message);
    }
}
