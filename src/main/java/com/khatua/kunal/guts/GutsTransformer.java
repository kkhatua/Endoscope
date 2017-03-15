package com.khatua.kunal.guts;

import java.io.File;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.script.ScriptEngineManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.khatua.kunal.endoscope.AbstractTransformEngine;
import com.khatua.kunal.endoscope.EndoscopeConfKeyword;
import com.khatua.kunal.endoscope.EndoscopeDefault;
import com.khatua.kunal.endoscope.EndoscopeProperties;
import com.khatua.kunal.endoscope.NoSuchFormulaException;

/** 
 * Transformer Class for translating MapR guts' inputs into desired outputs
 * @author kkhatua
 */
public class GutsTransformer extends AbstractTransformEngine {
	//TODO: Make this an option in EndoscopeConfKeyword
	static DecimalFormat format = new DecimalFormat(EndoscopeDefault.NUMERIC_VALUE_FORMAT);

	private static File schemaSrcFile;
	private static DocumentBuilderFactory docFactory;
	private static DocumentBuilder docBuilder;
	private static Object lookupDoc;
	private static XPathFactory xFactory;
	private static XPath xpath;

	private static String defaultValue;
	private File transformFile;
	/** @deprecated */
	HashSet<String> requiredFormulaInputs;
	private LinkedHashMap<String, GutMetric> inputMetricMap;
	private LinkedHashMap<String,String> xFormationMap;
	private GutsParser parserHandle;

	/* Takes transform file to understand what is to be transformed.
	 * This will identify required inputs and list them
	 * 
	 */
	public GutsTransformer(String transformFileName) {
		schemaSrcFile = new File(transformFileName);
		requiredFormulaInputs = new HashSet<String>(); 

		try {
			loadSchema();
			inferRequiredInputs();
		} catch (Exception e) {
			e.printStackTrace();
		}

		jsEngineMgr = new ScriptEngineManager();
		jsEngine = jsEngineMgr.getEngineByName("JavaScript");
	}

	/**
	 * Loads Transform XML 
	 * @param schemaSrcFile 
	 */
	private static void loadSchema() throws Exception {
		docFactory = DocumentBuilderFactory.newInstance();
		docBuilder = docFactory.newDocumentBuilder();
		lookupDoc = docBuilder.parse(schemaSrcFile);
		xFactory = XPathFactory.newInstance();
		xpath = xFactory.newXPath();
		defaultValue = EndoscopeProperties.getProperty(EndoscopeConfKeyword.DEFAULT_VALUE, EndoscopeDefault.DEFAULT_VALUE);
	}

	/**
	 * Sets the inputs from the Guts Parser
	 * TODO: Apply Input LnkdHashMap
	 * This will check against list of required inputs (i.e. requiredFormulaInputs is a subset of setInputs)
	 * @param parser
	 * @throws NoSuchFieldException
	 * @throws XPathExpressionException
	 */
	public void setInputs(GutsParser parser) throws NoSuchFieldException, NoSuchFormulaException, XPathExpressionException {
		//Apply handle to Parser
		this.parserHandle = parser;
		//Read
		LinkedHashMap<String, GutMetric> availableInputs = parserHandle.getMetricMap();
		System.out.println("[FUNC] Setting up input");
		inputIndexLookup = new HashMap<String, Integer>(availableInputs.size());
		Iterator<String> iter = availableInputs.keySet().iterator();
		int currIndex = 0;

		while (iter.hasNext()) {
			String column = (String) iter.next();
			//			System.out.print(column + " ");			

			//Indexing location of column
			//dBug: System.out.println("Indexed "+column+" at "+currIndex);
			inputIndexLookup.put(column, currIndex++);

			//Eliminating required inputs
			//			System.out.println("Removed? " + requiredFormulaInputs.remove(column));
			requiredFormulaInputs.remove(column);

		}
		//		System.out.println("\n--Scanned Cols-- : " + availableInputs.size());

		//Make this While Loop
		if (!requiredFormulaInputs.isEmpty()) {
			System.out.println("[WARNING] Variable needed??");
			iter = requiredFormulaInputs.iterator();
			while (iter.hasNext()) {
				String missingInput = iter.next();
				System.out.println("?" + missingInput);
				try {
					throw new NoSuchFieldException("Input = "+missingInput);
				} catch (NoSuchFieldException e) {
					System.err.println(e);
				}
			}
		}
		else 
			System.out.println("[INFO] No intermediate variables needed!");

		inputMetricMap = availableInputs;

		//Setting Outputs based on Inputs
		setOutputs(inputMetricMap);
	}

