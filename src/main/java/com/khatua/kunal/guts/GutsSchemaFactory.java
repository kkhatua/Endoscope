package com.khatua.kunal.guts;
/**
 * Provides  
 */

import java.io.File;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.khatua.kunal.endoscope.EndoscopeDefault;

/**
 * Schema Loader Class to identify entries in a Guts data file
 * @author kkhatua
 *
 */
public class GutsSchemaFactory {
	private static File schemaSrcFile; //TODO: Define Convention for Naming and value acquisition
	private static DocumentBuilderFactory docFactory;
	private static DocumentBuilder docBuilder;
	private static Document lookupDoc;
	private static XPathFactory xFactory;
	private static XPath xpath;
	
	/**
	 * Set reference XML file for Schema interpretation
	 * @param fileName
	 */
	public static void setSchemaSource(String fileName) {
		File srcFile = new File(fileName);
		if (srcFile.isFile()) {
			schemaSrcFile = srcFile;
			lookupDoc = null;
			try {
				loadSchema();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	//Do Nothing
	private GutsSchemaFactory() {}

	/**
	 * Returns list of metrics associated with Id
	 * @param labelId
	 * @return
	 * @throws NoSuchFieldException
	 * @throws XPathExpressionException
	 */
	public static LinkedList<GutMetric> getMetric(String labelId) throws NoSuchFieldException, XPathExpressionException {
		LinkedList<GutMetric> listOfMetrics;

		//Defining Expression to capture ALL gutMetrics
		XPathExpression expr = xpath.compile("/guts/entity[@id='"+labelId+"']/gutMetric");
		NodeList metricsNL = (NodeList) expr.evaluate(lookupDoc,XPathConstants.NODESET);
		
		//Checking if NodeList?
		if (metricsNL.getLength() == 0) 
			throw new NoSuchFieldException(labelId);
		
		//Initializing
		listOfMetrics = new LinkedList<GutMetric>();
		
		for (int i=0; i < metricsNL.getLength(); i++) {
			Element metricElem = (Element) metricsNL.item(i);
			if (metricElem.getNodeType() == Element.ELEMENT_NODE) {
				GutMetric metric = buildMetric(metricElem);
				//dBug:System.out.println(metric.getName() + " : " + metric.getDescription() + " ("+metric.getUnit()+")");
		        listOfMetrics.add(metric);			
			}   			  
		}
		
		return listOfMetrics;
	}

	/**
	 * Construct a Gut Metric
	 * @param metricElem
	 * @return
	 */
	private static GutMetric buildMetric(Element metricElem) {
		GutMetric metric = new GutMetric();
		
		Node nameNode = metricElem.getElementsByTagName("name").item(0);
		Node descriptionNode = metricElem.getElementsByTagName("description").item(0);
		Node unitNode = metricElem.getElementsByTagName("unit").item(0);
//		System.out.println(nameNode.getTextContent());
//		System.out.println(descriptionNode.getTextContent());
//		System.out.println(unitNode.getTextContent());
		metric.setName(nameNode.getTextContent());
		metric.setDescription(descriptionNode.getTextContent());
		metric.setUnit(unitNode.getTextContent());
		
		return metric;
	}

	/**
	 * Loads Metrics XML 
	 */
	public/*private*/ static void loadSchema() throws Exception {
		//TODO: Where do I load defaults from if not provided?
		if (schemaSrcFile == null)
			schemaSrcFile = new File(EndoscopeDefault.METRICS_XML);
		docFactory = DocumentBuilderFactory.newInstance();
		docBuilder = docFactory.newDocumentBuilder();
		lookupDoc = docBuilder.parse(schemaSrcFile);
	    xFactory = XPathFactory.newInstance();
	    xpath = xFactory.newXPath();
	}
}