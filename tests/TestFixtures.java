package tests;

import Model.Order;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Shared test utilities — temp directories, sample data files, and order builders.
 */
final class TestFixtures {

    static final DateTimeFormatter ORDER_FILE_DATE = DateTimeFormatter.ofPattern("MMddyyyy");

    private TestFixtures() {
    }

    static File createTempDir(String name) {
        File dir = new File(System.getProperty("java.io.tmpdir"), "flooring-" + name + "-" + System.nanoTime());
        if (!dir.mkdirs()) {
            throw new RuntimeException("Could not create temp directory");
        }
        return dir;
    }

    static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    static void writeDefaultProducts(File file) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("ProductType,CostPerSquareFoot,LaborCostPerSquareFoot");
            out.println("Tile,3.50,4.15");
            out.println("Wood,5.15,4.75");
        }
    }

    static void writeDefaultTaxes(File file) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("StateAbbreviation,StateName,TaxRate");
            out.println("TX,Texas,10.00");
            out.println("CA,California,25.00");
        }
    }

    static void writeSampleOrders(File file, LocalDate date) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("OrderNumber,CustomerName,State,TaxRate,ProductType,Area,CostPerSquareFoot,LaborCostPerSquareFoot,MaterialCost,LaborCost,Tax,Total");
            out.println("1,\"Acme, Inc.\",TX,10.00,Tile,100.00,3.50,4.15,350.00,415.00,76.50,841.50");
            out.println("2,Ada Lovelace,CA,25.00,Wood,200.00,5.15,4.75,1030.00,950.00,495.00,2475.00");
        }
    }

    static File orderFileForDate(File ordersDir, LocalDate date) {
        return new File(ordersDir, "Orders_" + date.format(ORDER_FILE_DATE) + ".txt");
    }

    static Order buildPersistedOrder(int number, LocalDate date, String customer, String state,
                                     String product, BigDecimal area, BigDecimal taxRate,
                                     BigDecimal material, BigDecimal labor, BigDecimal tax, BigDecimal total) {
        Order order = new Order();
        order.setOrderNumber(number);
        order.setOrderDate(date);
        order.setCustomerName(customer);
        order.setState(state);
        order.setTaxRate(taxRate);
        order.setProductType(product);
        order.setArea(area);
        order.setCostPerSquareFoot(product.equals("Wood") ? new BigDecimal("5.15") : new BigDecimal("3.50"));
        order.setLaborCostPerSquareFoot(product.equals("Wood") ? new BigDecimal("4.75") : new BigDecimal("4.15"));
        order.setMaterialCost(material);
        order.setLaborCost(labor);
        order.setTax(tax);
        order.setTotal(total);
        return order;
    }

    static Order buildSimpleOrder(int number, LocalDate date, String customer, String state, String product) {
        return buildPersistedOrder(number, date, customer, state, product,
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                new BigDecimal("350.00"),
                new BigDecimal("415.00"),
                new BigDecimal("76.50"),
                new BigDecimal("841.50"));
    }
}
