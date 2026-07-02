/*
 * =============================================================================
 * INTERFACE: OrderDAO
 * PACKAGE: DAO
 * =============================================================================
 * WHAT: Persistence contract for Order entities stored in date-specific files.
 *
 * WHY INTERFACE (Abstraction + loose coupling): OrderServiceImpl depends on OrderDAO,
 *      not OrderDAOImpl — enables test injection and future DB swap.
 *
 * PATTERN: DAO — Data Access Object.
 *
 * COLLECTIONS: Implementations use Map<Integer, Order> in memory while reading/writing files.
 *
 * INTERVIEW EXPLANATION:
 * "OrderDAO defines what persistence can do — get by date, add, update, remove, next number.
 *  How CSV files work is hidden inside OrderDAOImpl."
 * =============================================================================
 */
package DAO;

import Model.Order;

import java.time.LocalDate;
import java.util.List;

public interface OrderDAO {

    /** Returns all orders for one date, typically sorted by order number in impl. */
    List<Order> getOrdersByDate(LocalDate date) throws FlooringPersistenceException;

    /** Returns one order or null if order number does not exist for that date. */
    Order getOrder(LocalDate date, int orderNumber) throws FlooringPersistenceException;

    /** Persists order under the given date file; returns previous order at same number if any. */
    Order addOrder(LocalDate date, Order order) throws FlooringPersistenceException;

    /** Overwrites existing order with same order number for that date. */
    Order updateOrder(LocalDate date, Order order) throws FlooringPersistenceException;

    /** Removes order from date file; returns removed order or null. */
    Order removeOrder(LocalDate date, int orderNumber) throws FlooringPersistenceException;

    /**
     * BUSINESS RULE SUPPORT: Next order number = max existing + 1, or 1 if file empty.
     * Called by service during createOrderDraft.
     */
    int getNextOrderNumber(LocalDate date) throws FlooringPersistenceException;

    /** Scans all Orders_*.txt files — used by export feature in service layer. */
    List<Order> getAllOrders() throws FlooringPersistenceException;
}
