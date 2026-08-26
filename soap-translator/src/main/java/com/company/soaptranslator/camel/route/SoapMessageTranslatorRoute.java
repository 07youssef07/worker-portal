package com.company.soaptranslator.camel.route;

import com.company.soaptranslator.camel.processor.SoapHeaderAuthProcessor;
import com.company.soaptranslator.camel.processor.WorkerRequestProcessor;
import com.company.soaptranslator.camel.processor.WorkerResponseProcessor;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

@Dependent
public class SoapMessageTranslatorRoute extends RouteBuilder {

    @Inject
    @Named("soapHeaderAuthProcessor")
    private SoapHeaderAuthProcessor soapHeaderAuthProcessor;

    @Inject
    @Named("workerRequestProcessor")
    private WorkerRequestProcessor workerRequestProcessor;

    @Inject
    @Named("workerResponseProcessor")
    private WorkerResponseProcessor workerResponseProcessor;

    @Override
    public void configure() throws Exception {

        onException(Exception.class)
            .handled(true)
            .log("SOAP Error: ${exception.message}")
            .setHeader(Exchange.CONTENT_TYPE, constant("text/xml; charset=UTF-8"))
            .setBody(constant(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body><Fault><faultcode>Server</faultcode>"
                + "<faultstring>Internal processing error</faultstring>"
                + "</Fault></soap:Body></soap:Envelope>"));

        from("direct:soap-translate")
            .routeId("soap-message-translator")
            .log("SOAP request - operation: ${header.operation}")
            .process(soapHeaderAuthProcessor)
            .choice()
                .when(header("authFailed").isEqualTo(true))
                    .setHeader(Exchange.CONTENT_TYPE, constant("text/xml; charset=UTF-8"))
                .otherwise()
                    .process(workerRequestProcessor)
                    .process(workerResponseProcessor)
                    .setHeader(Exchange.CONTENT_TYPE, constant("text/xml; charset=UTF-8"))
            .end();
    }
}
