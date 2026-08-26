package com.company.soaptranslator.camel.processor;

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

@ApplicationScoped
@Named("soapHeaderAuthProcessor")
public class SoapHeaderAuthProcessor implements Processor {

    private static final String WSSE_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

    @Override
    public void process(Exchange exchange) throws Exception {
        String xml = getBodyAsString(exchange);
        if (xml == null || xml.isBlank()) {
            setFault(exchange, "WS-Security credentials missing");
            return;
        }

        Document doc = parseXml(xml);
        if (doc == null) {
            setFault(exchange, "Invalid SOAP XML");
            return;
        }

        String username = extractByUsernamePath(doc, "Username");
        String password = extractByUsernamePath(doc, "Password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            setFault(exchange, "WS-Security credentials missing");
            return;
        }

        // Store credentials — EJB Remote security is handled by the Payara container
        exchange.getIn().setHeader("authenticatedUser", username);
        exchange.getIn().setHeader("authenticatedPassword", password);
    }

    private String extractByUsernamePath(Document doc, String localName) {
        NodeList tokens = doc.getElementsByTagNameNS(WSSE_NS, "UsernameToken");
        if (tokens.getLength() == 0) return null;

        Element token = (Element) tokens.item(0);
        NodeList children = token.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child) {
                String name = child.getLocalName() != null ? child.getLocalName() : child.getTagName();
                if (name.equals(localName)) {
                    return child.getTextContent().trim();
                }
            }
        }
        return null;
    }

    private void setFault(Exchange exchange, String message) {
        String faultXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body><Fault><faultcode>Client</faultcode>"
            + "<faultstring>" + message + "</faultstring>"
            + "</Fault></soap:Body></soap:Envelope>";

        exchange.getIn().setBody(faultXml);
        exchange.getIn().setHeader("authFailed", true);
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
}
