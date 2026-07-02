/*
 * =============================================================================
 * INTERFACE: ProductDAO
 * PACKAGE: DAO
 * =============================================================================
 * WHAT: Read-only access to product catalog from Data/Products.txt.
 *
 * WHY: Service layer must not parse product files directly (separation of concerns).
 *
 * INTERVIEW EXPLANATION:
 * "ProductDAO is the only class that should know the Products.txt format."
 * =============================================================================
 */
package DAO;

import Model.Product;

import java.util.List;

public interface ProductDAO {

    /** Lookup by type; null if unknown (service converts to validation error). */
    Product getProduct(String productType) throws FlooringPersistenceException;

    /** All products — controller/view lists choices when adding/editing orders. */
    List<Product> getAllProducts() throws FlooringPersistenceException;
}
