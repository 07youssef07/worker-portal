package com.company.workerportal.camel.processor;

import com.company.workerportal.model.Worker;
import com.company.workerportal.service.WorkerService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@Named("workerRequestProcessor")
public class WorkerRequestProcessor implements Processor {

    private final WorkerService workerService = new WorkerService();

    @Override
    public void process(Exchange exchange) throws Exception {
        String operation = exchange.getIn().getHeader("operation", String.class);

        if (operation == null || operation.isBlank()) {
            operation = extractOperationFromSoapBody(exchange);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("operation", operation);

        switch (operation) {
            case "getAllWorkers" -> {
                String searchTerm = extractSearchTermFromSoap(exchange);
                LocalDate dateFrom = extractDateFromSoap(exchange, "dateFrom");
                LocalDate dateTo = extractDateFromSoap(exchange, "dateTo");
                boolean hasFilter = (searchTerm != null && !searchTerm.isBlank())
                    || dateFrom != null || dateTo != null;
                Worker[] workers = hasFilter
                    ? workerService.searchWorkers(searchTerm, null, null, false, dateFrom, dateTo)
                    : workerService.getAllWorkers();
                result.put("data", workers);
                result.put("success", true);
            }
            case "getWorkerById" -> {
                String idStr = exchange.getIn().getHeader("id", String.class);
                Long id = idStr != null ? Long.parseLong(idStr) : extractLongFromBody(exchange, "id");
                Worker worker = workerService.getWorkerById(id);
                result.put("data", worker);
                result.put("success", worker != null);
            }
            case "addWorker" -> {
                Map<String, String> fields = extractFieldsFromSoap(exchange);
                Worker worker = mapToWorker(fields);
                if (worker != null) {
                    Long newId = workerService.addWorker(worker);
                    result.put("data", newId != null ? newId : -1L);
                    result.put("success", newId != null);
                } else {
                    result.put("data", -1L);
                    result.put("success", false);
                }
            }
            case "updateWorker" -> {
                String idStr = exchange.getIn().getHeader("id", String.class);
                Long id = idStr != null ? Long.parseLong(idStr) : extractLongFromBody(exchange, "id");
                Map<String, String> fields = extractFieldsFromSoap(exchange);
                Worker worker = mapToWorker(fields);
                if (worker != null && id != null) {
                    boolean success = workerService.updateWorker(id, worker);
                    result.put("data", success);
                    result.put("success", success);
                } else {
                    result.put("data", false);
                    result.put("success", false);
                }
            }
            case "deleteWorker" -> {
                String idStr = exchange.getIn().getHeader("id", String.class);
                Long id = idStr != null ? Long.parseLong(idStr) : extractLongFromBody(exchange, "id");
                boolean success = workerService.deleteWorker(id);
                result.put("data", success);
                result.put("success", success);
            }
            default -> {
                result.put("data", null);
                result.put("success", false);
                result.put("error", "Unknown operation: " + operation);
            }
        }

        exchange.getIn().setBody(result);
    }

    private String extractOperationFromSoapBody(Exchange exchange) {
        try {
            String xml = getBodyAsString(exchange);
            if (xml == null) return null;

            Document doc = parseXml(xml);
            if (doc == null) return null;

            Element envelope = doc.getDocumentElement();
            NodeList bodyNodes = envelope.getElementsByTagNameNS("*", "Body");
            if (bodyNodes.getLength() == 0) {
                bodyNodes = envelope.getElementsByTagName("soap:Body");
                if (bodyNodes.getLength() == 0) bodyNodes = envelope.getElementsByTagName("Body");
            }

            if (bodyNodes.getLength() > 0) {
                Element body = (Element) bodyNodes.item(0);
                for (int i = 0; i < body.getChildNodes().getLength(); i++) {
                    if (body.getChildNodes().item(i) instanceof Element opElement) {
                        return opElement.getLocalName() != null
                            ? opElement.getLocalName()
                            : opElement.getTagName();
                    }
                }
            }
        } catch (Exception e) {
            // fall through
        }
        return null;
    }

    private Map<String, String> extractFieldsFromSoap(Exchange exchange) {
        Map<String, String> fields = new HashMap<>();
        try {
            String xml = getBodyAsString(exchange);
            if (xml == null) return fields;

            Document doc = parseXml(xml);
            if (doc == null) return fields;

            String[] fieldNames = {"firstName", "lastName", "dateOfBirth", "role"};
            for (String field : fieldNames) {
                NodeList nodes = doc.getElementsByTagName(field);
                if (nodes.getLength() > 0) {
                    fields.put(field, nodes.item(0).getTextContent().trim());
                }
            }
        } catch (Exception e) {
            // return what we have
        }
        return fields;
    }

    private String extractSearchTermFromSoap(Exchange exchange) {
        try {
            String xml = getBodyAsString(exchange);
            if (xml == null) return null;
            Document doc = parseXml(xml);
            if (doc == null) return null;
            NodeList nodes = doc.getElementsByTagName("searchTerm");
            if (nodes.getLength() > 0) {
                return nodes.item(0).getTextContent().trim();
            }
        } catch (Exception e) {
            // fall through
        }
        return null;
    }

    private LocalDate extractDateFromSoap(Exchange exchange, String tagName) {
        try {
            String xml = getBodyAsString(exchange);
            if (xml == null) return null;
            Document doc = parseXml(xml);
            if (doc == null) return null;
            NodeList nodes = doc.getElementsByTagName(tagName);
            if (nodes.getLength() > 0) {
                String value = nodes.item(0).getTextContent().trim();
                return LocalDate.parse(value);
            }
        } catch (Exception e) {
            // fall through
        }
        return null;
    }

    private Long extractLongFromBody(Exchange exchange, String tagName) {
        try {
            String xml = getBodyAsString(exchange);
            if (xml == null) return null;

            Document doc = parseXml(xml);
            if (doc == null) return null;

            NodeList nodes = doc.getElementsByTagName(tagName);
            if (nodes.getLength() > 0) {
                String value = nodes.item(0).getTextContent().trim();
                return Long.parseLong(value);
            }
        } catch (Exception e) {
            // fall through
        }
        return null;
    }

    private String getBodyAsString(Exchange exchange) {
        Object body = exchange.getIn().getBody();
        if (body instanceof String s) return s;
        if (body instanceof InputStream is) {
            try {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                return null;
            }
        }
        return body != null ? body.toString() : null;
    }

    private Document parseXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

    private Worker mapToWorker(Map<String, String> fields) {
        if (fields == null) return null;

        String firstName = fields.get("firstName");
        String lastName = fields.get("lastName");
        String dateOfBirth = fields.get("dateOfBirth");
        String role = fields.get("role");

        if (isBlank(firstName) || isBlank(lastName) || isBlank(dateOfBirth) || isBlank(role)) {
            return null;
        }

        LocalDate dob;
        try {
            dob = LocalDate.parse(dateOfBirth);
        } catch (Exception e) {
            return null;
        }

        Worker worker = new Worker();
        worker.setFirstName(firstName);
        worker.setLastName(lastName);
        worker.setDateOfBirth(dob);
        worker.setRole(role);
        return worker;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
