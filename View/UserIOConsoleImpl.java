/*
 * =============================================================================
 * CLASS: UserIOConsoleImpl
 * PACKAGE: View
 * =============================================================================
 * WHAT: Concrete {@link UserIO} implementation using System.in/out and java.util.Scanner.
 *
 * SPRING DI: XML bean id "userIO" in applicationContext.xml (or @Component with AppConfig).
 *
 * PATTERN: Polymorphism — implements UserIO interface; injected into FlooringView.
 *
 * STATEFUL: Holds one Scanner bound to stdin for the life of the application.
 *
 * VALIDATION: Only format validation (parseable int/BigDecimal, menu range). Business
 *             rules (min area 100, future dates) are NOT enforced here.
 *
 * INTERVIEW EXPLANATION:
 * "UserIOConsoleImpl is the adapter between the view and the physical console. Swapping
 *  this class is how you'd unit-test the view without a human typing."
 * =============================================================================
 */
package View;

import java.math.BigDecimal;
import java.util.Scanner;

public class UserIOConsoleImpl implements UserIO {

    /** Stateful scanner — reads lines from standard input until program exits. */
    private Scanner scanner = new Scanner(System.in);

    @Override
    public void print(String msg) {
        System.out.println(msg);
    }

    @Override
    public String readString(String prompt) {
        print(prompt);
        return scanner.nextLine();
    }

    @Override
    public int readInt(String prompt) {
        while (true) {
            print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                print("Invalid number. Please try again.");
            }
        }
    }

    @Override
    public int readInt(String prompt, int min, int max) {
        int userInput;
        while (true) {
            print(prompt);
            try {
                userInput = Integer.parseInt(scanner.nextLine());
                if (userInput >= min && userInput <= max) {
                    return userInput;
                }
                // Re-prompt if out of range (menu must be 1-6).
                print(prompt);
            } catch (NumberFormatException e) {
                print("Invalid number. Please try again.");
            }
        }
    }

    @Override
    public BigDecimal readBigDecimal(String prompt) {
        while (true) {
            print(prompt);
            try {
                // BigDecimal preferred over double for area — matches service layer type.
                return new BigDecimal(scanner.nextLine());
            } catch (NumberFormatException e) {
                print("Invalid decimal value. Please try again.");
            }
        }
    }
}
