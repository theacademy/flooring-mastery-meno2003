package Practice.FlooringMastery.tests;

import Practice.FlooringMastery.DAO.OrderDAO;
import Practice.FlooringMastery.DAO.OrderDAOImpl;
import Practice.FlooringMastery.DAO.ProductDAO;
import Practice.FlooringMastery.DAO.ProductDAOImpl;
import Practice.FlooringMastery.DAO.TaxDAO;
import Practice.FlooringMastery.DAO.TaxDAOImpl;
import Practice.FlooringMastery.Model.Order;
import Practice.FlooringMastery.service.FlooringValidationException;
import Practice.FlooringMastery.service.OrderService;
import Practice.FlooringMastery.service.OrderServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
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
        tempRoot = createTempDir("service-tests");
        dataDir = new File(tempRoot, "Data");
        ordersDir = new File(tempRoot, "Orders");
        backupDir = new File(tempRoot, "Backup");
        dataDir.mkdirs();
        ordersDir.mkdirs();
        backupDir.mkdirs();

        writeProducts(new File(dataDir, "Products.txt"));
        writeTaxes(new File(dataDir, "Taxes.txt"));

        OrderDAO orderDao = new OrderDAOImpl(ordersDir.getPath());
        ProductDAO productDao = new ProductDAOImpl(new File(dataDir, "Products.txt").getPath());
        TaxDAO taxDao = new TaxDAOImpl(new File(dataDir, "Taxes.txt").getPath());
        service = new OrderServiceImpl(orderDao, productDao, taxDao, backupDir.getPath());
        futureDate = LocalDate.now().plusDays(5);
    }

    @AfterEach
    public void tearDown() {
        deleteRecursively(tempRoot);
    }

    @Test
    @DisplayName("Negative - Same-day date is rejected")
    public void invalidDateValidationTest() {
        Assertions.assertThrows(FlooringValidationException.class, () ->
                service.createOrderDraft(LocalDate.now(), "Acme, Inc.", "TX", "Tile", new BigDecimal("100")));
    }

    @Test
    @DisplayName("Positive - Calculations are correct for added order")
    public void createOrderCalculationTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Acme, Inc.", "TX", "Tile", new BigDecimal("100"));
        Assertions.assertEquals(1, draft.getOrderNumber());
        Assertions.assertEquals(0, new BigDecimal("350.00").compareTo(draft.getMaterialCost()));
        Assertions.assertEquals(0, new BigDecimal("415.00").compareTo(draft.getLaborCost()));
        Assertions.assertEquals(0, new BigDecimal("76.50").compareTo(draft.getTax()));
        Assertions.assertEquals(0, new BigDecimal("841.50").compareTo(draft.getTotal()));
    }

    @Test
    @DisplayName("Positive - Add and display orders")
    public void addAndDisplayOrdersTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Acme, Inc.", "TX", "Tile", new BigDecimal("100"));
        service.addOrder(draft);
        List<Order> orders = service.displayOrders(futureDate);
        Assertions.assertEquals(1, orders.size());
    }

    @Test
    @DisplayName("Positive - Edit preserves blank fields and recalculates")
    public void editOrderDraftTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Acme, Inc.", "TX", "Tile", new BigDecimal("100"));
        service.addOrder(draft);
        Order edited = service.editOrderDraft(futureDate, 1, "", "", "", new BigDecimal("200"));
        Assertions.assertEquals("Acme, Inc.", edited.getCustomerName());
        Assertions.assertEquals(0, new BigDecimal("1683.00").compareTo(edited.getTotal()));
    }

    @Test
    @DisplayName("Positive - Save edit, export data, then remove order")
    public void saveExportRemoveFlowTest() throws Exception {
        Order draft = service.createOrderDraft(futureDate, "Acme, Inc.", "TX", "Tile", new BigDecimal("100"));
        service.addOrder(draft);
        Order edited = service.editOrderDraft(futureDate, 1, "", "", "", new BigDecimal("200"));
        service.saveEditedOrder(edited);
        Order fromStore = service.getOrder(futureDate, 1);
        Assertions.assertEquals(0, new BigDecimal("200.00").compareTo(fromStore.getArea()));

        service.exportAllData();
        File exportFile = new File(backupDir, "DataExport.txt");
        Assertions.assertTrue(exportFile.exists());

        service.removeOrder(futureDate, 1);
        Assertions.assertTrue(service.displayOrders(futureDate).isEmpty());
    }

    private static void writeProducts(File file) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("ProductType,CostPerSquareFoot,LaborCostPerSquareFoot");
            out.println("Tile,3.50,4.15");
        }
    }

    private static void writeTaxes(File file) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("StateAbbreviation,StateName,TaxRate");
            out.println("TX,Texas,10.00");
        }
    }

    private static File createTempDir(String name) {
        File dir = new File(System.getProperty("java.io.tmpdir"), "flooring-" + name + "-" + System.nanoTime());
        if (!dir.mkdirs()) {
            throw new RuntimeException("Could not create temp directory");
        }
        return dir;
    }

    private static void deleteRecursively(File file) {
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

}
