/*
 * =============================================================================
 * CLASS: AppConfig
 * PACKAGE: config
 * =============================================================================
 * SPRING ROLE: Annotation-based configuration (alternate to applicationContext.xml).
 *
 * @Configuration — marks this class as a Spring Java configuration source.
 * @ComponentScan — Spring finds classes annotated with @Repository, @Service, @Component
 *                  and registers them as beans, wiring @Autowired constructors automatically.
 *
 * TO USE THIS INSTEAD OF XML:
 *   In App.main, replace ClassPathXmlApplicationContext with:
 *     new AnnotationConfigApplicationContext(AppConfig.class);
 *   and use context.getBean(FlooringController.class).
 *
 * COURSE ALIGNMENT:
 *   Lesson covers XML configuration AND annotation-based configuration — this project
 *   demonstrates both; XML is the default bootstrap in App.java.
 * =============================================================================
 */
package config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"Model", "DAO", "View", "controller", "service", "config"})
public class AppConfig {
}
