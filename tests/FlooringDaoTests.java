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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

@DisplayName("Flooring DAO Tests")
public class FlooringDaoTests {

    private File tempRoot;
    private File dataDir;
    private File ordersDir;
    private LocalDate date;

    private ProductDAO productDao;
    private TaxDAO taxDao;
    private OrderDAO orderDao;

    @BeforeEach
    public void setUp() throws Exception {
        tempRoot = TestFixtures.createTempDir("dao-tests");
        dataDir = new File(tempRoot, "Data");
        ordersDir = new File(tempRoot, "Orders");
        dataDir.mkdirs();
        ordersDir.mkdirs();

        TestFixtures.writeDefaultProducts(new File(dataDir, "Products.txt"));
        TestFixtures.writeDefaultTaxes(new File(dataDir, "Taxes.txt"));
        date = LocalDate.now().plusDays(1);
        TestFixtures.writeSampleOrders(TestFixtures.orderFileForDate(ordersDir, date), date);

        productDao = new ProductDAOImpl(new File(dataDir, "Products.txt").getPath());
        taxDao = new TaxDAOImpl(new File(dataDir, "Taxes.txt").getPath());
        orderDao = new OrderDAOImpl(ordersDir.getPath());
    }

    @AfterEach
    public void tearDown() {
        TestFixtures.deleteRecursively(tempRoot);
    }

    // --- ProductDAO ---

    @Test
    @DisplayName("Positive - All products load from file")
    public void productsLoadTest() throws Exception {
        List<Product> products = productDao.getAllProducts();
        Assertions.assertEquals(2, products.size());
        Assertions.assertEquals("Tile", products.get(0).getProductType());
    }

    @Test
    @DisplayName("Positive - Product lookup is case-insensitive")
    public void productLookupCaseInsensitiveTest() throws Exception {
        Assertions.assertNotNull(productDao.getProduct("tile"));
        Assertions.assertNotNull(productDao.getProduct("WOOD"));
    }

    @Test
    @DisplayName("Negative - Unknown product returns null")
    public void unknownProductReturnsNullTest() throws Exception {
        Assertions.assertNull(productDao.getProduct("Marble"));
    }

    @Test
    @DisplayName("Negative - Missing products file throws persistence exception")
    public void missingProductsFileTest() {
        ProductDAO missingFileDao = new ProductDAOImpl(new File(dataDir, "NoProducts.txt").getPath());
        Assertions.assertThrows(FlooringPersistenceException.class, missingFileDao::getAllProducts);
    }

    // --- TaxDAO ---

    @Test
    @DisplayName("Positive - All taxes load from file")
    public void taxesLoadTest() throws Exception {
        Assertions.assertEquals(2, taxDao.getAllTaxes().size());
    }

    @Test
    @DisplayName("Positive - Tax lookup is case-insensitive")
    public void taxLookupCaseInsensitiveTest() throws Exception {
        Tax tx = taxDao.getTax("tx");
        Assertions.assertNotNull(tx);
        Assertions.assertEquals("TX", tx.getStateAbbreviation());
    }

    @Test
    @DisplayName("Negative - Unknown state returns null")
    public void unknownStateReturnsNullTest() throws Exception {
        Assertions.assertNull(taxDao.getTax("ZZ"));
    }

    @Test
    @DisplayName("Negative - Missing taxes file throws persistence exception")
    public void missingTaxesFileTest() {
        TaxDAO missingFileDao = new TaxDAOImpl(new File(dataDir, "NoTaxes.txt").getPath());
        Assertions.assertThrows(FlooringPersistenceException.class, missingFileDao::getAllTaxes);
    }

    // --- OrderDAO read ---

    @Test
    @DisplayName("Positive - Orders load with quoted customer names parsed")
    public void ordersLoadQuotedNameTest() throws Exception {
        Order order = orderDao.getOrder(date, 1);
        Assertions.assertNotNull(order);
        Assertions.assertEquals("Acme, Inc.", order.getCustomerName());
    }

