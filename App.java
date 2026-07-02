/*
 * =============================================================================
 * CLASS: App
 * PACKAGE: Practice.FlooringMastery (root / composition root)
 * =============================================================================
 * WHAT: Application entry point. Starts the Spring IoC container and runs the menu loop.
 *
 * SPRING DEPENDENCY INJECTION (inversion of control):
 *   Previously, this class manually called `new` on every implementation and passed
 *   references into constructors. Spring now creates and wires those objects for us.
 *
 * TWO CONFIGURATION STYLES (course covers both):
 *
 *   1) XML (default below):
 *      ClassPathXmlApplicationContext loads config/applicationContext.xml.
 *      Beans are declared with <bean> and <constructor-arg ref="..."/>.
 *
 *   2) Annotation-based (alternate):
 *      Replace the context line with:
 *        new AnnotationConfigApplicationContext(AppConfig.class);
 *      Implementation classes use @Repository, @Service, @Component, and @Autowired.
 *
 * PROGRAMMING TO INTERFACES:
 *   FlooringController depends on OrderService (interface), not OrderServiceImpl.
 *   OrderServiceImpl depends on OrderDAO, ProductDAO, TaxDAO — not concrete DAO classes.
 *   Spring injects the matching implementation bean for each interface-typed constructor parameter.
 *
 * UNIT TESTS:
 *   Tests still use manual `new` with custom file paths — no Spring context required.
 * =============================================================================
 */
import controller.FlooringController;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class App {

    /**
     * JVM entry point.
     *
     * SPRING FLOW (XML configuration):
     *   1. ApplicationContext loads applicationContext.xml from the classpath.
     *   2. Spring instantiates each <bean> and satisfies <constructor-arg ref="..."/>.
     *   3. getBean("controller", ...) returns the fully wired FlooringController.
     *   4. controller.run() — same menu loop; only object creation moved to Spring.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        ApplicationContext context =
                new ClassPathXmlApplicationContext("applicationContext.xml");

        // SPRING: Lookup by bean id from XML; type parameter ensures correct class.
        FlooringController controller =
                context.getBean("controller", FlooringController.class);

        controller.run();
    }
}
