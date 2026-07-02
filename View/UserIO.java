/*
 * =============================================================================
 * INTERFACE: UserIO
 * PACKAGE: View
 * =============================================================================
 * WHAT: Abstraction for all console input/output operations (print, read String/int/BigDecimal).
 *
 * WHY (Abstraction + Polymorphism): FlooringView should not hard-code System.in/out so you
 *      could substitute a test double or different UI without changing view logic.
 *
 * PATTERN: Strategy / interface segregation — minimal I/O contract.
 *
 * DEPENDENCY RELATIONSHIP: FlooringView depends on UserIO (composition via constructor injection).
 *                         Dependency type: interface (loose coupling).
 *
 * INTERVIEW EXPLANATION:
 * "UserIO is the seam between presentation formatting and actual console reads. In tests
 *  you could provide a fake UserIO that returns scripted answers."
 * =============================================================================
 */
package View;

import java.math.BigDecimal;

public interface UserIO {

    /** Writes a line to the user (console implementation uses println). */
    void print(String msg);

    /** Prompts and returns a full line of text from the user. */
    String readString(String prompt);

    /** Prompts until the user enters a valid integer. */
    int readInt(String prompt);

    /**
     * Prompts until the user enters an integer within [min, max] inclusive.
     * Used for main menu selection (1–6).
     */
    int readInt(String prompt, int min, int max);

    /**
     * Prompts until the user enters a valid {@link BigDecimal} (area input).
     * BUSINESS NOTE: Min area (100) is enforced in service, not here.
     */
    BigDecimal readBigDecimal(String prompt);
}
