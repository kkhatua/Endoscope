package com.khatua.kunal.guts;
/**                                                                                                                                                                                
 * Copyright (c) 2013 MapR Technologies. All rights reserved.                                                                                                                             
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import com.khatua.kunal.endoscope.EndoscopeConfKeyword;
import com.khatua.kunal.endoscope.EndoscopeDefault;
import com.khatua.kunal.endoscope.EndoscopeProperties;

/**
 * Parser for Guts input
 * @author kkhatua
 */
public class GutsParser {
	//Specifies Date & ClockTime Positions
	private static int DATE_IDX = 0, CLOCK_IDX = 1;
	//Metric Offset: If Date+Time => CCOff=2, else 1
	private static int METRIC_COL_OFFSET;
	//Specifies Date Format
	private SimpleDateFormat dateFormat;
	
	//Report Date Error once (assume each entry is 1sec interval and proceed)
	static private boolean seenDateFormatError;
	
	//Marker for start time in Unix Epochs
	private long startUnixTime = Long.MAX_VALUE;	
	
	//Specifies Numeric output Format
	private DecimalFormat deciformat;
	
	//Holds Metrics
	private LinkedHashMap<String, GutMetric> metricMap;
	
	//Number of Cores detected
	private int numOfCores;

	//FIXME
	private boolean reportBusyCore;
	private boolean reportEachCore;
	private boolean reportCumulativeCore;
	private boolean reportAverageCore;
	private boolean showUnixTime;
	private boolean showEpochTime;
	private boolean showElapsedTime;
	private boolean showElapsedClock;

	/**
	 * Constructor
	 */
	public GutsParser() {
		//TODO: Decide how to change Output in GutsTransformer!
		//i.e. XFormer OP decides WHAT Parser will feed .. for CPU and Time
		//Report Busy Core
		reportBusyCore = EndoscopeProperties.getBooleanProperty(EndoscopeConfKeyword.GUTS_CPU_BUSY,EndoscopeDefault.GUTS_CPU_BUSY);
		reportCumulativeCore = EndoscopeProperties.getBooleanProperty(EndoscopeConfKeyword.GUTS_CPU_SUM, EndoscopeDefault.GUTS_CPU_SUM);
		reportAverageCore = EndoscopeProperties.getBooleanProperty(EndoscopeConfKeyword.GUTS_CPU_AVERAGE, EndoscopeDefault.GUTS_CPU_AVERAGE);
		if (reportAverageCore)	// Update Average
			reportCumulativeCore = true;
		
		//Defining Time
		initTimeFormat();

		deciformat = new DecimalFormat(EndoscopeProperties.getProperty(EndoscopeConfKeyword.NUMERIC_VALUE_FORMAT, EndoscopeDefault.NUMERIC_VALUE_FORMAT));
	}

	/**
	 * For Use by External Apps
	 * TODO: Make this protected/private/package?
	 * @return
	 */
	public LinkedHashMap<String, GutMetric> getMetricMap() {
		return metricMap;
	}

	/**
	 * Initialize input and output time format
	 */
	private void initTimeFormat() {
		//Input Format
		dateFormat = new SimpleDateFormat(EndoscopeProperties.getProperty(EndoscopeConfKeyword.GUTS_TIME_FORMAT, EndoscopeDefault.FULL_DATE_FORMAT));
		
		//Defining Offset from where CPU Columns start
		METRIC_COL_OFFSET = dateFormat.toPattern().split(" ").length;
		System.out.println("[dBug] METRIC_COL_OFFSET = "+METRIC_COL_OFFSET);

		//DATE_IDX & TIME_IDX
		if (METRIC_COL_OFFSET > 1) {
			DATE_IDX = 0;
			CLOCK_IDX = 1;
		} else {
			CLOCK_IDX = 0;
		}		

		showUnixTime = EndoscopeProperties.matches(EndoscopeConfKeyword.OUTPUT_TIME_FORMAT, EndoscopeDefault.GUTS_TIME_UNIX);
		showEpochTime = EndoscopeProperties.matches(EndoscopeConfKeyword.OUTPUT_TIME_FORMAT,EndoscopeDefault.GUTS_TIME_EPOCH);
		showElapsedTime = EndoscopeProperties.matches(EndoscopeConfKeyword.OUTPUT_TIME_FORMAT,EndoscopeDefault.GUTS_TIME_ELAPSED);
		showElapsedClock = EndoscopeProperties.matches(EndoscopeConfKeyword.OUTPUT_TIME_FORMAT,EndoscopeDefault.GUTS_CLOCK_ELAPSED);
	}

