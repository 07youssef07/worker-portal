package com.company.workerportal.camel.processor;

import com.company.workerportal.model.Worker;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@Named("workerResponseProcessor")
public class WorkerResponseProcessor implements Processor {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    @SuppressWarnings("unchecked")
    public void process(Exchange exchange) throws Exception {
        Object body = exchange.getIn().getBody();
        String operation = null;

        if (body instanceof Map) {
            Map<String, Object> result = (Map<String, Object>) body;
            operation = (String) result.get("operation");
            Boolean success = (Boolean) result.get("success");

            String soapXml = switch (operation) {
                case "getAllWorkers" -> buildGetAllWorkersResponse(result, success);
                case "getWorkerById" -> buildGetWorkerByIdResponse(result, success);
                case "addWorker" -> buildAddWorkerResponse(result, success);
                case "updateWorker" -> buildUpdateWorkerResponse(result, success);
                case "deleteWorker" -> buildDeleteWorkerResponse(result, success);
                default -> buildErrorResponse("Unknown operation");
            };

            exchange.getIn().setBody(soapXml);
            exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "text/xml; charset=UTF-8");
        }
    }

    private String buildGetAllWorkersResponse(Map<String, Object> result, Boolean success) {
        StringBuilder xml = new StringBuilder();
        xml.append(wrapSoapBodyStart());
        xml.append("<getAllWorkersResponse>");
        xml.append("<success>").append(success).append("</success>");
        xml.append("<workers>");

        Object data = result.get("data");
        if (data instanceof Worker[] workers) {
            for (Worker w : workers) {
                xml.append(workerToXml(w));
            }
        }

        xml.append("</workers>");
        xml.append("</getAllWorkersResponse>");
        xml.append(wrapSoapBodyEnd());
        return xml.toString();
    }

    private String buildGetWorkerByIdResponse(Map<String, Object> result, Boolean success) {
        StringBuilder xml = new StringBuilder();
        xml.append(wrapSoapBodyStart());
        xml.append("<getWorkerByIdResponse>");
        xml.append("<success>").append(success).append("</success>");
        xml.append("<worker>");

        Object data = result.get("data");
        if (data instanceof Worker w) {
            xml.append(workerFieldsToXml(w));
        }

        xml.append("</worker>");
        xml.append("</getWorkerByIdResponse>");
        xml.append(wrapSoapBodyEnd());
        return xml.toString();
    }

    private String buildAddWorkerResponse(Map<String, Object> result, Boolean success) {
        StringBuilder xml = new StringBuilder();
        xml.append(wrapSoapBodyStart());
        xml.append("<addWorkerResponse>");
        xml.append("<success>").append(success).append("</success>");
        Object data = result.get("data");
        xml.append("<id>").append(data != null ? data : -1).append("</id>");
        xml.append("</addWorkerResponse>");
        xml.append(wrapSoapBodyEnd());
        return xml.toString();
    }

    private String buildUpdateWorkerResponse(Map<String, Object> result, Boolean success) {
        StringBuilder xml = new StringBuilder();
        xml.append(wrapSoapBodyStart());
        xml.append("<updateWorkerResponse>");
        xml.append("<success>").append(success).append("</success>");
        xml.append("</updateWorkerResponse>");
        xml.append(wrapSoapBodyEnd());
        return xml.toString();
    }

    private String buildDeleteWorkerResponse(Map<String, Object> result, Boolean success) {
        StringBuilder xml = new StringBuilder();
        xml.append(wrapSoapBodyStart());
        xml.append("<deleteWorkerResponse>");
        xml.append("<success>").append(success).append("</success>");
        xml.append("</deleteWorkerResponse>");
        xml.append(wrapSoapBodyEnd());
        return xml.toString();
    }

    private String buildErrorResponse(String message) {
        return wrapSoapBodyStart()
            + "<Fault><faultcode>Client</faultcode><faultstring>" + message + "</faultstring></Fault>"
            + wrapSoapBodyEnd();
    }

    private String workerToXml(Worker w) {
        StringBuilder xml = new StringBuilder();
        xml.append("<worker>");
        xml.append(workerFieldsToXml(w));
        xml.append("</worker>");
        return xml.toString();
    }

    private String workerFieldsToXml(Worker w) {
        StringBuilder xml = new StringBuilder();
        xml.append("<id>").append(w.getId()).append("</id>");
        xml.append("<firstName>").append(escapeXml(w.getFirstName())).append("</firstName>");
        xml.append("<lastName>").append(escapeXml(w.getLastName())).append("</lastName>");
        xml.append("<dateOfBirth>").append(w.getDateOfBirth() != null ? w.getDateOfBirth().format(DATE_FMT) : "").append("</dateOfBirth>");
        xml.append("<role>").append(escapeXml(w.getRole())).append("</role>");
        return xml.toString();
    }

    private String wrapSoapBodyStart() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body>";
    }

    private String wrapSoapBodyEnd() {
        return "</soap:Body></soap:Envelope>";
    }

    private String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }
}
