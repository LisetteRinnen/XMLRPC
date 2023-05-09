import java.io.*;
import java.net.*;
import java.net.http.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import org.w3c.dom.*;

import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;

import org.w3c.dom.Node;

/**
 * This approach uses the java.net.http.HttpClient classes, which
 * were introduced in Java11.
 */
public class Client {
  private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
  private static String host;
  private static String port;


  public static void main(String... args) throws Exception {
    host = args[0];
    port = args[1];

    System.out.println(add() == 0);
    System.out.println(add(1, 2, 3, 4, 5) == 15);
    System.out.println(add(2, 4) == 6);
    System.out.println(subtract(12, 6) == 6);
    System.out.println(multiply(3, 4) == 12);
    System.out.println(multiply(1, 2, 3, 4, 5) == 120);
    System.out.println(divide(10, 5) == 2);
    System.out.println(modulo(10, 5) == 0);

    try {
      add(84673582, 90109893);
    } catch (Exception e) {
      System.out.println("Received error on overflowing add: " + e.toString());
    }

    try {
      multiply(29165, 16582);
    } catch (Exception e) {
      System.out.println("Received error on overflowing multiply: " + e.toString());
    }

    try {
      subtract("hi", "bye");
    } catch (Exception e) {
      System.out.println("Received error on subtracting strings: " + e.toString());
    }

    try {
      divide(5, 0);
    } catch (Exception e) {
      System.out.println("Received error on dividing by 0: " + e.toString());
    }
  }


  public static int add(int lhs, int rhs) throws Exception {
    Object[] params = { lhs, rhs };
    String response = sendRequest("add", params);
    return parseResponse(response);
  }


  public static int add(Integer... params) throws Exception {
    String response = sendRequest("add", params);
    return parseResponse(response);
  }


  public static int subtract(int lhs, int rhs) throws Exception {
    Object[] params = { lhs, rhs };
    String response = sendRequest("subtract", params);
    return parseResponse(response);
  }

  public static int subtract(String lhs, String rhs) throws Exception {
    Object[] params = { lhs, rhs };
    String response = sendRequest("subtract", params);
    return parseResponse(response);
  }


  public static int multiply(int lhs, int rhs) throws Exception {
    Object[] params = { lhs, rhs };
    String response = sendRequest("multiply", params);
    return parseResponse(response);
  }


  public static int multiply(Integer... params) throws Exception {
    String response = sendRequest("multiply", params);
    return parseResponse(response);
  }


  public static int divide(int lhs, int rhs) throws Exception {
    Object[] params = { lhs, rhs };
    String response = sendRequest("divide", params);
    return parseResponse(response);
  }


  public static int modulo(int lhs, int rhs) throws Exception {
    Object[] params = { lhs, rhs };
    String response = sendRequest("modulo", params);
    return parseResponse(response);
  }


  private static String sendRequest(String methodName, Object[] params) throws Exception {
    // Create the xml formatted body
    String xmlRequest = buildXML(methodName, params);
    String length = Integer.toString(xmlRequest.getBytes().length);

    // build and send the rpc request
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://" + host + ":" + port + "/RPC"))
            .header("User-Agent", "TedsTerrifyingThinkers")
            // .header("Host", "localhost")
            .header("Content-Type", "text/xml")
            // .header("Content-Length", length)
            .POST(BodyPublishers.ofString(xmlRequest))
            .build();

    // Receive the response
    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
    String xmlResponse = response.body();

    return xmlResponse;
  }


  private static int parseResponse(String response) throws Exception {
    // Create a readable xml document
    DocumentBuilder db = dbf.newDocumentBuilder();
    InputStream input = new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
    Document doc = db.parse(input);
    XPath xpath = XPathFactory.newInstance().newXPath();

    if (doc.getElementsByTagName("fault").getLength() > 0) {
      String faultString = doc.getElementsByTagName("string").item(0).getTextContent();
      throw new Exception("Server threw an exception: " + faultString);

    } else {
      NodeList nodeList = doc.getElementsByTagName("methodResponse");
      // NodeList nodeList = (NodeList) xPath.compile("")
      Node resultNode = nodeList.item(0);
      System.out.println(doc);

      return Integer.parseInt(resultNode.getTextContent());
    }
  }


  private static String buildXML(String methodName, Object[] params) {
    StringBuilder sb = new StringBuilder();
    sb.append("<?xml version=\"1.0\"?>\n");

    sb.append("<methodCall>\n");
    sb.append("<methodName>").append(methodName).append("</methodName>\n");
    sb.append("<params>\n");

    for (Object value : params) {
      sb.append("<param>\n");
      sb.append("<value><i4>").append(value).append("</i4></value>\n");
      sb.append("</param>\n");
    }

    sb.append("</params>\n");
    sb.append("</methodCall>");

    return sb.toString();
  }
}