    @Test
    @DisplayName("Positive - Orders for date are sorted by order number")
    public void ordersSortedByNumberTest() throws Exception {
        List<Order> orders = orderDao.getOrdersByDate(date);
        Assertions.assertEquals(2, orders.size());
        Assertions.assertEquals(1, orders.get(0).getOrderNumber());
        Assertions.assertEquals(2, orders.get(1).getOrderNumber());
    }

    @Test
    @DisplayName("Positive - Empty date returns empty list and next order number 1")
    public void emptyDateFileTest() throws Exception {
        LocalDate emptyDate = date.plusDays(30);
        Assertions.assertTrue(orderDao.getOrdersByDate(emptyDate).isEmpty());
        Assertions.assertEquals(1, orderDao.getNextOrderNumber(emptyDate));
    }

    @Test
    @DisplayName("Positive - Next order number increments after existing orders")
    public void nextOrderNumberTest() throws Exception {
        Assertions.assertEquals(3, orderDao.getNextOrderNumber(date));
    }

    @Test
    @DisplayName("Negative - Get missing order returns null")
    public void getMissingOrderTest() throws Exception {
        Assertions.assertNull(orderDao.getOrder(date, 999));
    }

    // --- OrderDAO write ---

    @Test
    @DisplayName("Positive - Add order persists and can be retrieved")
    public void addOrderTest() throws Exception {
        Order newOrder = TestFixtures.buildSimpleOrder(3, date, "New Customer", "TX", "Tile");
        orderDao.addOrder(date, newOrder);
        Order loaded = orderDao.getOrder(date, 3);
        Assertions.assertNotNull(loaded);
        Assertions.assertEquals("New Customer", loaded.getCustomerName());
    }

    @Test
    @DisplayName("Positive - Add order with comma in name round-trips through file")
    public void addOrderCommaInNameTest() throws Exception {
        Order newOrder = TestFixtures.buildSimpleOrder(3, date, "Acme, Inc.", "TX", "Tile");
        orderDao.addOrder(date, newOrder);
        Assertions.assertEquals("Acme, Inc.", orderDao.getOrder(date, 3).getCustomerName());
    }

    @Test
    @DisplayName("Positive - Update order replaces existing row")
    public void updateOrderTest() throws Exception {
        Order existing = orderDao.getOrder(date, 1);
        existing.setCustomerName("Updated Name");
        orderDao.updateOrder(date, existing);
        Assertions.assertEquals("Updated Name", orderDao.getOrder(date, 1).getCustomerName());
    }

    @Test
    @DisplayName("Positive - Remove order deletes from file")
    public void removeOrderTest() throws Exception {
        orderDao.removeOrder(date, 1);
        Assertions.assertNull(orderDao.getOrder(date, 1));
        Assertions.assertEquals(1, orderDao.getOrdersByDate(date).size());
    }

    @Test
    @DisplayName("Negative - Remove missing order returns null")
    public void removeMissingOrderTest() throws Exception {
        Assertions.assertNull(orderDao.removeOrder(date, 999));
    }

    @Test
    @DisplayName("Positive - getAllOrders aggregates multiple date files")
    public void getAllOrdersMultipleDatesTest() throws Exception {
        LocalDate secondDate = date.plusDays(10);
        Order secondDateOrder = TestFixtures.buildSimpleOrder(1, secondDate, "Second Day", "TX", "Tile");
        orderDao.addOrder(secondDate, secondDateOrder);

        List<Order> all = orderDao.getAllOrders();
        Assertions.assertEquals(3, all.size());
    }

    @Test
    @DisplayName("Positive - getAllOrders returns empty when orders folder has no files")
    public void getAllOrdersEmptyFolderTest() throws Exception {
        File emptyOrdersDir = new File(tempRoot, "EmptyOrders");
        emptyOrdersDir.mkdirs();
        OrderDAO emptyDao = new OrderDAOImpl(emptyOrdersDir.getPath());
        Assertions.assertTrue(emptyDao.getAllOrders().isEmpty());
    }

    @Test
    @DisplayName("Positive - Order date on loaded order matches file date")
    public void orderDateFromFileNameTest() throws Exception {
        Order order = orderDao.getOrder(date, 2);
        Assertions.assertEquals(date, order.getOrderDate());
        Assertions.assertEquals("Wood", order.getProductType());
    }
}
