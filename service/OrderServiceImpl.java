/*
 * =============================================================================
 * CLASS: OrderServiceImpl
 * PACKAGE: service
 * =============================================================================
 * WHAT: Concrete service layer — validation, pricing, tax, export orchestration.
 *
 * IMPLEMENTS: OrderService (polymorphism — controller depends on interface).
 *
 * SPRING DI: XML bean id "orderService" receives orderDao, productDao, taxDao via <constructor-arg>.
 *            Constructor parameters use interface types (programming to interfaces).
 *            Annotation alternative: @Service + @Autowired on the three-arg constructor + AppConfig.
 *
 * DEPENDENCIES (constructor injection — three DAO interfaces + optional backup path):
 *   OrderDAO, ProductDAO, TaxDAO — aggregation/injection; service does not new DAOs.
 *
 * STATE: Effectively stateless per request except injected DAO references and backup path.
 *        Product/Tax DAOs cache maps internally (stateful DAO — see ProductDAOImpl).
 *
 * PATTERNS: Service Layer, DTO population, BigDecimal money math.
 *
 * INTERVIEW EXPLANATION:
 * "OrderServiceImpl is the brain — every rubric rule about dates, names, area, tax, and
 *  totals lives here or in private helpers, not in the controller or DAO."
 * =============================================================================
 */
package service;

import DAO.OrderDAO;
import DAO.FlooringPersistenceException;
import DAO.ProductDAO;
import DAO.TaxDAO;
import Model.Order;
import Model.Product;
import Model.Tax;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

public class OrderServiceImpl implements OrderService{

