package Practice.FlooringMastery.tests;

import Practice.FlooringMastery.DAO.OrderDAO;
import Practice.FlooringMastery.DAO.OrderDAOImpl;
import Practice.FlooringMastery.DAO.ProductDAO;
import Practice.FlooringMastery.DAO.ProductDAOImpl;
import Practice.FlooringMastery.DAO.TaxDAO;
import Practice.FlooringMastery.DAO.TaxDAOImpl;
import Practice.FlooringMastery.Model.Order;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

@DisplayName("Flooring DAO Tests")
public class FlooringDaoTests {
    private static final DateTimeFormatter FILE_FORMAT = DateTimeFormatter.ofPattern("MMddyyyy");
    private File tempRoot;
    private File dataDir;
    private File ordersDir;
    private LocalDate date;
    private ProductDAO productDao;
    private TaxDAO taxDao;
    private OrderDAO orderDao;

    @BeforeEach
    public void setUp() throws Exception {
        tempRoot = createTempDir("dao-tests");
        dataDir = new File(tempRoot, "Data");
        ordersDir = new File(tempRoot, "Orders");
        dataDir.mkdirs();
        ordersDir.mkdirs();

        writeProducts(new File(dataDir, "Products.txt"));
        writeTaxes(new File(dataDir, "Taxes.txt"));
        date = LocalDate.now().plusDays(1);
        writeOrders(new File(ordersDir, "Orders_" + date.format(FILE_FORMAT) + ".txt"));

        productDao = new ProductDAOImpl(new File(dataDir, "Products.txt").getPath());
        taxDao = new TaxDAOImpl(new File(dataDir, "Taxes.txt").getPath());
        orderDao = new OrderDAOImpl(ordersDir.getPath());
    }

    @AfterEach
    public void tearDown() {
        deleteRecursively(tempRoot);
    }

    @Test
    @DisplayName("Positive - Products and taxes load correctly")
    public void productsAndTaxesLoadTest() throws Exception {
        Assertions.assertEquals(2, productDao.getAllProducts().size());
        Assertions.assertNotNull(taxDao.getTax("TX"));
    }

    @Test
    @DisplayName("Positive - Orders load and next order number increments")
    public void loadOrdersAndNextNumberTest() throws Exception {
        List<Order> loaded = orderDao.getOrdersByDate(date);
        Assertions.assertEquals(2, loaded.size());
        Assertions.assertEquals(3, orderDao.getNextOrderNumber(date));
    }

    @Test
    @DisplayName("Positive - Add order and retrieve by number")
    public void addOrderTest() throws Exception {
        Order newOrder = buildOrder(3, date, "Acme, Inc.", "TX", "Tile");
        orderDao.addOrder(date, newOrder);
        Assertions.assertNotNull(orderDao.getOrder(date, 3));
    }

    @Test
    @DisplayName("Positive - Remove order successfully")
    public void removeOrderTest() throws Exception {
        orderDao.removeOrder(date, 1);
        Assertions.assertNull(orderDao.getOrder(date, 1));
    }

    private static Order buildOrder(int number, LocalDate date, String customer, String state, String product) {
        Order order = new Order();
        order.setOrderNumber(number);
        order.setOrderDate(date);
        order.setCustomerName(customer);
        order.setState(state);
        order.setTaxRate(new BigDecimal("10.00"));
        order.setProductType(product);
        order.setArea(new BigDecimal("100.00"));
        order.setCostPerSquareFoot(new BigDecimal("3.50"));
        order.setLaborCostPerSquareFoot(new BigDecimal("4.15"));
        order.setMaterialCost(new BigDecimal("350.00"));
        order.setLaborCost(new BigDecimal("415.00"));
        order.setTax(new BigDecimal("76.50"));
        order.setTotal(new BigDecimal("841.50"));
        return order;
    }

    private static void writeProducts(File file) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("ProductType,CostPerSquareFoot,LaborCostPerSquareFoot");
            out.println("Tile,3.50,4.15");
            out.println("Wood,5.15,4.75");
        }
    }

    private static void writeTaxes(File file) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("StateAbbreviation,StateName,TaxRate");
            out.println("TX,Texas,10.00");
            out.println("CA,California,25.00");
        }
    }

    private static void writeOrders(File file) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("OrderNumber,CustomerName,State,TaxRate,ProductType,Area,CostPerSquareFoot,LaborCostPerSquareFoot,MaterialCost,LaborCost,Tax,Total");
            out.println("1,\"Acme, Inc.\",TX,10.00,Tile,100.00,3.50,4.15,350.00,415.00,76.50,841.50");
            out.println("2,Ada Lovelace,CA,25.00,Wood,200.00,5.15,4.75,1030.00,950.00,495.00,2475.00");
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
