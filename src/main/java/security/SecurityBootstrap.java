package security;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class SecurityBootstrap implements ServletContextListener {
    @Override public void contextInitialized(ServletContextEvent event) {
        try {
            AccountStore.initializeAndBootstrap();
            event.getServletContext().setAttribute("campuspark.security.ready", Boolean.TRUE);
        } catch (Exception ex) {
            event.getServletContext().log("CampusPark account bootstrap failed; API access is disabled.", ex);
            event.getServletContext().setAttribute("campuspark.security.ready", Boolean.FALSE);
        }
    }
}
