/*
 * =============================================================================
 * CLASS: OrderDAOImpl
 * PACKAGE: DAO
 * =============================================================================
 * WHAT: Persists orders as one CSV file per order date: Orders/Orders_MMddyyyy.txt
 *
 * PATTERN: DAO — marshall/unmarshall Order DTO ↔ disk lines.
 *
 * COLLECTIONS:
 *   - HashMap<Integer, Order> per date during load/modify/write cycle
 *   - ArrayList for sorted output lists
 *   - Comparator for sorting by order number or date+number
 *
 * FILE I/O: read with Scanner; write with PrintWriter; try-with-resources on write path.
 *
 * STATELESS SERVICE STYLE: Each public method loads map, mutates, writes — no cross-request cache.
 *
 * SPRING DI: Wired in config/applicationContext.xml as bean id "orderDao" (XML configuration).
 *            For annotation-based config, add @Repository and use AppConfig + @ComponentScan.
 *
 * INTERVIEW EXPLANATION:
 * "OrderDAOImpl treats each date file as the database table. We load the whole file into a Map,
 *  apply CRUD in memory, then rewrite the file — acceptable for small training datasets."
 * =============================================================================
 */
package DAO;

import Model.Order;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class OrderDAOImpl implements OrderDAO {

    private static final String DEFAULT_ORDERS_FOLDER = "Orders";
    private static final String HEADER = "OrderNumber,CustomerName,State,TaxRate,ProductType,Area,CostPerSquareFoot,LaborCostPerSquareFoot,MaterialCost,LaborCost,Tax,Total";
    private static final String DELIMITER = ",";
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("MMddyyyy");

    /** Configurable folder — tests inject temp directory path. */
    private final String ordersFolder;

    public OrderDAOImpl() {
        this(DEFAULT_ORDERS_FOLDER);
    }

    public OrderDAOImpl(String ordersFolder) {
        this.ordersFolder = ordersFolder;
    }

    @Override
    public List<Order> getOrdersByDate(LocalDate date) throws FlooringPersistenceException {
        Map<Integer, Order> orders = loadOrders(date);
        List<Order> orderList = new ArrayList<>(orders.values());
        orderList.sort(Comparator.comparing(Order::getOrderNumber));
        return orderList;
    }

    @Override
    public Order getOrder(LocalDate date, int orderNumber) throws FlooringPersistenceException {
        return loadOrders(date).get(orderNumber);
    }

    @Override
    public Order addOrder(LocalDate date, Order order) throws FlooringPersistenceException {
        Map<Integer, Order> orders = loadOrders(date);
        // Map.put returns previous value at key — may be null for new order number.
        Order previous = orders.put(order.getOrderNumber(), order);
        writeOrders(date, orders);
        return previous;
    }

    @Override
    public Order updateOrder(LocalDate date, Order order) throws FlooringPersistenceException {
        Map<Integer, Order> orders = loadOrders(date);
        Order previous = orders.put(order.getOrderNumber(), order);
        writeOrders(date, orders);
        return previous;
    }

    @Override
    public Order removeOrder(LocalDate date, int orderNumber) throws FlooringPersistenceException {
        Map<Integer, Order> orders = loadOrders(date);
        Order removed = orders.remove(orderNumber);
        writeOrders(date, orders);
        return removed;
    }

    @Override
    public int getNextOrderNumber(LocalDate date) throws FlooringPersistenceException {
        // STREAM API: max order number + 1, or 1 if no orders (orElse(0)+1).
        return loadOrders(date).keySet().stream().max(Integer::compareTo).orElse(0) + 1;
    }

    @Override
    public List<Order> getAllOrders() throws FlooringPersistenceException {
        File folder = new File(ordersFolder);
        List<Order> allOrders = new ArrayList<>();
        if (!folder.exists()) {
            return allOrders;
        }
        File[] files = folder.listFiles((dir, name) -> name.startsWith("Orders_") && name.endsWith(".txt"));
        if (files == null) {
            return allOrders;
        }
        for (File file : files) {
            LocalDate date = parseDateFromFileName(file.getName());
            if (date != null) {
                allOrders.addAll(getOrdersByDate(date));
            }
        }
        allOrders.sort(Comparator.comparing(Order::getOrderDate).thenComparing(Order::getOrderNumber));
        return allOrders;
    }

    /**
     * Loads one date file into HashMap keyed by order number.
     * Missing file → empty map (not an error — first order for that date).
     */
    private Map<Integer, Order> loadOrders(LocalDate date) throws FlooringPersistenceException {
        Map<Integer, Order> orders = new HashMap<>();
        File file = new File(getFileName(date));
        if (!file.exists()) {
            return orders;
        }
        try (Scanner scanner = new Scanner(new BufferedReader(new FileReader(file)))) {
            if (scanner.hasNextLine()) {
                scanner.nextLine(); // skip HEADER row
            }
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                Order order = unmarshallOrder(line, date);
                orders.put(order.getOrderNumber(), order);
            }
        } catch (FileNotFoundException e) {
            throw new FlooringPersistenceException("Could not load orders for " + date + ".", e);
        }
        return orders;
    }

    /** Rewrites entire date file from in-memory map (full-file rewrite strategy). */
    private void writeOrders(LocalDate date, Map<Integer, Order> orders) throws FlooringPersistenceException {
        File folder = new File(ordersFolder);
        if (!folder.exists() && !folder.mkdirs()) {
            throw new FlooringPersistenceException("Could not create Orders folder.");
        }
        try (PrintWriter out = new PrintWriter(new FileWriter(getFileName(date)))) {
            out.println(HEADER);
            List<Order> list = new ArrayList<>(orders.values());
            list.sort(Comparator.comparing(Order::getOrderNumber));
            for (Order order : list) {
                out.println(marshallOrder(order));
            }
        } catch (IOException e) {
            throw new FlooringPersistenceException("Could not write orders for " + date + ".", e);
        }
    }

    /** Builds path Orders/Orders_MMddyyyy.txt */
    private String getFileName(LocalDate date) {
        return ordersFolder + "/Orders_" + date.format(FILE_DATE_FORMAT) + ".txt";
    }

    private LocalDate parseDateFromFileName(String fileName) {
        try {
            String datePart = fileName.replace("Orders_", "").replace(".txt", "");
            return LocalDate.parse(datePart, FILE_DATE_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * CSV → Order DTO. Order date comes from filename, not row (row has no date column in storage file).
     * Supports quoted customer names with commas via parseCsvLine.
     */
    private Order unmarshallOrder(String line, LocalDate date) {
        List<String> tokens = parseCsvLine(line);
        Order order = new Order();
        order.setOrderDate(date);
        order.setOrderNumber(Integer.parseInt(tokens.get(0)));
        order.setCustomerName(tokens.get(1));
        order.setState(tokens.get(2));
        order.setTaxRate(new BigDecimal(tokens.get(3)));
        order.setProductType(tokens.get(4));
        order.setArea(new BigDecimal(tokens.get(5)));
        order.setCostPerSquareFoot(new BigDecimal(tokens.get(6)));
        order.setLaborCostPerSquareFoot(new BigDecimal(tokens.get(7)));
        order.setMaterialCost(new BigDecimal(tokens.get(8)));
        order.setLaborCost(new BigDecimal(tokens.get(9)));
        order.setTax(new BigDecimal(tokens.get(10)));
        order.setTotal(new BigDecimal(tokens.get(11)));
        return order;
    }

    /** Order DTO → CSV data line (no date column in per-date persistence file). */
    private String marshallOrder(Order order) {
        return order.getOrderNumber() + DELIMITER
                + escapeCsv(order.getCustomerName()) + DELIMITER
                + order.getState() + DELIMITER
                + order.getTaxRate() + DELIMITER
                + order.getProductType() + DELIMITER
                + order.getArea() + DELIMITER
                + order.getCostPerSquareFoot() + DELIMITER
                + order.getLaborCostPerSquareFoot() + DELIMITER
                + order.getMaterialCost() + DELIMITER
                + order.getLaborCost() + DELIMITER
                + order.getTax() + DELIMITER
                + order.getTotal();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Custom CSV parser for quoted fields — needed for customer names like "Acme, Inc."
     * STATEFUL loop: tracks inQuotes flag while scanning characters.
     */
    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        result.add(current.toString());
        return result;
    }
}
