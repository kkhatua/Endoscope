package com.khatua.kunal.endoscope;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.DecimalFormat;

import com.khatua.kunal.guts.GutsParser;
import com.khatua.kunal.guts.GutsSchemaFactory;
import com.khatua.kunal.guts.GutsTransformer;

/**
 * 
 */

/**
 * MapR Endoscope Control Class
 * @author kkhatua
 */
public class Endoscope {
	static DecimalFormat format = new DecimalFormat("#,###.##");
	private boolean outputHeader;
	private GutsParser parser;
	private GutsTransformer xformer;
	private long recordCount;

	/**
	 * 
	 */
	public Endoscope() {
		outputHeader = Boolean.valueOf(System.getProperty(EndoscopeConfKeyword.OUTPUT_HEADER, EndoscopeDefault.OUTPUT_HEADER));
	}

	/**
	 * Print usage
	 */
	public static void printUsage() {
		System.err.println("[ERROR] Insufficient inputs");
		System.err.println("USAGE: "
				+ "\tjava [options] -jar Endoscope.jar <inputGutsFile> <outputFile>" + "\n"
				+ "\t---Endoscope Options---" + "\n"
				+ "\tguts.cpu.busy \t\t- Show busy percent (DEFAULT=true)" + "\n"
				+ "\tguts.cpu.sum \t\t- Show sum of ALL CPU cores (DEFAULT=false)" + "\n"
				+ "\tguts.cpu.average \t- Show Average of all CPU cores (DEFAULT=false)" + "\n"
				+ "\tguts.time.format \t- Display timestamp in specific format (DEFAULT=true; Options)" + "\n"
				+ "\t\t unix : Time in msec since epoch" + "\n"
				+ "\t\t elapsed : Elapsed time in seconds since start of guts capture" + "\n"
				+ "\t\t timer : Elapsed time in hh:mm:ss since start of guts capture" + "\n"
				+ "\theader.toutput \t- Output the header (DEFAULT: true)" + "\n"
				+ "\tnumeric.value.format \t- Java numeric format (DEFAULT: Number with 2 decimal places)" + "\n"
				+ "--- Set these as -D<option>=<value> ---");	
	}

	/**
	 * Load configs & Initialize Engines 
	 * @param gutsSrcFile
	 * @throws Exception
	 */
	private void loadConfig(File gutsSrcFile) throws Exception {
		//Loads the Schema to correctly identify inputs
		GutsSchemaFactory.setSchemaSource(EndoscopeProperties.getProperty(EndoscopeConfKeyword.METRICS_XML, EndoscopeDefault.METRICS_XML));

		//Initialize Parser 
		parser = new GutsParser();
		//Sampling Header
		boolean successfulSampling = parser.sampleForHeader(gutsSrcFile);
		if (!successfulSampling)
			throw new Exception("Unable to interpret file contents. Aborting!");

		//Estimate Record Count
		recordCount = countRecords(gutsSrcFile);
		System.out.println("[INFO] Will read "+recordCount+" records from " + gutsSrcFile.getAbsolutePath());

		//Initialize Transformer 
		xformer = new GutsTransformer(EndoscopeProperties.getProperty(EndoscopeConfKeyword.TRANSFORM_XML, EndoscopeDefault.TRANSFORM_XML));
		xformer.setInputs(parser);		
	}

