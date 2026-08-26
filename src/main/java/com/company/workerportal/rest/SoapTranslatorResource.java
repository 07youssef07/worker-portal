package com.company.workerportal.rest;

import com.company.workerportal.camel.CamelBootstrap;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;

/**
 * SOAP message translator entry point.
 * POST /api/soap/translate?operation=getAllWorkers
 * POST /api/soap/translate?operation=getWorkerById&id=1
 * POST /api/soap/translate?operation=addWorker
 * POST /api/soap/translate?operation=updateWorker&id=1
 * POST /api/soap/translate?operation=deleteWorker&id=1
 */
@Path("/soap")
@Dependent
public class SoapTranslatorResource {

    @Inject
    private CamelBootstrap camelBootstrap;

    @POST
    @Path("/translate")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response translate(
            @QueryParam("operation") String operation,
            @QueryParam("id") String id,
            String soapBody) {

        if (operation == null || operation.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(soapFault("Missing required parameter: operation"))
                    .build();
        }

        CamelContext camelContext = camelBootstrap.getCamelContext();
        if (camelContext == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(soapFault("Camel context not initialized"))
                    .build();
        }

        try {
            ProducerTemplate producer = camelContext.createProducerTemplate();
            String result = producer.requestBodyAndHeaders(
                    "direct:soap-translate",
                    soapBody != null ? soapBody : "",
                    Map.of(
                        "operation", operation,
                        "id", id != null ? id : ""
                    ),
                    String.class
            );

            return Response.ok(result, MediaType.APPLICATION_XML).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(soapFault("Translation failed: " + e.getMessage()))
                    .build();
        }
    }

    private String soapFault(String message) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body><Fault><faultcode>Client</faultcode>"
                + "<faultstring>" + message + "</faultstring>"
                + "</Fault></soap:Body></soap:Envelope>";
    }
}
