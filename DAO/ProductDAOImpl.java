/*
 * =============================================================================
 * CLASS: ProductDAOImpl
 * PACKAGE: DAO
 * =============================================================================
 * WHAT: File-backed ProductDAO — reads Data/Products.txt into an in-memory Map.
 *
 * PATTERN: DAO + in-memory cache (LinkedHashMap preserves file order for display).
 *
 * STATEFUL: products map repopulated on each loadProducts() call (clear + reload).
 *
 * FILE I/O: Scanner + BufferedReader; skips header line; splits simple CSV (no quoted commas).
 *
 * COLLECTIONS: Map<String, Product> keyed by productType.toLowerCase() for case-insensitive lookup.
 *
 * SPRING DI: XML bean id "productDao" in applicationContext.xml (or @Repository with AppConfig).
 *
 * CONSTRUCTOR INJECTION (testability): Overload accepts custom file path — used in FlooringDaoTests.
 *
 * INTERVIEW EXPLANATION:
 * "ProductDAOImpl isolates catalog file format. The service asks for 'tile' or 'Tile' and
 *  we normalize to lowercase keys so users aren't punished for casing."
 * =============================================================================
 */
package DAO;

import Model.Product;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ProductDAOImpl implements ProductDAO {

    private static final String DEFAULT_PRODUCTS_FILE = "Data/Products.txt";
    private static final String DELIMITER = ",";

    /** Path to Products.txt — overridable in tests. */
    private final String productsFile;

    /**
     * In-memory cache — STATEFUL across calls until loadProducts clears and reloads.
     * LinkedHashMap: iteration order matches file order for getAllProducts().
     */
    private final Map<String, Product> products = new LinkedHashMap<>();

    public ProductDAOImpl() {
        this(DEFAULT_PRODUCTS_FILE);
    }

    public ProductDAOImpl(String productsFile) {
        this.productsFile = productsFile;
    }

    @Override
    public Product getProduct(String productType) throws FlooringPersistenceException {
        loadProducts();
        if (productType == null) {
            return null;
        }
        return products.get(productType.trim().toLowerCase());
    }

    @Override
    public List<Product> getAllProducts() throws FlooringPersistenceException {
        loadProducts();
        return new ArrayList<>(products.values());
    }

    /**
     * Reads entire product file into map. Called on every public method — simple but re-reads disk.
     * WEAKNESS (interview): Could cache until file modified; current design favors correctness/simplicity.
     */
    private void loadProducts() throws FlooringPersistenceException {
        products.clear();
        try (Scanner scanner = new Scanner(new BufferedReader(new FileReader(productsFile)))) {
            if (scanner.hasNextLine()) {
                scanner.nextLine(); // skip header: ProductType,CostPerSquareFoot,LaborCostPerSquareFoot
            }
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                Product product = unmarshallProduct(line);
                products.put(product.getProductType().toLowerCase(), product);
            }
        } catch (FileNotFoundException e) {
            throw new FlooringPersistenceException("Could not load products data.", e);
        }
    }

    /** Parses one CSV line into Product DTO (BigDecimal for money fields). */
    private Product unmarshallProduct(String line) {
        String[] tokens = line.split(DELIMITER);
        Product product = new Product();
        product.setProductType(tokens[0]);
        product.setCostPerSquareFoot(new BigDecimal(tokens[1]));
        product.setLaborCostPerSquareFoot(new BigDecimal(tokens[2]));
        return product;
    }
}
