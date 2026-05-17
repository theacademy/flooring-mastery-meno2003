package Practice.FlooringMastery.View;

import java.math.BigDecimal;
import java.util.Scanner;

public class UserIOConsoleImpl implements UserIO {

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
                return new BigDecimal(scanner.nextLine());
            } catch (NumberFormatException e) {
                print("Invalid decimal value. Please try again.");
            }
        }

    }
}
