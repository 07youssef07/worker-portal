package com.company.soaptranslator.camel;

import com.company.soaptranslator.camel.route.SoapMessageTranslatorRoute;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class CamelBootstrap {

    private static final Logger LOG = LoggerFactory.getLogger(CamelBootstrap.class);

    private CamelContext camelContext;

    @Inject
    private SoapMessageTranslatorRoute soapRoute;

    public void onStartup(@Observes @Initialized(ApplicationScoped.class) Object init) {
        try {
            camelContext = new DefaultCamelContext();
            camelContext.addRoutes(soapRoute);
            camelContext.start();
            LOG.info("Camel context started for soap-translator");
        } catch (Exception e) {
            LOG.error("Failed to start Camel context", e);
            throw new RuntimeException("Camel startup failed", e);
        }
    }

    public void onShutdown(@Observes @Destroyed(ApplicationScoped.class) Object init) {
        if (camelContext != null) {
            try {
                camelContext.stop();
                LOG.info("Camel context stopped");
            } catch (Exception e) {
                LOG.error("Error stopping Camel context", e);
            }
        }
    }

    public CamelContext getCamelContext() {
        return camelContext;
    }
}
