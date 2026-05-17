package Practice.FlooringMastery.DAO;

import Practice.FlooringMastery.Model.Product;

import java.util.List;

public interface ProductDAO {
    Product getProduct(String productType) throws FlooringPersistenceException;

    List<Product> getAllProducts() throws FlooringPersistenceException;
}