	/**
	 * @deprecated Why do we have this?
	 * Set output time format
	 * @param timeMetric
	 */
	void setOutputTimeFormat(String timeMetric) {
		showUnixTime = EndoscopeDefault.GUTS_TIME_UNIX.equalsIgnoreCase(timeMetric);
		showEpochTime = EndoscopeDefault.GUTS_TIME_EPOCH.equalsIgnoreCase(timeMetric);
		showElapsedTime = EndoscopeDefault.GUTS_TIME_ELAPSED.equalsIgnoreCase(timeMetric);
		showElapsedClock = EndoscopeDefault.GUTS_CLOCK_ELAPSED.equalsIgnoreCase(timeMetric);
	}

	/**
	 * Sample the file for headers
	 * @param gutsFileToSample
	 * @return
	 */
	public boolean sampleForHeader(File gutsFileToSample) {
		BufferedReader sampler = null;
		long rowsSampled = 0;

		try {
			sampler = new BufferedReader(new FileReader(gutsFileToSample));
			String lineRead = null;

			//Sample File
			while ( ( rowsSampled <  EndoscopeDefault.MAX_ROWS_TO_SAMPLE )		&&		
					( lineRead = sampler.readLine()) != null ) 
			{
				rowsSampled++;
				if (lineRead.trim().startsWith("time")) {
					//System.out.println(lineRead);
					this.metricMap = constructMetricMap(lineRead);
					rowsSampled = EndoscopeDefault.MAX_ROWS_TO_SAMPLE; //Fail the While Condition to exit
				}
			}

			//Closing sampler
			sampler.close();			
		} catch (Exception e) {
			e.printStackTrace();
		}			
		
		//if (this.headerMap !=  null)
		if (this.metricMap !=  null)
			return true;
		else
			System.err.println("Could not infer headers by sampling first "+EndoscopeDefault.MAX_ROWS_TO_SAMPLE+" rows");
		return false;
	}


	//Prints Usage Message
	//TODO
	private static void printUsage() {

	}

	/**
	 * Tokenize given input line
	 * @param input
	 * @return
	 */
	public String[] parseInput(String input) {
		//Skip for blank lines
		if (input.trim().length() < "time".length() 
			|| input.trim().startsWith("time")) return null;
		
		if (metricMap == null)  {
			System.err.println("[WARNING] Need to sample file first!");
			return null;
		}

		//Parse
		String values[] = input.trim().split("\\s+");
		LinkedList<String> valueList = new LinkedList<String>();
	
		//Collapse Time
		if (showUnixTime || showEpochTime || showElapsedTime || showElapsedClock) {
			long unixTime = 0;
			try {
				if (METRIC_COL_OFFSET > 1)
					unixTime = dateFormat.parse(values[DATE_IDX] + " " + values[CLOCK_IDX]).getTime();
				else
					unixTime = dateFormat.parse(values[CLOCK_IDX]).getTime();
			} catch (ParseException e) {
				if (!seenDateFormatError) {
					e.printStackTrace();
					System.err.println("[WARNING] Will not report date errors any more");
					seenDateFormatError = true;
				}
				return null; //FIXME Wrong TS means what?
			}

			if (showUnixTime) { 
				//Setting Date value as UnixTime
				valueList.add(String.valueOf(unixTime));
			} else
				if (showEpochTime) { 
					//Setting Date value as UnixTime
					valueList.add(String.valueOf(unixTime/1000)); //Setting Epoch
				} else {	//Assuming Elapsed Time
					if (unixTime < startUnixTime  )  //Indicates 1st time ever
						startUnixTime = unixTime;

					//Setting Date value as ElapsedTime
					if (showElapsedTime) 
						valueList.add(String.valueOf(
							(unixTime - startUnixTime) / 1000 //Converting to Seconds
							));

					if (showElapsedClock) {
						valueList.add(clockTime((unixTime - startUnixTime)/1000));
					}
				}
		}

		//TODO: Method of its own?
		// FIRST Collapse Cores 
//		if (reportBusyCore) { //Always Busy is shown!		
		if (reportCumulativeCore || reportAverageCore || reportEachCore) {		
			//Insert ALL CPU values to Linkedist
			for (int coreId = 0; coreId < numOfCores; coreId++) {
				//values[coreId+coreColumnOffset] = String.valueOf(100-Integer.valueOf(values[coreId+coreColumnOffset]));
				valueList.add( 
						String.valueOf(
								100 - Integer.valueOf(values[coreId+METRIC_COL_OFFSET])	//Compute
								)
						);
			}

			//Iterate and sum up 
			if (reportCumulativeCore || reportAverageCore) {
				float cumulative = 0; 
				for (int coreId = 0; coreId < numOfCores; coreId++) {
					//Remove and Accumulate
					cumulative += Integer.valueOf(valueList.removeLast());
				}
				//Injecting final Value
				if (reportAverageCore) 
					valueList.add(String.valueOf(deciformat.format(cumulative/numOfCores)));
				else 
					valueList.add(String.valueOf(cumulative));
			}
		}			
		
		//Iterate and add remaining columns
		//FIXME: TIME is skipped!! for (int i = METRIC_COL_OFFSET+numOfCores; i < values.length; i++) {
		for (int i = 0; i < values.length; i++) {
			valueList.add(values[i]);
		}

		//Returns Values 
		return valueList.toArray(new String[valueList.size()]);
	}

