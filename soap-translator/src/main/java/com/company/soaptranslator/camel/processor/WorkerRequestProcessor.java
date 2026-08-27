package com.company.soaptranslator.camel.processor;

import com.company.workerportal.service.AuthRequest;
import com.company.workerportal.service.WorkerDTO;
import com.company.workerportal.service.WorkerServiceRemote;
import jakarta.ejb.EJB;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@Named("workerRequestProcessor")
public class WorkerRequestProcessor implements Processor {

    @EJB(lookup = "java:global/worker-portal/WorkerService!com.company.workerportal.service.WorkerServiceRemote")
    private WorkerServiceRemote workerService;

    @Override
    public void process(Exchange exchange) throws Exception {
        String operation = exchange.getIn().getHeader("operation", String.class);

        if (operation == null || operation.isBlank()) {
            operation = extractOperationFromSoapBody(exchange);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("operation", operation);

        AuthRequest caller = new AuthRequest();
        caller.setUsername(exchange.getIn().getHeader("authenticatedUser", String.class));
        caller.setPassword(exchange.getIn().getHeader("authenticatedPassword", String.class));

        try {
            switch (operation) {
                case "getAllWorkers" -> {
                    String searchTerm = extractTextFromSoap(exchange, "searchTerm");
                    String dateFromStr = extractTextFromSoap(exchange, "dateFrom");
                    String dateToStr = extractTextFromSoap(exchange, "dateTo");
                    LocalDate dateFrom = dateFromStr != null ? LocalDate.parse(dateFromStr) : null;
                    LocalDate dateTo = dateToStr != null ? LocalDate.parse(dateToStr) : null;

                    boolean hasFilter = (searchTerm != null && !searchTerm.isBlank())
                        || dateFrom != null || dateTo != null;

                    WorkerDTO[] workers = hasFilter
                        ? workerService.searchWorkers(caller, searchTerm, null, null, false, dateFrom, dateTo)
                        : workerService.getAllWorkers(caller);
                    result.put("data", workers);
                    result.put("success", true);
                }
                case "getWorkerById" -> {
                    Long id = extractId(exchange);
                    WorkerDTO worker = workerService.getWorkerById(caller, id);
                    result.put("data", worker);
                    result.put("success", worker != null);
                }
                case "addWorker" -> {
                    WorkerDTO worker = extractWorkerFromSoap(exchange);
                    if (worker != null) {
                        Long newId = workerService.addWorker(caller, worker);
                        result.put("data", newId != null ? newId : -1L);
                        result.put("success", newId != null);
                    } else {
                        result.put("data", -1L);
                        result.put("success", false);
                    }
                }
                case "updateWorker" -> {
                    Long id = extractId(exchange);
                    WorkerDTO worker = extractWorkerFromSoap(exchange);
                    if (worker != null && id != null) {
                        boolean success = workerService.updateWorker(caller, id, worker);
                        result.put("data", success);
                        result.put("success", success);
                    } else {
                        result.put("data", false);
                        result.put("success", false);
                    }
                }
                case "deleteWorker" -> {
                    Long id = extractId(exchange);
                    boolean success = workerService.deleteWorker(caller, id);
                    result.put("data", success);
                    result.put("success", success);
                }
                default -> {
                    result.put("data", null);
                    result.put("success", false);
                    result.put("error", "Unknown operation: " + operation);
                }
            }
        } catch (Exception e) {
            result.put("data", null);
            result.put("success", false);
            result.put("error", e.getMessage() != null ? e.getMessage() : e.toString());
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

    private String extractTextFromSoap(Exchange exchange, String tagName) {
        try {
            String xml = getBodyAsString(exchange);
            if (xml == null) return null;
            Document doc = parseXml(xml);
            if (doc == null) return null;
            NodeList nodes = doc.getElementsByTagName(tagName);
            if (nodes.getLength() > 0) {
                String value = nodes.item(0).getTextContent().trim();
                return value.isEmpty() ? null : value;
            }
        } catch (Exception e) {
            // fall through
        }
        return null;
    }

    private Long extractId(Exchange exchange) {
        String idStr = exchange.getIn().getHeader("id", String.class);
        if (idStr != null && !idStr.isBlank()) {
            return Long.parseLong(idStr);
        }
        String bodyId = extractTextFromSoap(exchange, "id");
        if (bodyId != null) {
            return Long.parseLong(bodyId);
        }
        return null;
    }

    private WorkerDTO extractWorkerFromSoap(Exchange exchange) {
        String firstName = extractTextFromSoap(exchange, "firstName");
        String lastName = extractTextFromSoap(exchange, "lastName");
        String dateOfBirth = extractTextFromSoap(exchange, "dateOfBirth");
        String role = extractTextFromSoap(exchange, "role");

        if (firstName == null || lastName == null || dateOfBirth == null || role == null) {
            return null;
        }

        try {
            WorkerDTO worker = new WorkerDTO();
            worker.setFirstName(firstName);
            worker.setLastName(lastName);
            worker.setDateOfBirth(dateOfBirth);
            worker.setRole(role);
            return worker;
        } catch (Exception e) {
            return null;
        }
    }

    private String getBodyAsString(Exchange exchange) {
        Object body = exchange.getIn().getBody();
        if (body instanceof String s) return s;
        if (body instanceof java.io.InputStream is) {
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
}
