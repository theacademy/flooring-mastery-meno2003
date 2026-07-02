/*
 * =============================================================================
 * CLASS: Product
 * PACKAGE: Model
 * =============================================================================
 * WHAT: DTO for one row in Data/Products.txt — flooring type and per-square-foot costs.
 *
 * WHY: ProductDAO loads these; OrderServiceImpl uses them to compute material/labor costs.
 *
 * PATTERN: DTO / encapsulation (private fields + accessors).
 *
 * INTERVIEW EXPLANATION:
 * "Product is reference data, not an order. The service looks up Product by type and
 *  copies rates onto the Order so historical orders keep correct pricing if files change."
 * =============================================================================
 */
package Model;

import java.math.BigDecimal;

public class Product {

    /** Display/key name e.g. Tile, Wood — matched case-insensitively in ProductDAOImpl. */
    private String productType;

    /** Material price per square foot from Products.txt column 2. */
    private BigDecimal costPerSquareFoot;

    /** Labor price per square foot from Products.txt column 3. */
    private BigDecimal laborCostPerSquareFoot;

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public BigDecimal getCostPerSquareFoot() {
        return costPerSquareFoot;
    }

    public void setCostPerSquareFoot(BigDecimal costPerSquareFoot) {
        this.costPerSquareFoot = costPerSquareFoot;
    }

    public BigDecimal getLaborCostPerSquareFoot() {
        return laborCostPerSquareFoot;
    }

    public void setLaborCostPerSquareFoot(BigDecimal laborCostPerSquareFoot) {
        this.laborCostPerSquareFoot = laborCostPerSquareFoot;
    }
}
