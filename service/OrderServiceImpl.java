package Practice.FlooringMastery.service;

import Practice.FlooringMastery.DAO.OrderDAO;
import Practice.FlooringMastery.DAO.FlooringPersistenceException;
import Practice.FlooringMastery.DAO.ProductDAO;
import Practice.FlooringMastery.DAO.TaxDAO;
import Practice.FlooringMastery.Model.Order;
import Practice.FlooringMastery.Model.Product;
import Practice.FlooringMastery.Model.Tax;

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

    private static final Pattern CUSTOMER_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9., ]+$");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal MIN_AREA = new BigDecimal("100");
    private static final DateTimeFormatter EXPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private static final String EXPORT_HEADER = "OrderNumber,CustomerName,State,TaxRate,ProductType,Area,CostPerSquareFoot,LaborCostPerSquareFoot,MaterialCost,LaborCost,Tax,Total,OrderDate";
    private static final String DEFAULT_BACKUP_FOLDER = "Backup";

    private final OrderDAO orderDao;
    private final ProductDAO productDao;
    private final TaxDAO taxDao;
    private final String backupFolderPath;


    public OrderServiceImpl(OrderDAO orderDao, ProductDAO productDao, TaxDAO taxDao){
        this(orderDao, productDao, taxDao, DEFAULT_BACKUP_FOLDER);
    }

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
        order.setCostPerSquareFoot(scale2(product.getCostPerSquareFoot()));
        order.setLaborCostPerSquareFoot(scale2(product.getLaborCostPerSquareFoot()));

        BigDecimal materialCost = area.multiply(product.getCostPerSquareFoot());
        BigDecimal laborCost = area.multiply(product.getLaborCostPerSquareFoot());
        BigDecimal taxAmount = materialCost.add(laborCost).multiply(tax.getTaxRate().divide(HUNDRED, 4, RoundingMode.HALF_UP));
        BigDecimal total = materialCost.add(laborCost).add(taxAmount);

        order.setMaterialCost(scale2(materialCost));
        order.setLaborCost(scale2(laborCost));
        order.setTax(scale2(taxAmount));
        order.setTotal(scale2(total));
        return order;
    }

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
        if (area == null || area.compareTo(MIN_AREA) < 0) {
            throw new FlooringValidationException("Area must be at least 100 sq ft.");
        }
    }

    private void validateOrderForSave(Order order) throws FlooringValidationException {
        if (order == null || order.getOrderDate() == null || order.getOrderNumber() == null) {
            throw new FlooringValidationException("Order is incomplete and cannot be saved.");
        }
    }

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

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private BigDecimal scale2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
