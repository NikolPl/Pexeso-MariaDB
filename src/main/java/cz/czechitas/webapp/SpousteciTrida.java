package cz.czechitas.webapp;

import java.sql.*;
import javax.sql.*;
import org.mariadb.jdbc.*;
import org.slf4j.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.builder.*;
import org.springframework.boot.context.embedded.*;
import org.springframework.boot.context.event.*;
import org.springframework.boot.web.support.*;
import org.springframework.context.*;
import org.springframework.context.annotation.*;
import org.springframework.context.event.*;
import org.springframework.core.env.*;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.view.*;

@SpringBootApplication
public class SpousteciTrida extends SpringBootServletInitializer {

    private static final Logger logger = LoggerFactory.getLogger(SpousteciTrida.class);


    /**
     * Spousteci metoda v aplikaci, pokud je aplikace spoustena jako .jar
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplicationBuilder()
                .sources(SpousteciTrida.class)
                .build();
        app.run(args);
    }


    /**
     * Spousteci metoda v aplikaci, pokud je aplikace spoustena jako .war
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(SpousteciTrida.class);
    }


    /**
     * Oprava chovani beanu, ktery je zodpovedny za tuto funkcionalitu:
     * Pokud v ModelAndView neni rucne nastavene viewName (= cesta k souboru sablony),
     * tento bean vygeneruje viewName automaticky na zaklade URL pozadavku.
     *
     * Problem je ve chvili, kdy si vypneme automaticke suffixovani viewNamu ve viewResolveru,
     * napriklad pomoci spring.thymeleaf.suffix="",
     * protoze misto "index" chceme psat "index.html".
     * Ve vychozim nastaveni by tento bean totiz zahazoval
     * priponu souboru z URL.
     *
     * Aby to nedelal, predefinovavame ho zde.
     */
    @Bean
    public RequestToViewNameTranslator viewNameTranslator(Environment environment) {
        DefaultRequestToViewNameTranslator viewNameTranslator = new DefaultRequestToViewNameTranslator();
        String thymeleafSuffix = environment.getProperty("spring.thymeleaf.suffix");
        if (thymeleafSuffix != null && thymeleafSuffix.isEmpty()) {
            viewNameTranslator.setStripExtension(false);
        }
        return viewNameTranslator;
    }


    /**
     * Posluchac udalosti nasazeni webove aplikace na Tomcat, ktery zaloguje adresu,
     * na ktere je aplikace nasazena
     * @param evt
     */
    @EventListener
    public void onAppEvent(EmbeddedServletContainerInitializedEvent evt) {
        int port = evt.getEmbeddedServletContainer().getPort();
        logger.info("Your web app address: http://localhost:" + port +
                evt.getApplicationContext().getServletContext().getContextPath());
    }

    /*
    // TODO
    @Bean
    public DataSource zdrojDat() {
        try {
            MariaDbDataSource dataSource = new MariaDbDataSource();
            dataSource.setUserName("student");
            dataSource.setPassword("password");
            dataSource.setUrl("jdbc:mysql://localhost:3306/Pexeso");
            return dataSource;
        } catch (SQLException e) {
            throw new RuntimeException("Nepodarilo se pripojit do databaze: " + e.getMessage(), e);
        }
    }
    */

}