	/**
	 * Sets the outputs of the Transformer
	 * TODO: Handle time and CPU summary
	 * @param mapOfInputs
	 * @throws XPathExpressionException
	 * @throws NoSuchFieldException
	 */
	public void setOutputs(LinkedHashMap<String,GutMetric> mapOfInputs) throws XPathExpressionException, NoSuchFieldException {
		System.out.println("[INFO] Setting Outputs!!");

		//Preparing transformation map 
		xFormationMap = new LinkedHashMap<String,String>();

		boolean passThroughAllInputs = EndoscopeProperties.getBooleanProperty(EndoscopeConfKeyword.INPUT_PASS_ALL, EndoscopeDefault.INPUTS_PASS_ALL);
		if (passThroughAllInputs) {
			for (String inputPort : mapOfInputs.keySet()) {
				/* 
				 * FIXME: What about 
				 * #1. Date/Clock (block evaluation?)
				 * #2. CPU 
				 */
				if (!GutMetric.isCPUMetric(inputPort))  {//Skip CPU
					//[KK] 
					System.out.println("\t Implicit PassTroo:: " + inputPort);
					xFormationMap.put(inputPort, inputPort);
				}
			}
		}

		//Implicitly setting columns provided in flags
		for (String inputCol : mapOfInputs.keySet()) {
			//Implicitly setting time column for output
			if (GutMetric.isTimeMetric(inputCol)) {
				//?mapOfInputs.put(inputCol, GutMetric.genTimeMetric(inputCol));
				//?replaceParserOutputTime(inputCol);
				//Update GutsParser
				//?parserHandle.setOutputTimeFormat(inputCol);
				//Create info from 
				//				System.out.println("\t PassThroo:: " + inputCol);
				//				xFormationMap.put(inputCol, inputCol);
			}
			//Implicitly setting time column for output
			if (GutMetric.isCPUMetric(inputCol)) {
				/*OLD:
				System.out.println("\t PassThrough:: " + inputCol);
				xFormationMap.put(inputCol, inputCol);
				 */
				//NEW: Set CPU Output
			}
		}


		//Defining Expression to capture ALL gutMetrics
		XPathExpression expr; // = xpath.compile("/translate/metric/formula");
		NodeList outputNL; // = (NodeList) expr.evaluate(lookupDoc,XPathConstants.NODESET);

		//Extracting ALL outputs
		expr = xpath.compile("/translate/metric");
		outputNL = (NodeList) expr.evaluate(lookupDoc,XPathConstants.NODESET);
		for (int i = 0; i < outputNL.getLength(); i++) {
			Element elem = (Element) outputNL.item(i);
			String outputPort = elem.getAttribute("id");

			//[KK] System.out.println("[INFO] Output #"+ i + " >->" + outputPort);
			Element formulaElem = (Element) elem.getElementsByTagName("formula").item(0);

			//Check if no formula is needed.. i.e. pass-through
			if (formulaElem == null) {
				//Check if TimeMetric defined for output is missing. We'll override setting for Guts Input
				if (GutMetric.isTimeMetric(outputPort)) {
					System.out.println("UNK OP:" + outputPort);
					mapOfInputs.put(outputPort, GutMetric.createTimeMetric(outputPort));
					replaceParserOutputTime(outputPort);
					//Update GutsParser
					parserHandle.setOutputTimeFormat(outputPort);					
				}
				if (!mapOfInputs.containsKey(outputPort)) {
					/**
					 * FIXME: Should we insist on specifying CPU? 
					 * How about we check for CPU/Time specific outputs and replace it? 
					 */

					System.out.println("[WARN] NoSuchFieldException(PassThru = " + outputPort+")");
					continue;
					//					throw new NoSuchFieldException("PassThru = " + outputPort);
				}

				//Create info from 
				//[KK] 
				System.out.println("\t PassThru:: " + outputPort);
				xFormationMap.put(outputPort, outputPort);
			} else {
				if (validFormulaInputs(formulaElem.getTextContent(),mapOfInputs)) {
					//[KK] 
					System.out.println("\t Formula:: " + formulaElem.getTextContent());
					xFormationMap.put(outputPort, formulaElem.getTextContent());
				} else {
					System.out.println("[WARN] NoSuchFormulaException(Formula = " + formulaElem.getTextContent()+")");
					continue;
				}
			}				
		}

		//Update previously unvalidated formula? DINN
	}

