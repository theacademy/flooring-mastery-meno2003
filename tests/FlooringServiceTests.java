package tests;

import DAO.FlooringPersistenceException;
import DAO.OrderDAO;
import DAO.OrderDAOImpl;
import DAO.ProductDAO;
import DAO.ProductDAOImpl;
import DAO.TaxDAO;
import DAO.TaxDAOImpl;
import Model.Order;
import Model.Product;
import Model.Tax;
import service.FlooringValidationException;
import service.OrderService;
import service.OrderServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

@DisplayName("Flooring Service Tests")
public class FlooringServiceTests {

    private File tempRoot;
    private File dataDir;
    private File ordersDir;
    private File backupDir;
    private OrderService service;
    private LocalDate futureDate;

    @BeforeEach
    public void setUp() throws Exception {
        tempRoot = TestFixtures.createTempDir("service-tests");
        dataDir = new File(tempRoot, "Data");
        ordersDir = new File(tempRoot, "Orders");
        backupDir = new File(tempRoot, "Backup");
        dataDir.mkdirs();
        ordersDir.mkdirs();
        backupDir.mkdirs();

        TestFixtures.writeDefaultProducts(new File(dataDir, "Products.txt"));
        TestFixtures.writeDefaultTaxes(new File(dataDir, "Taxes.txt"));

        OrderDAO orderDao = new OrderDAOImpl(ordersDir.getPath());
        ProductDAO productDao = new ProductDAOImpl(new File(dataDir, "Products.txt").getPath());
        TaxDAO taxDao = new TaxDAOImpl(new File(dataDir, "Taxes.txt").getPath());
        service = new OrderServiceImpl(orderDao, productDao, taxDao, backupDir.getPath());
        futureDate = LocalDate.now().plusDays(5);
    }

    @AfterEach
    public void tearDown() {
        TestFixtures.deleteRecursively(tempRoot);
    }

    // --- Date validation (add only) ---

    @Test
    @DisplayName("Negative - Same-day date is rejected on create")
    public void sameDayDateRejectedTest() {
        Assertions.assertThrows(FlooringValidationException.class, () ->
                service.createOrderDraft(LocalDate.now(), "Acme, Inc.", "TX", "Tile", new BigDecimal("100")));
    }

    @Test
    @DisplayName("Negative - Past date is rejected on create")
    public void pastDateRejectedTest() {
        Assertions.assertThrows(FlooringValidationException.class, () ->
                service.createOrderDraft(LocalDate.now().minusDays(1), "Acme, Inc.", "TX", "Tile", new BigDecimal("100")));
    }

