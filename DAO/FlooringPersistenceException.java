/*
 * =============================================================================
 * CLASS: FlooringPersistenceException
 * PACKAGE: DAO
 * =============================================================================
 * WHAT: Checked exception for file I/O and persistence-layer failures.
 *
 * WHY: DAO methods should not leak raw java.io.IOException to the service/controller —
 *      wrapping preserves abstraction and allows consistent error messages.
 *
 * EXCEPTION HANDLING: Service methods declare throws FlooringPersistenceException;
 *                     controller catches and shows message via view.
 *
 * INTERVIEW EXPLANATION:
 * "The DAO layer owns persistence. If Products.txt is missing, the DAO throws
 *  FlooringPersistenceException — the service propagates it, the controller displays it."
 * =============================================================================
 */
package DAO;

/**
 * Signals that reading or writing flooring data files failed.
 */
public class FlooringPersistenceException extends Exception {

    /**
     * Message-only constructor for persistence errors without an underlying cause.
     *
     * @param message user- or log-friendly description
     */
    public FlooringPersistenceException(String message) {
        super(message);
    }

    /**
     * Wraps a lower-level I/O exception while preserving the stack trace (chained cause).
     *
     * @param message high-level description
     * @param cause   original IOException or similar
     */
    public FlooringPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