	/**
	 * Get the output headers
	 * @return
	 */
	public String[] getOutputHeaders() {
		//Returning list of headers
		return xFormationMap.keySet().toArray(new String[xFormationMap.size()]);
	}

	/**
	 * Transform the inputs to get output
	 * @param input
	 * @return
	 */
	public String[] transformInput(String[] input) {
		if (input == null) return null;

		String outputs[] = getOutputHeaders(); //Place holder for

		for (int i = 0; i < outputs.length; i++) {
			//Extract
			String output = outputs[i];
			//Get Formula
			String formula = xFormationMap.get(output);
			//Evaluate
			//TODO: System.out.println(formula + " Needs:  "+ needsEvaluation(formula));
			if (needsEvaluation(formula)) {
				outputs[i] = evaluate(input, formula, defaultValue);
			}
			else 
				outputs[i] = input[inputIndexLookup.get(formula.trim())];
		}

		return outputs;
	}

	/**
	 * Test for formula needing evaluation (involves computation)
	 * TODO: Check for Date format??
	 * @param formula
	 * @return
	 */
	private boolean needsEvaluation(String formula) {
		/*if (formula.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {//Assuming Date
			System.out.println(formula + " is a date");
			return false;
		}
		else */
		if (formula.replaceAll("[a-zA-Z_]*", "")/*TODO:'date clock'.trim()*/.length() > 0)
			return true;
		else
			return false;
	}

	/**
	 * Validates if a given formula is valid based on the inputs
	 * @param formulaToValidate
	 * @param availableInputMap
	 * @return
	 */
	private boolean validFormulaInputs(String formulaToValidate, LinkedHashMap<String, GutMetric> availableInputMap) {
		String[] tokens = AbstractTransformEngine.extractTokens(formulaToValidate);
		for (String token : tokens) 
			if (!availableInputMap.containsKey(token))
				return false;
		return true;
	}

	/**
	 * TODO
	 * @param outputPort
	 */
	private void replaceParserOutputTime(String outputPort) {
		if (inputIndexLookup.containsKey(EndoscopeDefault.XFORM_XML_UNIX_TIME)) 
			inputIndexLookup.put(outputPort, inputIndexLookup.remove(EndoscopeDefault.XFORM_XML_UNIX_TIME));
		else if (inputIndexLookup.containsKey(EndoscopeDefault.XFORM_XML_ELAPSED_TIME)) 
			inputIndexLookup.put(outputPort, inputIndexLookup.remove(EndoscopeDefault.XFORM_XML_ELAPSED_TIME));
		else if (inputIndexLookup.containsKey(EndoscopeDefault.XFORM_XML_ELAPSED_CLOCK)) 
			inputIndexLookup.put(outputPort, inputIndexLookup.remove(EndoscopeDefault.XFORM_XML_ELAPSED_CLOCK));
		else 
			System.err.println("OutputPort didnt find match!!");			 
	}

	/**
	 * Gets the Transformation Map
	 * TODO: Who needs this??
	 * @param input
	 * @return
	 * @deprecated
	 */
	public HashMap<String, String> getTransformMap(String[] input) {
		if (input == null) return null;

		LinkedHashMap<String, String> xformMap = new LinkedHashMap<String, String>(xFormationMap.size());

		String headers[] = getOutputHeaders(); //Place holder for
		for (int i = 0; i < headers.length; i++) {
			//Extract
			String header = headers[i];
			//Get Formula
			String formula = xFormationMap.get(header);
			//Evaluate
			try {
				xformMap.put(header, evaluate(input, formula, defaultValue));
				System.out.println(header +" = "+ evaluate(input, formula, defaultValue) );
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println(header + " evaluate("+input+", "+formula+")");
			}
		}
		return xformMap;
	}