    /**
     * BUSINESS RULE: Customer name may only contain letters, digits, spaces, period, comma.
     * OOP: Encapsulated as constant — single source of truth for validation regex.
     */
    private static final Pattern CUSTOMER_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9., ]+$");

    /** Used to convert tax RATE percent (e.g. 10.00) into multiplier (0.10) for tax dollars. */
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** BUSINESS RULE: Minimum order area in square feet (rubric). */
    private static final BigDecimal MIN_AREA = new BigDecimal("100");

    /** Export file uses MM-dd-yyyy per spec (different from persistence MMddyyyy). */
    private static final DateTimeFormatter EXPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private static final String EXPORT_HEADER = "OrderNumber,CustomerName,State,TaxRate,ProductType,Area,CostPerSquareFoot,LaborCostPerSquareFoot,MaterialCost,LaborCost,Tax,Total,OrderDate";
    private static final String DEFAULT_BACKUP_FOLDER = "Backup";

    /** Injected persistence for orders — interface = loose coupling. */
    private final OrderDAO orderDao;
    private final ProductDAO productDao;
    private final TaxDAO taxDao;
    private final String backupFolderPath;

    /**
     * Constructor injection: Spring XML (or @Autowired) supplies the three DAO implementations.
     */
    public OrderServiceImpl(OrderDAO orderDao, ProductDAO productDao, TaxDAO taxDao){
        this(orderDao, productDao, taxDao, DEFAULT_BACKUP_FOLDER);
    }

    /**
     * Full constructor injection — tests pass custom backup path for export assertions (not used by Spring).
     */
    public OrderServiceImpl(OrderDAO orderDao, ProductDAO productDao, TaxDAO taxDao, String backupFolderPath){
        this.orderDao = orderDao;
        this.productDao = productDao;
        this.taxDao = taxDao;
        this.backupFolderPath = backupFolderPath;
    }

    @Override
    public List<Order> displayOrders(LocalDate orderDate) throws FlooringPersistenceException {
        return orderDao.getOrdersByDate(orderDate);
    }

    @Override
    public Order createOrderDraft(LocalDate orderDate, String customerName, String state, String productType, BigDecimal area)
            throws FlooringPersistenceException, FlooringValidationException {

        // --- FUTURE DATE VALIDATION (add orders only) ---
        // BUSINESS RULE: orderDate must be AFTER today (today is NOT allowed).
        // isAfter(now) is false for today and past → we reject.
        if (!orderDate.isAfter(LocalDate.now())) {
            throw new FlooringValidationException("Order date must be in the future.");
        }

        validateEditableInputs(customerName, state, productType, area);
        Order order = buildCalculatedOrder(orderDate, customerName, state, productType, area);
        order.setOrderNumber(orderDao.getNextOrderNumber(orderDate));
        return order;
    }

    @Override
    public Order addOrder(Order order) throws FlooringPersistenceException, FlooringValidationException {
        validateOrderForSave(order);
        return orderDao.addOrder(order.getOrderDate(), order);
    }

    @Override
    public Order editOrderDraft(LocalDate orderDate, int orderNumber, String customerName, String state, String productType, BigDecimal area)
            throws FlooringPersistenceException, FlooringValidationException {
        Order existing = orderDao.getOrder(orderDate, orderNumber);
        if (existing == null) {
            throw new FlooringValidationException("Order not found.");
        }

        // PARTIAL UPDATE: blank/null means "keep existing field" (edit UX rubric).
        String newCustomer = isBlank(customerName) ? existing.getCustomerName() : customerName;
        String newState = isBlank(state) ? existing.getState() : state;
        String newProductType = isBlank(productType) ? existing.getProductType() : productType;
        BigDecimal newArea = area == null ? existing.getArea() : area;

        validateEditableInputs(newCustomer, newState, newProductType, newArea);
        Order updated = buildCalculatedOrder(orderDate, newCustomer, newState, newProductType, newArea);
        updated.setOrderNumber(existing.getOrderNumber());
        return updated;
    }

    @Override
    public Order saveEditedOrder(Order order) throws FlooringPersistenceException, FlooringValidationException {
        validateOrderForSave(order);
        return orderDao.updateOrder(order.getOrderDate(), order);
    }

    @Override
    public Order getOrder(LocalDate orderDate, int orderNumber) throws FlooringPersistenceException {
        return orderDao.getOrder(orderDate, orderNumber);
    }

    @Override
    public Order removeOrder(LocalDate orderDate, int orderNumber) throws FlooringPersistenceException {
        return orderDao.removeOrder(orderDate, orderNumber);
    }

    @Override
    public void exportAllData() throws FlooringPersistenceException {
        List<Order> allOrders = orderDao.getAllOrders();
        File backupFolder = new File(backupFolderPath);
        if (!backupFolder.exists() && !backupFolder.mkdirs()) {
            throw new FlooringPersistenceException("Could not create backup folder.");
        }
        // FILE I/O: try-with-resources ensures PrintWriter closes even on error.
        try (PrintWriter out = new PrintWriter(new FileWriter(new File(backupFolder, "DataExport.txt")))) {
            out.println(EXPORT_HEADER);
            for (Order order : allOrders) {
                out.println(toExportRow(order));
            }
        } catch (IOException e) {
            throw new FlooringPersistenceException("Could not export all order data.", e);
        }
    }

    @Override
    public List<Product> getAllProducts() throws FlooringPersistenceException {
        return productDao.getAllProducts();
    }

    @Override
    public List<Tax> getAllTaxes() throws FlooringPersistenceException {
        return taxDao.getAllTaxes();
    }

    /**
     * Core calculation pipeline: lookup product & tax → populate Order DTO → compute costs.
     *
     * CALCULATION FORMULAS (rubric):
     *   materialCost = area × costPerSquareFoot
     *   laborCost    = area × laborCostPerSquareFoot
     *   tax          = (materialCost + laborCost) × (taxRate / 100)
     *   total        = materialCost + laborCost + tax
     *
     * BigDecimal: multiply/divide with explicit scale on final stored fields (scale2).
     */
    private Order buildCalculatedOrder(LocalDate date, String customerName, String state, String productType, BigDecimal area)
            throws FlooringPersistenceException, FlooringValidationException {
        Product product = productDao.getProduct(productType);
        if (product == null) {
            throw new FlooringValidationException("Invalid product type.");
        }
        Tax tax = taxDao.getTax(state);
        if (tax == null) {
            throw new FlooringValidationException("State is not currently supported.");
        }
        Order order = new Order();
        order.setOrderDate(date);
        order.setCustomerName(customerName.trim());
        order.setState(tax.getStateAbbreviation());
        order.setTaxRate(scale2(tax.getTaxRate()));
        order.setProductType(product.getProductType());
        order.setArea(scale2(area));
        // Snapshot rates onto order so file row is self-contained if Products.txt changes later.
        order.setCostPerSquareFoot(scale2(product.getCostPerSquareFoot()));
        order.setLaborCostPerSquareFoot(scale2(product.getLaborCostPerSquareFoot()));

        // --- MATERIAL & LABOR (before tax) ---
        BigDecimal materialCost = area.multiply(product.getCostPerSquareFoot());
        BigDecimal laborCost = area.multiply(product.getLaborCostPerSquareFoot());

        // --- TAX: rate is percent; divide by 100 with scale 4 intermediate, then multiply subtotal ---
        BigDecimal taxAmount = materialCost.add(laborCost)
                .multiply(tax.getTaxRate().divide(HUNDRED, 4, RoundingMode.HALF_UP));

        BigDecimal total = materialCost.add(laborCost).add(taxAmount);

        order.setMaterialCost(scale2(materialCost));
        order.setLaborCost(scale2(laborCost));
        order.setTax(scale2(taxAmount));
        order.setTotal(scale2(total));
        return order;
    }

    /**
     * Shared validation for add and edit after merge of blank fields.
     */
    private void validateEditableInputs(String customerName, String state, String productType, BigDecimal area)
            throws FlooringValidationException {
        if (isBlank(customerName)) {
            throw new FlooringValidationException("Customer name is required.");
        }
        if (!CUSTOMER_NAME_PATTERN.matcher(customerName).matches()) {
            throw new FlooringValidationException("Customer name can only use letters, digits, spaces, periods and commas.");
        }
        if (isBlank(state)) {
            throw new FlooringValidationException("State is required.");
        }
        if (isBlank(productType)) {
            throw new FlooringValidationException("Product type is required.");
        }
        // MIN AREA: compareTo < 0 means area strictly less than 100 is rejected.
        if (area == null || area.compareTo(MIN_AREA) < 0) {
            throw new FlooringValidationException("Area must be at least 100 sq ft.");
        }
    }

    /** Ensures order has identity before DAO write. */
    private void validateOrderForSave(Order order) throws FlooringValidationException {
        if (order == null || order.getOrderDate() == null || order.getOrderNumber() == null) {
            throw new FlooringValidationException("Order is incomplete and cannot be saved.");
        }
    }

    /** CSV row for export — includes formatted order date column. */
    private String toExportRow(Order order) {
        return order.getOrderNumber() + ","
                + escapeCsv(order.getCustomerName()) + ","
                + order.getState() + ","
                + order.getTaxRate() + ","
                + order.getProductType() + ","
                + order.getArea() + ","
                + order.getCostPerSquareFoot() + ","
                + order.getLaborCostPerSquareFoot() + ","
                + order.getMaterialCost() + ","
                + order.getLaborCost() + ","
                + order.getTax() + ","
                + order.getTotal() + ","
                + order.getOrderDate().format(EXPORT_DATE_FORMAT);
    }

    /** Escapes commas/quotes in customer name for CSV integrity. */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** Currency-style half-up rounding to 2 decimal places. */
    private BigDecimal scale2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
