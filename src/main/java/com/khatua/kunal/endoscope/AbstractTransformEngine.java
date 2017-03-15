/**
 * 
 */
package com.khatua.kunal.endoscope;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Abstract Transformation Engine Class
 * @author kkhatua
 */
public abstract class AbstractTransformEngine {
	static DecimalFormat format = new DecimalFormat(EndoscopeDefault.NUMERIC_VALUE_FORMAT);
	protected static ScriptEngineManager jsEngineMgr;
	protected static ScriptEngine jsEngine;
	protected static HashMap<String, Integer> inputIndexLookup;

	/**
	 * Identifies tokens within an equation
	 * @param equation
	 * @return
	 */
	public static String[] extractTokens(String equation) {
		//1. Normalize by embedding spaces
		String normalizedEquation = equation
				.replaceAll( "\\(", " \\( ").replaceAll("\\)", " \\) ")
				.replaceAll( "\\*", " ").replaceAll("/", " / ")
				.replaceAll( "\\+", " + ").replaceAll("-", " - ")
				.replaceAll( "\\^", " \\^ ")
				.replaceAll(",", " , ")
				.replaceAll("\\s+", " ") //Reducing repeated whitespace to single
				.trim();

		//2. Tokenize And filter
		LinkedList<String> tokenList = new LinkedList<String>();
		String[] initTokens = normalizedEquation.split("\\s");
		for (String token : initTokens) 
			if (isToken(token)) {
				tokenList.add(token);
				//System.out.println("[OK] "+token);
			}

		return tokenList.toArray(new String[tokenList.size()]);		
	}

	/**
	 * Specifies if an input is a token 
	 * @param token
	 * @return
	 */
	public static boolean isToken(String token) {
		if (
				token.contains("Math.") ||
				token.matches( "\\(" ) ||
				token.matches( "\\)" ) ||
				token.matches( "\\*" ) ||
				token.matches( "/" )	||
				token.matches( "\\+" ) ||
				token.matches( "-" ) ||
				token.matches( "\\^" ) ||
				token.matches( "," ) ||
				token.matches("[0-9]+[.]*[0-9]*") 
				)
			return false;
		return true;
	}

	/**
	 * Evaluates formula based on inputs
	 * TODO: Consider using MessageFormat
	 * @param input
	 * @param formula
	 * @return
	 */
	public static String evaluate(String[] input, String formula, String defaultValue) {
		String evaluation;
		//1. Tokenize
		for (String token : extractTokens(formula)) {
			//2. Lookup input for each token
			//System.out.println(token);
			//FIXME: if GutMetric.isTimeMetric(token)
			String inputValue = null;
			try {
				inputValue = input[inputIndexLookup.get(token)];
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println(token);
				System.err.println("inputIndexLookup.get("+token+")");
				System.err.println("input["+inputIndexLookup.get(token)+"]");
			} 
			//3. SAR token with input			
			formula = formula.replaceAll(token, inputValue);
		}		
		//4. Evaluate
		Double calculation = null;
		try {
			calculation = (Double) jsEngine.eval(formula);
			//jsEngine.put(key, value);
		} catch (ScriptException e) {
			/*e.printStackTrace();*/
			//REF: http://javalandscape.blogspot.com/2008/12/scripting-in-jdk6-jsr-223-part-1.html
		}
		//dBug: System.out.println("Evaluated...\" " + formula + " \" to get " + calculation);
		//5. Return evaluation
		if (calculation == null) {
			evaluation = formula;
		}
		else {
			//evaluation = String.valueOf(calculation);
			if (!calculation.isNaN()) //format of NaN yields ?
				evaluation = String.valueOf(format.format(calculation));
			else {
				evaluation = defaultValue;
			}
		}
//		if (formula.equals("clock"))
//			System.out.println(formula+">>>>>"+evaluation);
		
		return evaluation;
	}


}
