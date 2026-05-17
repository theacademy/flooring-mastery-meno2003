package Practice.FlooringMastery.controller;

import Practice.FlooringMastery.DAO.FlooringPersistenceException;
import Practice.FlooringMastery.Model.Order;
import Practice.FlooringMastery.View.FlooringView;
import Practice.FlooringMastery.service.FlooringValidationException;
import Practice.FlooringMastery.service.OrderService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class FlooringController {

    private final FlooringView view;
    private final OrderService service;

    public FlooringController(FlooringView view, OrderService service){
        this.view = view;
        this.service = service;
    }

    public void run(){

        boolean keepGoing = true;

        while(keepGoing){
            int menuSelection = view.printMenuAndGetSelection();
            try {
                switch(menuSelection){

                    case 1:
                        displayOrder();
                        break;

                    case 2:
                        addOrder();
                        break;

                    case 3:
                        editOrder();
                        break;

                    case 4:
                        removeOrder();
                        break;

                    case 5:
                        exportAlldata();
                        break;

                    case 6:
                        keepGoing = false;
                        break;

                    default:
                        view.displayUnknownCommandBanner();
                }
            } catch (FlooringPersistenceException | FlooringValidationException e) {
                view.displayErrorMessage(e.getMessage());
            }
        }
        view.displayExitBanner();
    }

    private void displayOrder() throws FlooringPersistenceException {
        view.displayDisplayOrdersBanner();
        LocalDate orderDate = view.getOrderDate();
        List<Order> orders = service.displayOrders(orderDate);
        view.displayOrders(orders);
    }

    private void addOrder() throws FlooringPersistenceException, FlooringValidationException {
        view.displayAddOrderBanner();
        LocalDate date = view.getOrderDate();
        String customerName = view.getCustomerName();
        String state = view.getStateAbbreviation();
        String productType = view.getProductType(service.getAllProducts());
        BigDecimal area = view.getArea();

        Order draft = service.createOrderDraft(date, customerName, state, productType, area);
        view.displayOrderSummary(draft);
        if (view.confirm("Place this order?")) {
            service.addOrder(draft);
            view.displaySuccess("Order added successfully.");
        } else {
            view.displaySuccess("Order was not saved.");
        }
    }

    private void editOrder() throws FlooringPersistenceException, FlooringValidationException {
        view.displayEditOrderBanner();
        LocalDate date = view.getOrderDate();
        int orderNumber = view.getOrderNumber();
        Order existing = service.getOrder(date, orderNumber);
        if (existing == null) {
            view.displaySuccess("Order not found.");
            return;
        }

        String customerName = view.editCustomerName(existing.getCustomerName());
        String state = view.editState(existing.getState());
        String productType = view.editProductType(existing.getProductType(), service.getAllProducts());
        BigDecimal area = view.editArea(existing.getArea());
        Order updated = service.editOrderDraft(date, orderNumber, customerName, state, productType, area);

        view.displayOrderSummary(updated);
        if (view.confirm("Save these edits?")) {
            service.saveEditedOrder(updated);
            view.displaySuccess("Order updated successfully.");
        } else {
            view.displaySuccess("Edit cancelled.");
        }
    }

    private void removeOrder() throws FlooringPersistenceException {
        view.displayRemoveOrderBanner();
        LocalDate date = view.getOrderDate();
        int orderNumber = view.getOrderNumber();
        Order existing = service.getOrder(date, orderNumber);
        if (existing == null) {
            view.displaySuccess("Order not found.");
            return;
        }
        view.displayOrderSummary(existing);
        if (view.confirm("Are you sure you want to remove this order?")) {
            service.removeOrder(date, orderNumber);
            view.displaySuccess("Order removed successfully.");
        } else {
            view.displaySuccess("Remove cancelled.");
        }
    }

    private void exportAlldata() throws FlooringPersistenceException {
        service.exportAllData();
        view.displaySuccess("All data exported to Backup/DataExport.txt");
    }
}
