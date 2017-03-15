package com.khatua.kunal.guts;

/**
 * Defines the generic properties for a Guts metric
 */
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.khatua.kunal.endoscope.EndoscopeDefault;

@XmlRootElement
public class GutMetric {
	String name;
	String description;
	String unit;

	public GutMetric() {

	}

	public String getName() {
		return name;
	}

	@XmlElement
	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	@XmlAttribute
	public void setDescription(String description) {
		this.description = description;
	}

	public String getUnit() {
		return unit;
	}

	@XmlElement
	public void setUnit(String unit) {
		this.unit = unit;
	}

	public static GutMetric genUnknownMetric(String metricName) {
		GutMetric unknownMetric = new GutMetric();
		unknownMetric.setName(metricName);
		unknownMetric.setDescription("Unknown");
		unknownMetric.setUnit("UNK");
		return unknownMetric;
	}

	/**
	 * Creates a TimeMetric
	 * @param metricName
	 * @return
	 */
	public static GutMetric createTimeMetric(String metricName) {
		GutMetric timeMetric = GutMetric.genUnknownMetric(metricName);
		System.out.println("###"+timeMetric.getName());

		if (metricName.equalsIgnoreCase(EndoscopeDefault.XFORM_XML_UNIX_TIME)) {
			timeMetric.setDescription("Time in Unix Epoch");
			timeMetric.setUnit("ms");
		}
		if (metricName.equalsIgnoreCase(EndoscopeDefault.XFORM_XML_EPOCH_TIME)) {
			timeMetric.setDescription("Time in Epoch");
			timeMetric.setUnit("sec");
		}
		if (metricName.equalsIgnoreCase(EndoscopeDefault.XFORM_XML_ELAPSED_TIME)) {
			timeMetric.setDescription("Elapsed Time");
			timeMetric.setUnit("sec");
		}
		if (metricName.equalsIgnoreCase(EndoscopeDefault.XFORM_XML_ELAPSED_CLOCK)) {
			timeMetric.setDescription("Elapsed Clock Time");
			timeMetric.setUnit("hh:mm:ss");
		}

		return timeMetric;
	}

	/**
	 * Test if metric is a time metric
	 * @param metricName
	 * @return
	 */
	public static boolean isTimeMetric(String metricName) {
		if (	metricName.equalsIgnoreCase(EndoscopeDefault.XFORM_XML_UNIX_TIME) ||
				metricName.equalsIgnoreCase(EndoscopeDefault.XFORM_XML_EPOCH_TIME) ||
				metricName.equalsIgnoreCase(EndoscopeDefault.XFORM_XML_ELAPSED_TIME) ||
				metricName.equalsIgnoreCase(EndoscopeDefault.XFORM_XML_ELAPSED_CLOCK) ||
				metricName.equalsIgnoreCase(EndoscopeDefault.XFORM_XML_DATE) ||
				metricName.equalsIgnoreCase(EndoscopeDefault.XFORM_XML_WALL_CLOCK)
				) 
			return true;
		else
			return false;
	}

	/**
	 * Test if a metric is a CPU metric
	 * @param metricName
	 * @return
	 */
	public static boolean isCPUMetric(String metricName) {
		if (	metricName.matches(EndoscopeDefault.XFORM_XML_CPU+"[0-9]+") ||
				metricName.matches(EndoscopeDefault.XFORM_XML_CPU+"Avg") ||
				metricName.matches(EndoscopeDefault.XFORM_XML_CPU+"Sum")								 
				) 
			return true;
		else
			return false;
	}
}