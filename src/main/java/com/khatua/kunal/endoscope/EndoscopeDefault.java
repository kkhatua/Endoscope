package com.khatua.kunal.endoscope;

/**
 * Collection of default configuration values for Endoscope
 * @author kkhatua
 */
public class EndoscopeDefault {
	public static final String _warden = "Location of WardenConf [optional]";
	public static final String WARDEN_CONFIG_LOCATION = "/opt/mapr/conf/warden.conf";
	
	public static final String METRICS_XML = "metrics.xml";
	public static final String TRANSFORM_XML = "transform.xml";
	public static final String GUTS_CPU_BUSY = "true";
	public static final String GUTS_CPU_SUM = "sum";
	public static final String GUTS_CPU_AVERAGE = "average";
	public static final String GUTS_CPU_ALL = "all";
	public static final String GUTS_CPU_NONE = "none";
	public static final String NUMERIC_VALUE_FORMAT = "####.##";
	public static final String GUTS_TIME_UNIX = "unix";
	public static final String GUTS_TIME_EPOCH = "epoch";
	public static final String GUTS_TIME_ELAPSED = "elapsed";
	public static final String GUTS_CLOCK_ELAPSED = "timer";
	public static final String XFORM_XML_ELAPSED_TIME = "timer";
	public static final String XFORM_XML_ELAPSED_CLOCK = "elapsed";
	public static final String XFORM_XML_UNIX_TIME = "unix";
	public static final String XFORM_XML_EPOCH_TIME = "epoch";
	public static final String XFORM_XML_DATE = "date";
	public static final String XFORM_XML_WALL_CLOCK = "clock";
	
	public static final String OUTPUT_HEADER = "true";
	public static final String OUTPUT_DELIMITER = "\t";
	public static final String FULL_DATE_FORMAT = "yyyy-MM-dd hh:mm:ss";	
	public static final String PARTIAL_DATE_FORMAT = "hh:mm:ss";
	public static final String XFORM_XML_CPU = "cpu";
	public static final long MAX_ROWS_TO_SAMPLE = 20;
	public static final String INPUTS_PASS_ALL = "false";
	public static final String PUBLISH_EVERY_PERCENT = "5";
	public static final String PUBLISH_EVERY_SECOND = "60";
	public static final String DEFAULT_VALUE = "0";
}
