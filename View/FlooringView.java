/*
 * =============================================================================
 * CLASS: FlooringView
 * PACKAGE: View
 * =============================================================================
 * MVC ROLE: View — all console presentation; zero business logic and zero persistence.
 *
 * SPRING DI: XML bean id "view" with <constructor-arg ref="userIO"/> (or @Component + @Autowired).
 *
 * COMPOSITION: Has-a UserIO (injected) — dependency injection via constructor.
 *
 * DATE FORMAT: MMddyyyy for user prompts (matches order file naming convention).
 *
 * INTERVIEW EXPLANATION:
 * "FlooringView formats the UX. It might loop on bad date format, but it never decides
 *  if tax applies — it just displays what the Order DTO already contains."
 * =============================================================================
 */
package View;

import Model.Order;
import Model.Product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class FlooringView {

    /** User-facing date pattern — must match what users type and DAO file suffixes use. */
    private static final DateTimeFormatter USER_DATE_FORMAT = DateTimeFormatter.ofPattern("MMddyyyy");

    /** Injected I/O abstraction — loose coupling from Scanner/System.out details. */
    private UserIO io;

    /**
     * Constructor injection of UserIO — Spring supplies UserIOConsoleImpl (XML or annotations).
     *
     * @param io strategy for console read/write
     */
    public FlooringView(UserIO io){
        this.io = io;
    }

    /**
     * Displays main menu and returns validated selection 1–6.
     *
     * @return menu choice
     */
    public int printMenuAndGetSelection(){
        io.print("**********************************************************************");
        io.print("*  << Flooring Program >>");
        io.print("* 1. Display Order");
        io.print("* 2. Add an Order");
        io.print("* 3. Edit an Order");
        io.print("* 4. Remove an Order");
        io.print("* 5. Export All Data");
        io.print("* 6. Quit");
        io.print("*");
        io.print("***********************************************************************");

        int selection = io.readInt("Please select from the above choices",1,6);
        return selection;
    }

    public void displayDisplayOrdersBanner(){
        io.print("=== Display Orders ===");
    }

    /**
     * Loops until user enters parseable MMddyyyy date.
     * NOTE: Does NOT enforce "future only" — that is service.createOrderDraft for adds.
     */
    public LocalDate getOrderDate() {
        while (true) {
            String input = io.readString("Please enter an order date (MMddyyyy): ");
            try {
                return LocalDate.parse(input, USER_DATE_FORMAT);
            } catch (DateTimeParseException e) {
                io.print("Invalid date format.");
            }
        }
    }

    /**
     * Renders each Order via displayOrderSummary; pauses for Enter before returning to menu.
     *
     * @param orders list from service (may be empty)
     */
    public void displayOrders(List<Order> orders) {
        if (orders.isEmpty()) {
            io.print("No orders exist for that date.");
        } else {
            for (Order order : orders) {
                displayOrderSummary(order);
                io.print("------------------------------------------------------");
            }
        }
        io.readString("Please hit enter to continue.");
    }

    public void displayAddOrderBanner() {
        io.print("=== Add Order ===");
    }

    /** Raw customer name — regex validation happens in OrderServiceImpl. */
    public String getCustomerName() {
        return io.readString("Please enter customer name: ");
    }

    /** Raw state — supported-state check happens in service via TaxDAO. */
    public String getStateAbbreviation() {
        return io.readString("Please enter state abbreviation: ");
    }

    /**
     * Shows catalog from service.getAllProducts() then reads product type string.
     *
     * @param products injected from controller (service pass-through)
     */
    public String getProductType(List<Product> products) {
        io.print("Available products:");
        for (Product product : products) {
            io.print(product.getProductType() + " (Material: " + product.getCostPerSquareFoot()
                    + ", Labor: " + product.getLaborCostPerSquareFoot() + ")");
        }
        return io.readString("Please enter product type: ");
    }

    /**
     * Reads area as BigDecimal. Prompt mentions min 100; enforcement is in service.
     */
    public BigDecimal getArea() {
        return io.readBigDecimal("Please enter area (min 100 sq ft): ");
    }

    /** Presents calculated Order DTO fields — no math here, only display. */
    public void displayOrderSummary(Order order) {
        io.print("Order #" + order.getOrderNumber() + " | Date: " + order.getOrderDate().format(USER_DATE_FORMAT));
        io.print("Customer: " + order.getCustomerName());
        io.print("State: " + order.getState() + " Tax Rate: " + order.getTaxRate());
        io.print("Product: " + order.getProductType() + " Area: " + order.getArea());
        io.print("Material: " + order.getMaterialCost() + " Labor: " + order.getLaborCost()
                + " Tax: " + order.getTax() + " Total: " + order.getTotal());
    }

    /** Confirmation loop — only Y/N accepted (case insensitive). */
    public boolean confirm(String message) {
        while (true) {
            String answer = io.readString(message + " (Y/N): ");
            if ("Y".equalsIgnoreCase(answer)) {
                return true;
            }
            if ("N".equalsIgnoreCase(answer)) {
                return false;
            }
            io.print("Please enter Y or N.");
        }
    }

    public void displayEditOrderBanner() {
        io.print("=== Edit Order ===");
    }

    public int getOrderNumber() {
        return io.readInt("Please enter order number: ");
    }

    /** Empty line at edit means "keep existing" — interpreted in service, not view. */
    public String editCustomerName(String currentCustomerName) {
        return io.readString("Enter customer name (" + currentCustomerName + "): ");
    }

    public String editState(String currentState) {
        return io.readString("Enter state (" + currentState + "): ");
    }

    public String editProductType(String currentProductType, List<Product> products) {
        io.print("Available products:");
        for (Product product : products) {
            io.print(product.getProductType() + " (Material: " + product.getCostPerSquareFoot()
                    + ", Labor: " + product.getLaborCostPerSquareFoot() + ")");
        }
        return io.readString("Enter product type (" + currentProductType + "): ");
    }

    /**
     * Edit area: blank input returns null → service keeps existing area.
     * Invalid number prints message and returns null (keep existing).
     */
    public BigDecimal editArea(BigDecimal currentArea) {
        String response = io.readString("Enter area (" + currentArea + "): ");
        if (response == null || response.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(response.trim());
        } catch (NumberFormatException e) {
            io.print("Invalid decimal value. Keeping existing area.");
            return null;
        }
    }

    public void displayRemoveOrderBanner() {
        io.print("=== Remove Order ===");
    }

    public void displaySuccess(String message) {
        io.print(message);
        io.readString("Please hit enter to continue.");
    }

    public void displayErrorMessage(String message) {
        io.print("=== ERROR ===");
        io.print(message);
        io.readString("Please hit enter to continue.");
    }

    public void displayExitBanner() {
        io.print("Good Bye!!!");
    }

    public void displayUnknownCommandBanner() {
        io.print("Unknown Command!!!");
    }
}
