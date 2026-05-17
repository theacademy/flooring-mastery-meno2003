package Practice.FlooringMastery;

import Practice.FlooringMastery.DAO.OrderDAO;
import Practice.FlooringMastery.DAO.OrderDAOImpl;
import Practice.FlooringMastery.DAO.ProductDAO;
import Practice.FlooringMastery.DAO.ProductDAOImpl;
import Practice.FlooringMastery.DAO.TaxDAO;
import Practice.FlooringMastery.DAO.TaxDAOImpl;
import Practice.FlooringMastery.View.FlooringView;
import Practice.FlooringMastery.View.UserIO;
import Practice.FlooringMastery.View.UserIOConsoleImpl;
import Practice.FlooringMastery.controller.FlooringController;
import Practice.FlooringMastery.service.OrderService;
import Practice.FlooringMastery.service.OrderServiceImpl;

public class App {
    public static void main(String[] args){
        UserIO userIO = new UserIOConsoleImpl();
        FlooringView view = new FlooringView(userIO);
        OrderDAO orderDao = new OrderDAOImpl();
        ProductDAO productDao = new ProductDAOImpl();
        TaxDAO taxDao = new TaxDAOImpl();
        OrderService service = new OrderServiceImpl(orderDao, productDao, taxDao);
        FlooringController controller = new FlooringController(view, service);
        controller.run();
    }
}
