package com.khatua.kunal.endoscope;

/**
 * Static class to provide properties in required datatype
 * @author kkhatua
 */
public class EndoscopeProperties {

	/**
	 * Debug message
	 * @param param
	 * @param defaultValue
	 */
	private static void dBugTweet(String param, String defaultValue) {
		System.out.println("[prop] Looked up "+param+" = "+System.getProperty(param)+" ["+defaultValue+"]");
	}

	/**
	 * Get boolean value
	 * @param param
	 * @param defaultValue
	 * @return
	 */
	public static boolean getBooleanProperty(String param, String defaultValue) {
		dBugTweet(param, defaultValue);
		return Boolean.valueOf(System.getProperty(param, defaultValue));
	}

	/**
	 * Get Long value
	 * @param param
	 * @param defaultValue
	 * @return
	 */
	public static long getLongProperty(String param, String defaultValue) {
		dBugTweet(param, defaultValue);
		return Long.valueOf(System.getProperty(param, defaultValue));
	}

	/**
	 * Get Property value
	 * @param param
	 * @param defaultValue
	 * @return
	 */
	public static String getProperty(String param, String defaultValue) {
		dBugTweet(param, defaultValue);
		return System.getProperty(param, defaultValue);
	}

	/**
	 * Get Integer value
	 * @param param
	 * @param defaultValue
	 * @return
	 */
	public static int getIntegerProperty(String param, String defaultValue) {
		dBugTweet(param, defaultValue);
		return Integer.valueOf(System.getProperty(param, defaultValue));
	}

	/**
	 * Test for matchof property and expected value
	 * @param param
	 * @param matchValue
	 * @return
	 */
	public static boolean matches(String param, String matchValue) {
		System.out.println("[propMatch] Matching "+param+" : "+System.getProperty(param)+"  = "+matchValue+" ? " + matchValue.equalsIgnoreCase(System.getProperty(param)));
		return matchValue.equalsIgnoreCase(System.getProperty(param));
	}
}