	/**
	 * Counts approximate number of records 
	 * @param gutsSrcFile
	 * @return
	 */
	private long countRecords(File gutsSrcFile) {
		long linesCounted = 0L;
		try {
			LineNumberReader lineReader = new LineNumberReader(new FileReader(gutsSrcFile));
			lineReader.skip(Long.MAX_VALUE);
			linesCounted = lineReader.getLineNumber();
			lineReader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return linesCounted;		
	}

	/** 
	 * Generate Header
	 * @param outputs
	 * @return
	 */
	private String generateHeader(String[] outputs) {
		if (!outputHeader) return null; 
		//Construct output header
		StringBuilder header = new StringBuilder();
		/* OLD:
			String outputs[] = xformer.getOutputHeaders();
			//dBug: parser.getMetricMap().keySet().toArray(new String[parser.getMetricMap().size()]);
		 */
		//NEW:
		for (String output : outputs) {
			boolean metricExists = true; //TODO: Check if output exists (via Metrics & via XForm)!
			if (metricExists) {
				header.append(output).append('\t');
				//					System.out.println(header);
			}
			else 
				System.err.println("Unknown Metric requested: "+ output);
		}
		return header.toString();
	}

	/**
	 * Process file to generate output
	 * @param gutsSrcFile
	 * @param dataFileToPlot
	 */
	private void processFile(File gutsSrcFile, File dataFileToPlot) {
		long duration, startTime = 0;
		long rowCount=0;

		BufferedReader reader = null;
		BufferedWriter writer = null;
		//Estimate publishing interval 
		int publishEveryXPercent = EndoscopeProperties.getIntegerProperty(EndoscopeConfKeyword.PUBLISH_EVERY_PERCENT, EndoscopeDefault.PUBLISH_EVERY_PERCENT);
		long publishEveryXNanoSecond = EndoscopeProperties.getLongProperty(EndoscopeConfKeyword.PUBLISH_EVERY_SECOND, EndoscopeDefault.PUBLISH_EVERY_SECOND)*1000000000;
		long fivePercentIncr=recordCount/(100/publishEveryXPercent);
		if (fivePercentIncr == 0) {
			fivePercentIncr = 1;
		}

		String inputLine = null;
		String delimiter = EndoscopeProperties.getProperty(EndoscopeConfKeyword.OUTPUT_DELIMITER, EndoscopeDefault.OUTPUT_DELIMITER);

		startTime = System.currentTimeMillis();
		try {
			reader = new BufferedReader(new FileReader(gutsSrcFile));
			writer = new BufferedWriter(new FileWriter(dataFileToPlot));

			//Writing Header:
			if (outputHeader) {
				//Define output header
				String header = generateHeader(xformer.getOutputHeaders()/*outputFields*/);
				//System.out.println("Header:: " + header);
				writer.append(header.toString());
				writer.newLine();
				writer.flush();
			}

			//Marker for Elapsed Time 
			long lastReportedAtEpoch = System.nanoTime()/1000000000;
			//Scan File
			while 	( //rowCount < 5 && //Use this to limit  
					( inputLine = reader.readLine()) != null) {
				//Grab Data
				String [] outputs = 
						xformer.transformInput(
								parser.parseInput(inputLine)
								);

				//Publish Status
				if ( 	(rowCount++%fivePercentIncr == 0) 
						|| ((System.nanoTime()-lastReportedAtEpoch) > publishEveryXNanoSecond) ) { 
					System.out.println("[INFO] Finished processing "+(rowCount*100/recordCount)+"% of the input");
					lastReportedAtEpoch = System.nanoTime();
				}

				//Check if output is valid
				if (outputs == null)  continue;

				//Write valid output
				for (int i = 0; i < outputs.length; i++) {
					writer.append(outputs[i]).append(delimiter);
				}
				writer.newLine(); writer.flush();

			}	
			System.out.println("[INFO] Finished processing "+(rowCount*100/recordCount)+"% of the input");

			writer.close();
			reader.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(127);
		}

		duration=System.currentTimeMillis()-startTime;
		System.out.println("[INFO] Processed "+rowCount+ " rows in "+(int)(duration/1000)+" sec ("+rowCount*1000/duration+" rows/sec)");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//Advanced Arg Parser to be implemented
		if (args.length < 1/*2*/) {
			printUsage();

			//Exit
			System.exit(127);
		}

		//Identifies input and output files
		File inputFile = new File(args[0]);
		File outputFile = new File((args.length > 1) ? args[1] : "output.txt");

		if (!inputFile.canRead()) {
			System.out.println("ERROR: Please provide valid input file");
			System.exit(127);
		}
		try {
			if (!outputFile.createNewFile())
				System.out.println("WARNING: Will be overwriting "+ outputFile.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		//Create Endoscope Instance
		Endoscope endoscope = new Endoscope();
		//Load configuration
		endoscope.loadConfig(inputFile);
		//Process File
		endoscope.processFile(inputFile, outputFile);
	}
}