	/**
	 * Translates Elapsed Time to pretty clock time
	 * @param elapsedInSec
	 * @return
	 */
	public String clockTime(long elapsedInSec) {
		int hours = (int) elapsedInSec / 3600;
		int remainder = (int) elapsedInSec - hours * 3600;
		int mins = remainder / 60;
		remainder = remainder - mins * 60;
		int secs = remainder;
		String clockTime = (hours<10? "0" : "")+ hours+(mins<10? ":0" : ":") + mins + (secs<10? ":0" : ":") + secs;
		return clockTime;
	}


	/**
	 * Constructs a LinkedHashMap of column and metric
	 * @param input
	 * @return
	 */
	public/*private*/ LinkedHashMap<String, GutMetric> constructMetricMap(String input) {
		//Extracting Headers
		String headerList[] = input.trim().split("\\s+");
		LinkedHashMap<String, GutMetric> inputMetricsMap = new LinkedHashMap<String, GutMetric>(headerList.length*3); //Scaling by 3 for optimal size
		
		//Loop through header
		for (String columnLabel : headerList) {
			try {				
				//Get List of Metrics represented by a column Label
				LinkedList<GutMetric> metricList = GutsSchemaFactory.getMetric(columnLabel);
				//Add metrics to header
				for (Iterator<GutMetric> iterator = metricList.iterator(); iterator.hasNext();) {
					GutMetric gutMetric = iterator.next();
					inputMetricsMap.put(gutMetric.getName(), gutMetric);
					//dBug:: System.out.println("cOL="+gutMetric.getName());
				}
			} catch (Exception e) {
				//e.printStackTrace(); //NoSuchFieldException
				//dBug:: System.err.println("[WARNING] Please define unknown column: " + e.getMessage());
				//Creating unknown Metric
				GutMetric unknownMetric = GutMetric.genUnknownMetric(columnLabel);
				boolean isCPURelated = accountForCPUCore(columnLabel, unknownMetric);
				if (isCPURelated)	columnLabel="cpu"+columnLabel; //

//				if (isCPURelated && !(reportCumulativeCore || reportAverageCore || reportEachCore)) 
//					continue; //i.e. Skip if no CPU info is needed
//				else 
					//Note: For CPUSum/Avg, this will re-update the map automatically
					inputMetricsMap.put(columnLabel, unknownMetric);

				//dBug:: System.out.println("col="+columnLabel);
				
				//*/
			}
		}

		//Returning reference
		return inputMetricsMap;
	}

	/**
	 * Accounting for cpu00 to cpu23... to cpu##
	 * @param columnLabel
	 * @param unknownMetric
	 * @return
	 */
	public/*private*/ boolean accountForCPUCore(String columnLabel, GutMetric unknownMetric) {
		String numberRegex= "[0-9]+";
		if(columnLabel.matches(numberRegex)) {
			//Setting GutMetric
			String cpuID = columnLabel;
			columnLabel = "cpu"+columnLabel;
			unknownMetric.setName(columnLabel);
			unknownMetric.setDescription("Idle-ness on Core #"+cpuID + " (NOTE: Inverted Values are produced)");
			unknownMetric.setUnit("%");
			
			//Tracking total #cores seen so far
			numOfCores++;

			//Checking for Cumulative
			if (reportBusyCore)
				if (reportCumulativeCore || reportAverageCore) {
					if (numOfCores > 0) { //i.e. 1st entry
						if (reportCumulativeCore) {//
							columnLabel = "cpuSum";
							//dBug:: System.out.println("header = cpuSum");
							unknownMetric.setDescription("Sum of all "+numOfCores+" cores");
						}
						if (reportAverageCore) {//
							columnLabel = "cpuAvg";
							//dBug:: System.out.println("header = cpuAvg");
							unknownMetric.setDescription("Overall CPU Usage of "+numOfCores+" cores");
						}
					}
				}
			return true;
		}
		return false;
	}
}