    @Test
    @DisplayName("Positive - Future date is accepted on create")
    public void futureDateAcceptedTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Acme, Inc.", "TX", "Tile", new BigDecimal("100"));
        Assertions.assertEquals(futureDate, draft.getOrderDate());
    }

    // --- Calculations ---

    @Test
    @DisplayName("Positive - Tile TX 100 sq ft calculations")
    public void tileTexasCalculationTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Acme, Inc.", "TX", "Tile", new BigDecimal("100"));
        Assertions.assertEquals(1, draft.getOrderNumber());
        assertMoneyEquals("350.00", draft.getMaterialCost());
        assertMoneyEquals("415.00", draft.getLaborCost());
        assertMoneyEquals("76.50", draft.getTax());
        assertMoneyEquals("841.50", draft.getTotal());
        assertMoneyEquals("10.00", draft.getTaxRate());
    }

    @Test
    @DisplayName("Positive - Wood TX 100 sq ft calculations")
    public void woodTexasCalculationTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Customer", "TX", "Wood", new BigDecimal("100"));
        assertMoneyEquals("515.00", draft.getMaterialCost());
        assertMoneyEquals("475.00", draft.getLaborCost());
        assertMoneyEquals("99.00", draft.getTax());
        assertMoneyEquals("1089.00", draft.getTotal());
    }

    @Test
    @DisplayName("Positive - Tile CA 100 sq ft uses 25% tax rate")
    public void californiaTaxCalculationTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Customer", "CA", "Tile", new BigDecimal("100"));
        assertMoneyEquals("25.00", draft.getTaxRate());
        assertMoneyEquals("191.25", draft.getTax());
        assertMoneyEquals("956.25", draft.getTotal());
    }

    @Test
    @DisplayName("Positive - Product lookup is case-insensitive in service")
    public void productCaseInsensitiveInServiceTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Customer", "TX", "tile", new BigDecimal("100"));
        Assertions.assertEquals("Tile", draft.getProductType());
    }

    // --- Customer name validation ---

    @Test
    @DisplayName("Negative - Blank customer name is rejected")
    public void blankCustomerNameTest() {
        Assertions.assertThrows(FlooringValidationException.class, () ->
                service.createOrderDraft(futureDate, "   ", "TX", "Tile", new BigDecimal("100")));
    }

    @Test
    @DisplayName("Negative - Customer name with invalid characters is rejected")
    public void invalidCustomerNameTest() {
        Assertions.assertThrows(FlooringValidationException.class, () ->
                service.createOrderDraft(futureDate, "Bad@Name!", "TX", "Tile", new BigDecimal("100")));
    }

    @Test
    @DisplayName("Positive - Customer name allows letters digits spaces comma period")
    public void validCustomerNameTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Acme, Inc. 2026", "TX", "Tile", new BigDecimal("100"));
        Assertions.assertEquals("Acme, Inc. 2026", draft.getCustomerName());
    }

    // --- State / product validation ---

    @Test
    @DisplayName("Negative - Blank state is rejected")
    public void blankStateTest() {
        Assertions.assertThrows(FlooringValidationException.class, () ->
                service.createOrderDraft(futureDate, "Customer", "", "Tile", new BigDecimal("100")));
    }

    @Test
    @DisplayName("Negative - Unsupported state is rejected")
    public void unsupportedStateTest() {
        Assertions.assertThrows(FlooringValidationException.class, () ->
                service.createOrderDraft(futureDate, "Customer", "ZZ", "Tile", new BigDecimal("100")));
    }

    @Test
    @DisplayName("Negative - Blank product type is rejected")
    public void blankProductTypeTest() {
        Assertions.assertThrows(FlooringValidationException.class, () ->
                service.createOrderDraft(futureDate, "Customer", "TX", "  ", new BigDecimal("100")));
    }

    @Test
    @DisplayName("Negative - Invalid product type is rejected")
    public void invalidProductTypeTest() {
        Assertions.assertThrows(FlooringValidationException.class, () ->
                service.createOrderDraft(futureDate, "Customer", "TX", "Marble", new BigDecimal("100")));
    }

    // --- Area validation ---

    @Test
    @DisplayName("Negative - Area below 100 sq ft is rejected")
    public void areaBelowMinimumTest() {
        Assertions.assertThrows(FlooringValidationException.class, () ->
                service.createOrderDraft(futureDate, "Customer", "TX", "Tile", new BigDecimal("99.99")));
    }

    @Test
    @DisplayName("Positive - Area exactly 100 sq ft is accepted")
    public void areaExactlyMinimumTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Customer", "TX", "Tile", new BigDecimal("100"));
        assertMoneyEquals("100.00", draft.getArea());
    }

    @Test
    @DisplayName("Positive - Area above minimum is accepted")
    public void areaAboveMinimumTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Customer", "TX", "Tile", new BigDecimal("250.50"));
        assertMoneyEquals("250.50", draft.getArea());
    }

    // --- Add / display / get ---

    @Test
    @DisplayName("Positive - Add and display orders for date")
    public void addAndDisplayOrdersTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Acme, Inc.", "TX", "Tile", new BigDecimal("100"));
        service.addOrder(draft);
        List<Order> orders = service.displayOrders(futureDate);
        Assertions.assertEquals(1, orders.size());
    }

    @Test
    @DisplayName("Positive - Display orders returns empty list when none exist")
    public void displayOrdersEmptyTest() throws Exception {
        LocalDate noOrdersDate = futureDate.plusDays(20);
        Assertions.assertTrue(service.displayOrders(noOrdersDate).isEmpty());
    }

    @Test
    @DisplayName("Positive - getOrder returns null when order missing")
    public void getOrderMissingTest() throws Exception {
        Assertions.assertNull(service.getOrder(futureDate, 1));
    }

    @Test
    @DisplayName("Negative - addOrder rejects incomplete order")
    public void addIncompleteOrderTest() throws Exception {
        Order incomplete = new Order();
        Assertions.assertThrows(FlooringValidationException.class, () -> service.addOrder(incomplete));
    }

    @Test
    @DisplayName("Positive - getAllProducts and getAllTaxes delegate to DAO")
    public void catalogPassThroughTest() throws Exception {
        List<Product> products = service.getAllProducts();
        List<Tax> taxes = service.getAllTaxes();
        Assertions.assertEquals(2, products.size());
        Assertions.assertEquals(2, taxes.size());
    }

    // --- Edit ---

    @Test
    @DisplayName("Negative - Edit missing order throws validation exception")
    public void editMissingOrderTest() throws Exception {
        Assertions.assertThrows(FlooringValidationException.class, () ->
                service.editOrderDraft(futureDate, 99, "Name", "TX", "Tile", new BigDecimal("100")));
    }

    @Test
    @DisplayName("Positive - Edit blank fields preserve existing values")
    public void editPreservesBlankFieldsTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Acme, Inc.", "TX", "Tile", new BigDecimal("100"));
        service.addOrder(draft);
        Order edited = service.editOrderDraft(futureDate, 1, "", "", "", new BigDecimal("200"));
        Assertions.assertEquals("Acme, Inc.", edited.getCustomerName());
        Assertions.assertEquals("TX", edited.getState());
        Assertions.assertEquals("Tile", edited.getProductType());
        assertMoneyEquals("1683.00", edited.getTotal());
    }

    @Test
    @DisplayName("Positive - Edit null area preserves existing area")
    public void editNullAreaPreservesTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Acme, Inc.", "TX", "Tile", new BigDecimal("100"));
        service.addOrder(draft);
        Order edited = service.editOrderDraft(futureDate, 1, "", "", "", null);
        assertMoneyEquals("100.00", edited.getArea());
        assertMoneyEquals("841.50", edited.getTotal());
    }

    @Test
    @DisplayName("Negative - Edit with area below minimum is rejected")
    public void editAreaBelowMinimumTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Customer", "TX", "Tile", new BigDecimal("100"));
        service.addOrder(draft);
        Assertions.assertThrows(FlooringValidationException.class, () ->
                service.editOrderDraft(futureDate, 1, "", "", "", new BigDecimal("50")));
    }

    @Test
    @DisplayName("Negative - Edit with invalid customer name is rejected")
    public void editInvalidCustomerNameTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Customer", "TX", "Tile", new BigDecimal("100"));
        service.addOrder(draft);
        Assertions.assertThrows(FlooringValidationException.class, () ->
                service.editOrderDraft(futureDate, 1, "Bad#Name", "", "", null));
    }

    @Test
    @DisplayName("Positive - Edit customer name only")
    public void editCustomerNameOnlyTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Old Name", "TX", "Tile", new BigDecimal("100"));
        service.addOrder(draft);
        Order edited = service.editOrderDraft(futureDate, 1, "New Name", "", "", null);
        Assertions.assertEquals("New Name", edited.getCustomerName());
        assertMoneyEquals("841.50", edited.getTotal());
    }

    @Test
    @DisplayName("Positive - Edit state recalculates tax and total")
    public void editStateRecalculatesTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Customer", "TX", "Tile", new BigDecimal("100"));
        service.addOrder(draft);
        Order edited = service.editOrderDraft(futureDate, 1, "", "CA", "", null);
        Assertions.assertEquals("CA", edited.getState());
        assertMoneyEquals("956.25", edited.getTotal());
    }

    @Test
    @DisplayName("Positive - Edit product type recalculates costs")
    public void editProductRecalculatesTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Customer", "TX", "Tile", new BigDecimal("100"));
        service.addOrder(draft);
        Order edited = service.editOrderDraft(futureDate, 1, "", "", "Wood", null);
        Assertions.assertEquals("Wood", edited.getProductType());
        assertMoneyEquals("1089.00", edited.getTotal());
    }

    @Test
    @DisplayName("Positive - Save edited order persists to DAO")
    public void saveEditedOrderTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Customer", "TX", "Tile", new BigDecimal("100"));
        service.addOrder(draft);
        Order edited = service.editOrderDraft(futureDate, 1, "Updated", "", "", null);
        service.saveEditedOrder(edited);
        Assertions.assertEquals("Updated", service.getOrder(futureDate, 1).getCustomerName());
    }

    // --- Remove ---

    @Test
    @DisplayName("Positive - Remove existing order")
    public void removeOrderTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Customer", "TX", "Tile", new BigDecimal("100"));
        service.addOrder(draft);
        service.removeOrder(futureDate, 1);
        Assertions.assertNull(service.getOrder(futureDate, 1));
    }

    @Test
    @DisplayName("Positive - Remove missing order returns null without error")
    public void removeMissingOrderTest() throws Exception {
        Assertions.assertNull(service.removeOrder(futureDate, 99));
    }

    // --- Export ---

    @Test
    @DisplayName("Positive - Export creates DataExport.txt with header")
    public void exportCreatesFileTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Acme, Inc.", "TX", "Tile", new BigDecimal("100"));
        service.addOrder(draft);
        service.exportAllData();

        File exportFile = new File(backupDir, "DataExport.txt");
        Assertions.assertTrue(exportFile.exists());
        String content = Files.readString(exportFile.toPath());
        Assertions.assertTrue(content.contains("OrderDate"));
        Assertions.assertTrue(content.contains("Acme, Inc."));
    }

    @Test
    @DisplayName("Positive - Export overwrites previous export file")
    public void exportOverwritesFileTest() throws Exception {
        File exportFile = new File(backupDir, "DataExport.txt");
        Files.writeString(exportFile.toPath(), "stale data");

        Order draft = service.createOrderDraft(futureDate, "Fresh", "TX", "Tile", new BigDecimal("100"));
        service.addOrder(draft);
        service.exportAllData();

        String content = Files.readString(exportFile.toPath());
        Assertions.assertFalse(content.contains("stale data"));
        Assertions.assertTrue(content.contains("Fresh"));
    }

    @Test
    @DisplayName("Positive - Export row includes MM-dd-yyyy order date")
    public void exportIncludesFormattedDateTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Customer", "TX", "Tile", new BigDecimal("100"));
        service.addOrder(draft);
        service.exportAllData();

        String expectedDate = String.format("%02d-%02d-%04d",
                futureDate.getMonthValue(), futureDate.getDayOfMonth(), futureDate.getYear());
        String content = Files.readString(new File(backupDir, "DataExport.txt").toPath());
        Assertions.assertTrue(content.contains(expectedDate));
    }

    @Test
    @DisplayName("Positive - Full flow save edit export remove")
    public void fullWorkflowTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Acme, Inc.", "TX", "Tile", new BigDecimal("100"));
        service.addOrder(draft);
        Order edited = service.editOrderDraft(futureDate, 1, "", "", "", new BigDecimal("200"));
        service.saveEditedOrder(edited);
        assertMoneyEquals("200.00", service.getOrder(futureDate, 1).getArea());

        service.exportAllData();
        Assertions.assertTrue(new File(backupDir, "DataExport.txt").exists());

        service.removeOrder(futureDate, 1);
        Assertions.assertTrue(service.displayOrders(futureDate).isEmpty());
    }

    private static void assertMoneyEquals(String expected, BigDecimal actual) {
        Assertions.assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "Expected " + expected + " but was " + actual);
    }
}