	/**
	 * Infers required inputs 
	 * @throws NoSuchFieldException
	 * @throws XPathExpressionException
	 * @deprecated
	 */
	private void inferRequiredInputs()  throws NoSuchFieldException, XPathExpressionException {
		//dBug::
		System.out.println("[INFO] Infering Inputs!!");

		//TODO
		//		if (System.getProperty(EndoscopeConfKeyword.OUTPUT_TIME_FORMAT) != null) {
		System.out.println("Output Wanted: " + System.getProperty(EndoscopeConfKeyword.OUTPUT_TIME_FORMAT));
		//		}		

		//Defining Expression to capture ALL gutMetrics
		XPathExpression expr; // = xpath.compile("/translate/metric/formula");
		NodeList formulaeNL; // = (NodeList) expr.evaluate(lookupDoc,XPathConstants.NODESET);

		//Reduce formulae to the basic inputs
		//Map to track formulae that refer to other formulae
		HashMap<String,String> selfies = new HashMap<String,String>();
		do {
			expr = xpath.compile("/translate/metric/formula");
			formulaeNL = (NodeList) expr.evaluate(lookupDoc,XPathConstants.NODESET);

			//Checking if NodeList of Formulae is empty?
			if (formulaeNL.getLength() == 0) return;

			/* TODO: Removes self-references. Do I care?			
				//Clear Selfies
				selfies.clear();

				//Get List of Selfies (Self-referencing Formulae) FIXME: Inner in DB?
				selfies = getListOfSelfies(formulaeNL); 

				//Replace
				substituteSelfies(selfies, formulaeNL);
			 */
			//			//dBUG:
			//			for (int i = 0; i < formulaeNL.getLength(); i++) {
			//				System.out.println("Formula #"+ i + " >->" + formulaeNL.item(i).getTextContent());
			//			}

		} while (!selfies.isEmpty());

		//dBUG:
		//[KK] 
		/*
			for (int i = 0; i < formulaeNL.getLength(); i++) {
				System.out.println("[INFO] Detected Formula #"+ i + " >--> " + formulaeNL.item(i).getTextContent());
			}

			for (String input : requiredFormulaInputs) {
				System.out.println("[WARNING] Expecting input.. " + input);
			}
			//*/		
	}

	/**
	 * Substitutes references to outports in existing formula itself
	 * @param selfies
	 * @param formulaeNL
	 * @deprecated
	 */
	private void substituteSelfies(HashMap<String, String> selfies, NodeList formulaeNL) {
		//		System.out.println("[FUNC] substituteSelfies");
		for (Iterator<String> substIter = selfies.keySet().iterator(); substIter.hasNext();) {
			String token = (String) substIter.next();
			for (int i=0; i < formulaeNL.getLength(); i++) {
				Element formulaElem = (Element) formulaeNL.item(i);
				if (formulaElem.getNodeType() == Element.ELEMENT_NODE) {
					String replacement = formulaElem.getTextContent().replaceAll(token, selfies.get(token));
					//dBug:: System.out.println("\tBefore:: " + formulaElem.getTextContent());
					//dBug:: System.out.println("\tAfter:: " + replacement);
					formulaElem.setTextContent(replacement);

					//Removing from list of requiredFormulaInputs
					requiredFormulaInputs.remove(token);
				}
			}
		}	
	}

	/**
	 * Identifies all references of required inputs that are outputs themselves
	 * @param formulaeNL
	 * @return
	 * @throws XPathExpressionException
	 * @Deprecated
	 */
	private HashMap<String, String> getListOfSelfies(NodeList formulaeNL) throws XPathExpressionException {
		//		System.out.println("[FUNC] getListOfSelfies");
		HashMap<String, String> selfies = new HashMap<String, String>();
		for (int i=0; i < formulaeNL.getLength(); i++) {
			Element formulaElem = (Element) formulaeNL.item(i);
			if (formulaElem.getNodeType() == Element.ELEMENT_NODE) {
				//dBug:System.out.println(formulaElem.getTextContent());
				//dBug:System.out.println(metric.getName() + " : " + metric.getDescription() + " ("+metric.getUnit()+")");

				String tokens[] = extractTokens(formulaElem.getTextContent());

				//KUNAL: Check if tokens are formulae themselves. 
				// If So... make change in the document itself!

				for (String token : tokens) {
					requiredFormulaInputs.add(token); 
					XPathExpression expr = xpath.compile("/translate/metric[@id='"+token+"']/formula");
					NodeList toSubstituteNL = (NodeList) expr.evaluate(lookupDoc,XPathConstants.NODESET);
					if (toSubstituteNL.getLength() > 0) {
						System.out.println("WARNING: Self-Reference exists! "+  token);
						for (int j=0; j < toSubstituteNL.getLength(); j++) {
							Element substitutionElem = (Element) toSubstituteNL.item(j);
							if (substitutionElem.getNodeType() == Element.ELEMENT_NODE) {
								String substitute = substitutionElem.getTextContent();
								selfies.put(token, substitute);
								//								System.out.println("To replace \""+token+"\" with \""+  substitute + "\"");
								break;
							}
						}		
					}
				}
			}   			  
		}

		return selfies;
	}

}
