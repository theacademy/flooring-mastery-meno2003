package Practice.FlooringMastery.View;

import Practice.FlooringMastery.Model.Order;
import Practice.FlooringMastery.Model.Product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class FlooringView {
    private static final DateTimeFormatter USER_DATE_FORMAT = DateTimeFormatter.ofPattern("MMddyyyy");
    private UserIO io;

    public FlooringView(UserIO io){
        this.io = io;
    }

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

    public String getCustomerName() {
        return io.readString("Please enter customer name: ");
    }

    public String getStateAbbreviation() {
        return io.readString("Please enter state abbreviation: ");
    }

    public String getProductType(List<Product> products) {
        io.print("Available products:");
        for (Product product : products) {
            io.print(product.getProductType() + " (Material: " + product.getCostPerSquareFoot()
                    + ", Labor: " + product.getLaborCostPerSquareFoot() + ")");
        }
        return io.readString("Please enter product type: ");
    }

    public BigDecimal getArea() {
        return io.readBigDecimal("Please enter area (min 100 sq ft): ");
    }

    public void displayOrderSummary(Order order) {
        io.print("Order #" + order.getOrderNumber() + " | Date: " + order.getOrderDate().format(USER_DATE_FORMAT));
        io.print("Customer: " + order.getCustomerName());
        io.print("State: " + order.getState() + " Tax Rate: " + order.getTaxRate());
        io.print("Product: " + order.getProductType() + " Area: " + order.getArea());
        io.print("Material: " + order.getMaterialCost() + " Labor: " + order.getLaborCost()
                + " Tax: " + order.getTax() + " Total: " + order.getTotal());
    }

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
