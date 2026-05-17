package Practice.FlooringMastery.DAO;

import Practice.FlooringMastery.Model.Product;

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
    private final String productsFile;
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

    private void loadProducts() throws FlooringPersistenceException {
        products.clear();
        try (Scanner scanner = new Scanner(new BufferedReader(new FileReader(productsFile)))) {
            if (scanner.hasNextLine()) {
                scanner.nextLine();
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

    private Product unmarshallProduct(String line) {
        String[] tokens = line.split(DELIMITER);
        Product product = new Product();
        product.setProductType(tokens[0]);
        product.setCostPerSquareFoot(new BigDecimal(tokens[1]));
        product.setLaborCostPerSquareFoot(new BigDecimal(tokens[2]));
        return product;
    }
}
