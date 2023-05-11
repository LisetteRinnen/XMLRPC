package edu.uw.info314.xmlrpc.server;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.logging.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static spark.Spark.*;
import spark.Filter;

import java.io.StringWriter;
import java.io.PrintWriter;

class Call {
    public String name;
    public List<Integer> args = new ArrayList<Integer>();
}

public class App {
  public static final Logger LOG = Logger.getLogger(App.class.getCanonicalName());

  public static void main(String[] args) {
    LOG.info("Starting up on port 8080");
    port(8080);

    post("/*", (request, response) -> {
      if (!request.pathInfo().equals("/RPC")) {
        String xmlResponse = buildXMLFault(404, "404: Not Found");
        response.status(404);
        response.header("Content-Type", "text/xml");
        return xmlResponse;
      } else {
        try {
          Call call = extractXMLRPCCall(request.body());
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
          response.header("Content-Type", "text/xml");
          return xmlResponse;

        } catch (NumberFormatException | SAXException e) {
          String xmlResponse = buildXMLFault(3, "Illegal Argument Type");
          response.status(200);
          response.header("Content-Type", "text/xml");
          return xmlResponse;

        } catch (ArithmeticException e) {
          String xmlResponse = buildXMLFault(1, "Divide by Zero");
          response.status(200);
          response.header("Content-Type", "text/xml");
          return xmlResponse;

        } catch (RuntimeException e) {
          String xmlResponse = buildXMLFault(2, "Overflow");
          response.status(200);
          response.header("Content-Type", "text/xml");
          return xmlResponse;

        } catch (Exception e) {
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
          String sStackTrace = sw.toString(); // stack trace as a string
          return sStackTrace;
        }
      }
    });

    get("/*", (request, response) -> {
      String xmlResponse = buildXMLFault(405, "405: Method Not Supported");
      response.status(405);
      response.header("Content-Type", "text/xml");
      return xmlResponse;
    });

    put("/*", (request, response) -> {
      String xmlResponse = buildXMLFault(405, "405: Method Not Supported");
      response.status(405);
      response.header("Content-Type", "text/xml");
      return xmlResponse;
    });

    delete("/*", (request, response) -> {
      String xmlResponse = buildXMLFault(405, "405: Method Not Supported");
      response.status(405);
      response.header("Content-Type", "text/xml");
      return xmlResponse;
    });
  }


  public static Call extractXMLRPCCall(String xmlBody) throws Exception, NumberFormatException {
    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = builder.parse(new ByteArrayInputStream(xmlBody.getBytes()));
    XPath xPath = XPathFactory.newInstance().newXPath();

    Node nameElement = (Node) xPath.compile("/methodCall/methodName").evaluate(
      doc, XPathConstants.NODE);
    String name = nameElement.getTextContent();

    // Get the list of parameters
    NodeList paramElements = (NodeList) xPath.compile("/methodCall/params/param/value").evaluate(
      doc, XPathConstants.NODESET);

    List<Integer> params = new ArrayList<Integer>();
    for (int i = 0; i < paramElements.getLength(); i++) {
      int param = Integer.parseInt(paramElements.item(i).getTextContent());
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
    sb.append("<value><i4>").append(faultCode).append("</i4></value>\n");
    sb.append("</member>\n");

    sb.append("<member>\n");
    sb.append("<name>faultString</name>");
    sb.append("<value><string>").append(faultString).append("</string></value>\n");
    sb.append("</member>\n");

    sb.append("</struct>\n");
    sb.append("</value>\n");
    sb.append("</fault>\n");
    sb.append("</methodResponse>");

    return sb.toString();
  }


  private static int handleAdd(List<Integer> params) throws SAXException, RuntimeException {
    long[] paramArr = new long[params.size()];
    for (int i = 0; i < params.size(); i++) {
      // Exception: input from xml is not of type interger (i4)
      if (!(params.get(i) instanceof Integer)) {
        throw new SAXException();
      }
      paramArr[i] = Long.parseLong(params.get(i).toString());
    }

    // Manually perform the add
    // Need to check for overflow
    long result = 0;
    for (long arg : paramArr) { result += arg; }

    // Exception: result integer is above max_value
    if (result > Integer.MAX_VALUE) {
      throw new RuntimeException();
    }

    return (int) result;
  }


  private static int handleSubtract(List<Integer> params) throws SAXException {
    // Exception: input from xml is not of type interger (i4)
    if (params.size() != 2 || !(params.get(0) instanceof Integer) || !(params.get(1) instanceof Integer)) {
      throw new SAXException();
    }

    Calc calc = new Calc();
    int lhs = Integer.parseInt(params.get(0).toString());
    int rhs = Integer.parseInt(params.get(1).toString());

    return calc.subtract(lhs, rhs);
  }


  private static int handleMultiply(List<Integer> params) throws SAXException, RuntimeException {
    long[] paramArr = new long[params.size()];
    for (int i = 0; i < params.size(); i++) {
      // Exception: input from xml is not of type interger (i4)
      if (!(params.get(i) instanceof Integer)) {
        throw new SAXException();
      }
      paramArr[i] = Long.parseLong(params.get(i).toString());
    }

    // Manually perform the multiply
    // Calc.java starts the result = 0. Anything times 0 equals 0
    // Need to check for overflow
    long result = 1;
    for (long arg : paramArr) { result *= arg; }

    // Exception: result integer above max_value
    if (result > Integer.MAX_VALUE) {
      throw new RuntimeException();
    }

    return (int) result;
  }


  private static int handleDivide(List<Integer> params) throws SAXException, ArithmeticException {
    // Exception: input from xml is not of type interger (i4)
    if (params.size() != 2 || !(params.get(0) instanceof Integer) || !(params.get(1) instanceof Integer)) {
      throw new SAXException();
    }

    int lhs = Integer.parseInt(params.get(0).toString());
    int rhs = Integer.parseInt(params.get(1).toString());

    // Exception: Dividing by 0
    if (rhs == 0) {
      throw new ArithmeticException();
    }

    Calc calc = new Calc();
    return calc.divide(lhs, rhs);
  }


  private static int handleModulo(List<Integer> params) throws SAXException, ArithmeticException {
    // Exception: input from xml is not of type interger (i4)
    if (params.size() != 2 || !(params.get(0) instanceof Integer) || !(params.get(1) instanceof Integer)) {
      throw new SAXException();
    }

    int lhs = Integer.parseInt(params.get(0).toString());
    int rhs = Integer.parseInt(params.get(1).toString());

    // Exception: Dividing by 0
    if (rhs == 0) {
      throw new ArithmeticException();
    }

    Calc calc = new Calc();
    return calc.modulo(lhs, rhs);
  }
}
