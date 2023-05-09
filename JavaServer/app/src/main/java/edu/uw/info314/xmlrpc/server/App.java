package edu.uw.info314.xmlrpc.server;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.logging.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLReporter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static spark.Spark.*;

class Call {
    public String name;
    public List<Object> args = new ArrayList<Object>();
}

public class App {
  public static final Logger LOG = Logger.getLogger(App.class.getCanonicalName());

  public static void main(String[] args) {
    LOG.info("Starting up on port 8080");
    port(8080);

    // ***FIX***
    // How to I test if the path is wrong?

    // path("/", () -> {
    //   before((request, response) -> {
    //     if (!request.contextPath().equals("/RPC")) {
    //       halt(404, "Path Not Found");
    //     }
    //   });
    // });

    post("/RPC", (request, response) -> {
      Call call = extractXMLRPCCall(response.body());

      try {
        int result = 0;
        if (call.name.equals("add")) {
          result = handleAdd(call.args);

        } else if (call.name.equals("subtract")) {
          result = handleSubtract(call.args);

        } else if (call.name.equals("multiply")) {
          result = handleMultiply(call.args);

        } else if (call.name.equals("divide")) {
          result = handleDivide(call.args);

        } else if (call.name.equals("modulo")) {
          result = handleModulo(call.args);
        }

        String xmlResponse = buildXML(result);
        response.status(200);
        response.header("Content-Length", xmlResponse.getBytes().length);
        response.header("Content-Type", "text/xml");
        LOG.info(xmlResponse);
        return xmlResponse;

      } catch (SAXException e) {
        String xmlResponse = buildXMLFault(3, "Illegal Argument Type");
        response.status(200);
        response.header("Content-Length", xmlResponse.getBytes().length);
        response.header("Content-Type", "text/xml");
        return xmlResponse;

      } catch (ArithmeticException e) {
        String xmlResponse = buildXMLFault(1, "Divide by Zero");
        response.status(200);
        response.header("Content-Length", xmlResponse.getBytes().length);
        response.header("Content-Type", "text/xml");
        return xmlResponse;
      }

    });

    get("/*", (request, response) -> {
      response.status(405);
      return "Method Not Supported";
    });

    put("/*", (request, response) -> {
      response.status(405);
      return "Method Not Supported";
    });

    delete("/*", (request, response) -> {
      response.status(405);
      return "Method Not Supported";
    });

    // Each of the verbs has a similar format: get() for GET,
    // put() for PUT, delete() for DELETE. There's also an exception()
    // for dealing with exceptions thrown from handlers.
    // All of this is documented on the SparkJava website (https://sparkjava.com/).

  }

  public static Call extractXMLRPCCall(String xmlBody) throws Exception {
    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = builder.parse(new ByteArrayInputStream(xmlBody.getBytes()));

    // Element methodCall = (Element) doc.getElementsByTagName("methodCall").item(0);

    // Get the list of parameters
    String name = doc.getElementsByTagName("methodName").item(0).getTextContent();
    NodeList paramElements = doc.getElementsByTagName("params");

    List<Object> params = new ArrayList<Object>();
    for (int i = 0; i < paramElements.getLength(); i++) {
      Object param = Integer.parseInt(paramElements.item(i).getTextContent());
      params.add(param);
    }

    Call call = new Call();
    call.name = name;
    call.args = params;

    return call;
  }

  private static String buildXML(int value) {
    StringBuilder sb = new StringBuilder();
    sb.append("<?xml version=\"1.0\"?>\n");

    sb.append("<methodResponse>\n");
    sb.append("<params>");
    sb.append("<param>\n");
    sb.append("<value><i4>").append(value).append("</i4></value>\n");
    sb.append("</param>");
    sb.append("</params>\n");
    sb.append("</methodResponse>");

    return sb.toString();
  }

  private static String buildXMLFault(int faultCode, String faultString) {
    StringBuilder sb = new StringBuilder();
    sb.append("<?xml version=\"1.0\"?>\n");

    sb.append("<methodResponse>\n");
    sb.append("<fault>\n");
    sb.append("<value>\n");
    sb.append("<struct>\n");

    sb.append("<member>\n");
    sb.append("<name>faultCode</name>");
    sb.append("<value><int>").append(faultCode).append("</int></value>\n");
    sb.append("</member>\n");

    sb.append("<member>\n");
    sb.append("<name>faultString</name>");
    sb.append("<value><String>").append(faultString).append("</String></value>\n");
    sb.append("</member>\n");

    sb.append("</struct>\n");
    sb.append("</value>\n");
    sb.append("</fault>\n");
    sb.append("</methodResponse>");

    return sb.toString();
  }

  private static int handleAdd(List<Object> params) throws SAXException {
    Calc calc = new Calc();

    int[] paramArr = new int[params.size()];
    for (int i = 0; i < params.size(); i++) {
      // Exception: input from xml is not of type interger (i4)
      if (!(params.get(i) instanceof Integer)) {
        throw new SAXException();
      }
      paramArr[i] = Integer.parseInt(params.get(i).toString());
    }

    return calc.add(paramArr);
  }

  private static int handleSubtract(List<Object> params) throws SAXException {
    // Exception: input from xml is not of type interger (i4)
    if (params.size() != 2 || !(params.get(0) instanceof Integer) || !(params.get(1) instanceof Integer)) {
      throw new SAXException();
    }

    Calc calc = new Calc();
    int lhs = Integer.parseInt(params.get(0).toString());
    int rhs = Integer.parseInt(params.get(1).toString());

    return calc.subtract(lhs, rhs);
  }

  private static int handleMultiply(List<Object> params) throws SAXException {
    Calc calc = new Calc();

    int[] paramArr = new int[params.size()];
    for (int i = 0; i < params.size(); i++) {
      // Exception: input from xml is not of type interger (i4)
      if (!(params.get(i) instanceof Integer)) {
        throw new SAXException();
      }
      paramArr[i] = Integer.parseInt(params.get(i).toString());
    }

    return calc.multiply(paramArr);
  }

  private static int handleDivide(List<Object> params) throws SAXException, ArithmeticException {
    // Exception: input from xml is not of type interger (i4)
    if (params.size() != 2 || !(params.get(0) instanceof Integer) || !(params.get(1) instanceof Integer)) {
      throw new SAXException();
    }

    Calc calc = new Calc();
    int lhs = Integer.parseInt(params.get(0).toString());
    int rhs = Integer.parseInt(params.get(1).toString());

    // Exception: Dividing by 0
    if (rhs == 0) {
      throw new ArithmeticException();
    }

    return calc.divide(lhs, rhs);
  }

  private static int handleModulo(List<Object> params) throws SAXException, ArithmeticException {
    // Exception: input from xml is not of type interger (i4)
    if (params.size() != 2 || !(params.get(0) instanceof Integer) || !(params.get(1) instanceof Integer)) {
      throw new SAXException();
    }

    Calc calc = new Calc();
    int lhs = Integer.parseInt(params.get(0).toString());
    int rhs = Integer.parseInt(params.get(1).toString());

    // Exception: Dividing by 0
    if (rhs == 0) {
      throw new ArithmeticException();
    }

    return calc.modulo(lhs, rhs);
  }
}
