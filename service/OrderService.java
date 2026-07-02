/*
 * =============================================================================
 * INTERFACE: OrderService
 * PACKAGE: service
 * =============================================================================
 * WHAT: Application service contract — all use cases the controller needs for orders.
 *
 * WHY INTERFACE (Polymorphism + DI): FlooringController holds OrderService, not
 *      OrderServiceImpl — same pattern Spring would use with @Service interface injection.
 *
 * PATTERN: Service Layer — sits between controller and DAOs; owns business rules.
 *
 * INTERVIEW EXPLANATION:
 * "The interface documents use cases: draft, add, edit, remove, export. Implementation
 *  contains validation and BigDecimal math; controller only sees this API."
 * =============================================================================
 */
package service;

import DAO.FlooringPersistenceException;
import Model.Order;
import Model.Product;
import Model.Tax;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface OrderService {

    /** Use case: Display Orders — fetch all orders for a date (no future-date rule here). */
    List<Order> displayOrders(LocalDate orderDate) throws FlooringPersistenceException;

    /**
     * Builds a calculated order preview with next order number; does not persist until addOrder.
     * Enforces future date + input validation + pricing.
     */
    Order createOrderDraft(LocalDate orderDate, String customerName, String state, String productType, BigDecimal area)
            throws FlooringPersistenceException, FlooringValidationException;

    /** Persists a previously drafted/confirmed order. */
    Order addOrder(Order order) throws FlooringPersistenceException, FlooringValidationException;

    /**
     * Rebuilds order with optional field updates (blank keeps existing); recalculates costs.
     */
    Order editOrderDraft(LocalDate orderDate, int orderNumber, String customerName, String state, String productType, BigDecimal area)
            throws FlooringPersistenceException, FlooringValidationException;

    /** Writes edited order to persistence after user confirmation. */
    Order saveEditedOrder(Order order) throws FlooringPersistenceException, FlooringValidationException;

    Order getOrder(LocalDate orderDate, int orderNumber) throws FlooringPersistenceException;

    Order removeOrder(LocalDate orderDate, int orderNumber) throws FlooringPersistenceException;

    /** Export all orders from all date files to Backup/DataExport.txt */
    void exportAllData() throws FlooringPersistenceException;

    /** Pass-through for view to list product choices — delegates to ProductDAO. */
    List<Product> getAllProducts() throws FlooringPersistenceException;

    List<Tax> getAllTaxes() throws FlooringPersistenceException;
}
