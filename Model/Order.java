/*
 * =============================================================================
 * CLASS: Order
 * PACKAGE: Model
 * =============================================================================
 * WHAT: Data Transfer Object (DTO) representing one flooring order — all fields needed
 *       for display, persistence, and export.
 *
 * WHY: Layers pass a single structured object instead of many parallel parameters.
 *      Encapsulation hides internal representation behind getters/setters.
 *
 * NOT: This class does not calculate tax or validate dates — that is OrderServiceImpl.
 *
 * PATTERN: JavaBean DTO; used by MVC Model role, DAO marshalling, and service calculations.
 *
 * BigDecimal: All money and area fields use BigDecimal per rubric (precision for currency).
 *
 * INTERVIEW EXPLANATION:
 * "Order is the contract between service and DAO. Once the service fills every field,
 *  the DAO serializes it to CSV without re-running business logic."
 * =============================================================================
 */
package Model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Order {

    /** Unique per order date file; assigned by service via orderDao.getNextOrderNumber. */
    private Integer orderNumber;

    /** Calendar date of the order; file name derived from this (MMddyyyy). */
    private LocalDate orderDate;

    /** Customer display name; validated in service with regex. */
    private String customerName;

    /** Two-letter state abbreviation; must exist in Taxes.txt. */
    private String state;

    /** Tax rate percent copied from Tax at calculation time (snapshot for persistence). */
    private BigDecimal taxRate;

    /** Product name (Tile, Wood, etc.) from Products.txt. */
    private String productType;

    /** Square footage; rubric minimum 100 enforced in service. */
    private BigDecimal area;

    /** Snapshot of product material cost per sq ft at order time. */
    private BigDecimal costPerSquareFoot;

    /** Snapshot of product labor cost per sq ft at order time. */
    private BigDecimal laborCostPerSquareFoot;

    /** Calculated: area × costPerSquareFoot (service layer). */
    private BigDecimal materialCost;

    /** Calculated: area × laborCostPerSquareFoot (service layer). */
    private BigDecimal laborCost;

    /** Calculated: (material + labor) × (taxRate / 100) (service layer). */
    private BigDecimal tax;

    /** Calculated: material + labor + tax (service layer). */
    private BigDecimal total;

    // --- ENCAPSULATION: standard accessors; no business logic in setters ---

    public Integer getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(Integer orderNumber) {
        this.orderNumber = orderNumber;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public BigDecimal getArea() {
        return area;
    }

    public void setArea(BigDecimal area) {
        this.area = area;
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

    public BigDecimal getMaterialCost() {
        return materialCost;
    }

    public void setMaterialCost(BigDecimal materialCost) {
        this.materialCost = materialCost;
    }

    public BigDecimal getLaborCost() {
        return laborCost;
    }

    public void setLaborCost(BigDecimal laborCost) {
        this.laborCost = laborCost;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}
