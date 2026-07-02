/*
 * =============================================================================
 * CLASS: FlooringController
 * PACKAGE: controller
 * =============================================================================
 * MVC ROLE: Controller — orchestrates application flow between View and Service.
 *
 * SPRING DI: XML bean id "controller" injects view + orderService (programming to OrderService interface).
 *
 * DEPENDENCIES (constructor injection, final fields):
 *   - FlooringView (composition) — all user I/O
 *   - OrderService (interface — abstraction/polymorphism) — all business operations
 *
 * DOES NOT: Open files, compute tax, validate regex — separation of concerns.
 *
 * EXCEPTION HANDLING: Catches FlooringPersistenceException | FlooringValidationException
 *                     in run() so one bad order does not exit the program.
 *
 * STATE: Menu loop flag keepGoing — minimal session state; no cached orders.
 *
 * INTERVIEW EXPLANATION:
 * "The controller orchestrates application flow but does not contain business logic.
 *  Business rules are delegated to the service layer to maintain separation of concerns."
 * =============================================================================
 */
package controller;

import DAO.FlooringPersistenceException;
import Model.Order;
import View.FlooringView;
import service.FlooringValidationException;
import service.OrderService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class FlooringController {

    /** VIEW dependency — composition; injected, not created here (loose coupling). */
    private final FlooringView view;

    /** SERVICE dependency — interface type enables polymorphism and test doubles. */
    private final OrderService service;

    /**
     * Constructor injection: Spring supplies view and OrderServiceImpl (wired as OrderService type).
     *
     * @param view    presentation layer
     * @param service business layer (interface)
     */
    public FlooringController(FlooringView view, OrderService service){
        this.view = view;
        this.service = service;
    }

    /**
     * Main application loop — runs until user selects Quit (6).
     * PATTERN: Front controller / menu-driven console app.
     */
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
                // EXCEPTION HANDLING: Recoverable errors — show message and return to menu.
                // INTERVIEW EXPLANATION:
                // "Checked exceptions force me to handle persistence and validation failures
                //  explicitly instead of crashing the JVM."
                view.displayErrorMessage(e.getMessage());
            }
        }
        view.displayExitBanner();
    }

    /**
     * USE CASE: Display Orders — read date from view, fetch from service, display list.
     * NOTE: No future-date validation on display (rubric allows viewing past/present dates).
     */
    private void displayOrder() throws FlooringPersistenceException {
        view.displayDisplayOrdersBanner();
        LocalDate orderDate = view.getOrderDate();
        List<Order> orders = service.displayOrders(orderDate);
        view.displayOrders(orders);
    }

    /**
     * USE CASE: Add Order — two-phase commit style: draft + confirm + persist.
     * FLOW: collect input → createOrderDraft (validate+calculate) → summary → confirm → addOrder.
     */
    private void addOrder() throws FlooringPersistenceException, FlooringValidationException {
        view.displayAddOrderBanner();
        LocalDate date = view.getOrderDate();
        String customerName = view.getCustomerName();
        String state = view.getStateAbbreviation();
        String productType = view.getProductType(service.getAllProducts());
        BigDecimal area = view.getArea();

        // Service assigns order number and runs all business rules / BigDecimal math.
        Order draft = service.createOrderDraft(date, customerName, state, productType, area);
        view.displayOrderSummary(draft);
        if (view.confirm("Place this order?")) {
            service.addOrder(draft);
            view.displaySuccess("Order added successfully.");
        } else {
            view.displaySuccess("Order was not saved.");
        }
    }

    /**
     * USE CASE: Edit Order — load existing, optional field updates via view, recalculate, confirm save.
     * BLANK FIELD RULE: Handled in service editOrderDraft (empty string keeps old value).
     */
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

    /**
     * USE CASE: Remove Order — show summary, confirm, delegate delete to service/DAO.
     */
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

    /**
     * USE CASE: Export All Data — service writes Backup/DataExport.txt; view confirms to user.
     */
    private void exportAlldata() throws FlooringPersistenceException {
        service.exportAllData();
        view.displaySuccess("All data exported to Backup/DataExport.txt");
    }
}
