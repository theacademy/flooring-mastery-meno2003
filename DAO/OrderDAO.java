package Practice.FlooringMastery.DAO;

import Practice.FlooringMastery.Model.Order;

import java.time.LocalDate;
import java.util.List;

public interface OrderDAO {
    List<Order> getOrdersByDate(LocalDate date) throws FlooringPersistenceException;

    Order getOrder(LocalDate date, int orderNumber) throws FlooringPersistenceException;

    Order addOrder(LocalDate date, Order order) throws FlooringPersistenceException;

    Order updateOrder(LocalDate date, Order order) throws FlooringPersistenceException;

    Order removeOrder(LocalDate date, int orderNumber) throws FlooringPersistenceException;

    int getNextOrderNumber(LocalDate date) throws FlooringPersistenceException;

    List<Order> getAllOrders() throws FlooringPersistenceException;
}
