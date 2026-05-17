package Practice.FlooringMastery.service;

import Practice.FlooringMastery.DAO.FlooringPersistenceException;
import Practice.FlooringMastery.Model.Order;
import Practice.FlooringMastery.Model.Product;
import Practice.FlooringMastery.Model.Tax;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface OrderService {
    List<Order> displayOrders(LocalDate orderDate) throws FlooringPersistenceException;

    Order createOrderDraft(LocalDate orderDate, String customerName, String state, String productType, BigDecimal area)
            throws FlooringPersistenceException, FlooringValidationException;

    Order addOrder(Order order) throws FlooringPersistenceException, FlooringValidationException;

    Order editOrderDraft(LocalDate orderDate, int orderNumber, String customerName, String state, String productType, BigDecimal area)
            throws FlooringPersistenceException, FlooringValidationException;

    Order saveEditedOrder(Order order) throws FlooringPersistenceException, FlooringValidationException;

    Order getOrder(LocalDate orderDate, int orderNumber) throws FlooringPersistenceException;

    Order removeOrder(LocalDate orderDate, int orderNumber) throws FlooringPersistenceException;

    void exportAllData() throws FlooringPersistenceException;

    List<Product> getAllProducts() throws FlooringPersistenceException;

    List<Tax> getAllTaxes() throws FlooringPersistenceException;

}
