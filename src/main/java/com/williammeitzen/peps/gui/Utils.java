package com.williammeitzen.peps.gui;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.ResultSet;
//import java.sql.ResultSetMetaData;
//import java.sql.SQLException;
//import java.sql.Statement;
import java.text.NumberFormat;
//import org.sqlite.SQLiteConfig;
//import org.sqlite.SQLiteOpenMode;

import javax.swing.ImageIcon;

//import org.json.simple.DeserializationException;
//import org.json.simple.JsonArray;
//import org.json.simple.JsonObject;
//import org.json.simple.Jsoner;

public class Utils {
	
	public enum LineThicknesses {
		ACTUAL_DATA_MASS
		,ACTUAL_ROWS
		,ACTUAL_VS_ESTIMATED_ROWS_RATIO
		,ACTUAL_VS_ESTIMATED_MASS_RATIO
		,ESTIMATED_DATA_MASS
		,ESTIMATED_ROWS
	}

	public enum PercentageBy {
		COST
		//,DISK_PLUS_RAM
		//,DISK
		//,RAM
		,EXCLUSIVE_TIME
		,TIME_DIFFERENCE
	}
	
	public static String nullDoubleToEmptyString(Double dblValue) { 
		if (dblValue == null) {
			return "";
		} else {
			return Double.toString(dblValue);
		}
	}

	public static double nullDoubleToZero(Double dblValue) {
		if (dblValue == null) {
			return 0.0d;
		} else {
			return dblValue;
		}
	}

	public static double nullDoubleToZero(Object objValue) {
		Double parsed = nullObjectToNullDouble(objValue);
		return parsed == null ? 0.0d : parsed.doubleValue();
	}

	public static Double nullObjectToNullDouble(Object objValue) {
		if (objValue == null) {
			return null;
		}
		if (objValue instanceof Number) {
			double d = ((Number) objValue).doubleValue();
			return Double.isFinite(d) ? d : null;
		}
		String text = String.valueOf(objValue).trim();
		if (text.isEmpty() || text.equalsIgnoreCase("null")) {
			return null;
		}
		try {
			double d = Double.parseDouble(text);
			return Double.isFinite(d) ? d : null;
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	public static boolean objectToBoolean(Object objValue) {
		if (objValue == null) {
			return false;
		} else {
			return (boolean) (objValue.toString().equals("1"));
		}
	}

	public static String getFileExtension(String name) {
		int pointIndex = name.lastIndexOf(".");
		
		if (pointIndex == -1) {
			return null;
		}
		
		if (pointIndex == name.length() - 1) {
			return null;
		}
		
		return name.substring(pointIndex + 1, name.length());
	}
	
	public static String blocks_to_bytes(Double blocks) {
		if (blocks == null) {
			return null;
		}
		return bytes(blocks * 8192);
	}
	
	public static String blocks_to_bytes(Double blocks, int precision) {
		if (blocks == null) {
			return null;
		}
		return bytesWithSpace(blocks * 8192, precision);
	}
	

	/** Display ceiling for abbreviated magnitudes (rows, cost, bytes, …). */
	public static final double DISPLAY_COUNT_CAP = 999_000_000_000_000d; // 999 trillion

	private static boolean exceedsDisplayCap(double value) {
		return !Double.isFinite(value) || Math.abs(value) > DISPLAY_COUNT_CAP;
	}

	public static String bytes(double bytes) {
		if (exceedsDisplayCap(bytes)) {
			return ">999 TB";
		}
		int digits = 0;
		String[] dictionary = { " bytes", "kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB" };
        int index = 0;
        for (index = 0; index < dictionary.length; index++) {
            if (bytes < 1000) {
                break;
            }
            bytes = bytes / 1024;
            //bytes = bytes / 1000; // this seems weird
        }
        if (index >= dictionary.length) {
        	return ">999 TB";
        }
        if (index > 0 && bytes <= 9) {
        	digits = 1;
        }
        return String.format("%." + digits + "f", bytes) + "" + dictionary[index];
	}

	public static String kilobytes(double kilobytes) {
		if (exceedsDisplayCap(kilobytes)) {
			return ">999 TB";
		}
		int digits = 0;
		String[] dictionary = { "kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB" };
        int index = 0;
        for (index = 0; index < dictionary.length; index++) {
            if (kilobytes < 1000) {
                break;
            }
            kilobytes = kilobytes / 1000; // this seems weird
        }
        if (index >= dictionary.length) {
        	return ">999 TB";
        }
        if (index > 0 && kilobytes <= 9) {
        	digits = 1;
        }
        return String.format("%." + digits + "f", kilobytes) + "" + dictionary[index];
	}

	public static String bytesWithSpace(double bytes, int precision) {
		if (exceedsDisplayCap(bytes)) {
			return ">999 TB";
		}
		String[] dictionary = { " bytes", "kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB" };
        int index = 0;
        for (index = 0; index < dictionary.length; index++) {
            if (bytes < 1000) {
                break;
            }
            bytes = bytes / 1024;
            //bytes = bytes / 1000; // this seems weird
        }
        if (index >= dictionary.length) {
        	return ">999 TB";
        }
        return String.format("%." + precision + "f", bytes) + " " + dictionary[index];
	}

	public static String formatPGVersionNumber(String versionNumber) {
		String strPGVersionNumber;
		int intPGVersionInteger = 0;
		int intPGVersionMajorMantissa = 0;
		int intPGVersionMinorMantissa = 0;
		// if it's a two-dot version, e.g., 9.6.5, make it 9.0605 / 9.15.13 -> 9.1513
		// if it's a one-dot version, e.g., 10.1, make it 10.01 / 10.16 stays the same
		if (versionNumber.matches("\\d+\\.\\d+\\.\\d+") == true) {
			intPGVersionInteger = Integer.parseInt(versionNumber.replaceAll("(\\d+)\\..*", "$1"));
			intPGVersionMajorMantissa = Integer.parseInt(versionNumber.replaceAll("\\d+\\.(\\d+).*", "$1"));
			intPGVersionMinorMantissa = Integer.parseInt(versionNumber.replaceAll("\\d+\\.\\d+\\.(.*)", "$1"));
		} else if (versionNumber.matches("\\d+\\.\\d+") == true) {
			intPGVersionInteger = Integer.parseInt(versionNumber.replaceAll("(\\d+)\\..+", "$1"));
			intPGVersionMajorMantissa = Integer.parseInt(versionNumber.replaceAll("\\d+\\.(\\d+).*", "$1"));
		} else {
			intPGVersionInteger = Integer.parseInt(versionNumber);
		}
		strPGVersionNumber = Integer.toString(intPGVersionInteger) + ".";
		//+ Integer.toString(intPGVersionMajorMantissa);
		strPGVersionNumber += ("00" + Integer.toString(intPGVersionMajorMantissa)).substring(("00" + Integer.toString(intPGVersionMajorMantissa)).length()-2);
		strPGVersionNumber += ("00" + Integer.toString(intPGVersionMinorMantissa)).substring(("00" + Integer.toString(intPGVersionMinorMantissa)).length()-2);
		return strPGVersionNumber;
	}
	
	// need to fix this. 30998 rows returns 30k rows
	public static String units(double rows) {
		if (exceedsDisplayCap(rows)) {
			return ">999T rows";
		}
		int digits = 0;
		String[] dictionary = { " rows", "k rows", "M rows", "B rows", "Tr rows", "Quadr rows", "Quint rows", "Sext rows", "Oct rows", "Noni rows" };
		String strDictionary;
        int index = 0;
        for (index = 0; index < dictionary.length; index++) {
            if (rows < 1000) {
                break;
            }
            rows = rows / 1000;
        }
        if (index >= dictionary.length) {
        	return ">999T rows";
        }
        strDictionary = dictionary[index];
        if (index == 0 && rows == 1) {
        	strDictionary = " row";
        }
        if (index > 0 && rows < 9.9) {
        	digits = 1;
        }
        return String.format("%." + digits + "f", rows) + "" + strDictionary;
	}

	public static String formattedCost(double cost) {
		if (exceedsDisplayCap(cost)) {
			return ">999T";
		}
		int digits = 0;
		String[] dictionary = { "", "k", "M", "B", "Tr", "Quadr", "Quint", "Sext", "Oct", "Noni" };
		String strDictionary;
        int index = 0;
        for (index = 0; index < dictionary.length; index++) {
            if (cost < 1000) {
                break;
            }
            cost = cost / 1000;
        }
        if (index >= dictionary.length) {
        	return ">999T";
        }
        strDictionary = dictionary[index];
        if (index == 0 && cost == 1) {
        	strDictionary = " row";
        }
        if (index > 0 && cost < 9.9) {
        	digits = 1;
        }
        return String.format("%." + digits + "f", cost) + "" + strDictionary;
	}

	public static String roundValue(double value, int precision) {
	    NumberFormat nf = NumberFormat.getNumberInstance();
	    nf.setMaximumFractionDigits(precision);
	    String roundedValue = nf.format(value);
	    return (String) roundedValue;
	}
	
	public static double roundValueToDouble(double value) {
		double roundedValue = (double) Math.rint(value);
	    return roundedValue;
	}
	
	public static String roundValueRequirePrecision(double value, int precision) {
	    return String.format("%." + precision + "f", value);
	}
	
	public static String elapsed_time_ms(double milliseconds) {
		return elapsed_time_sec(milliseconds / 1000);
	}
	
	public static String elapsed_time_sec(double seconds) {
		if (!Double.isFinite(seconds) || Math.abs(seconds) > 999d * 24d * 60d * 60d) {
			return seconds < 0 ? ">-999 days" : ">999 days";
		}
		String strElapsedTime;
		double fltOriginalSeconds = seconds;
		double fltAbsSeconds = Math.abs(seconds);
		double fltAbsOriginalSeconds = Math.abs(seconds);
		double minutesInSeconds = 60;
		double hoursInSeconds = minutesInSeconds * 60;
		double daysInSeconds = hoursInSeconds * 24;

		double elapsedDays = fltAbsOriginalSeconds / daysInSeconds;
		fltAbsOriginalSeconds = fltAbsOriginalSeconds % daysInSeconds;
		
		double elapsedHours = fltAbsOriginalSeconds / hoursInSeconds;
		fltAbsOriginalSeconds = fltAbsOriginalSeconds % hoursInSeconds;
		
		double elapsedMinutes = fltAbsOriginalSeconds / minutesInSeconds;
		fltAbsOriginalSeconds = fltAbsOriginalSeconds % minutesInSeconds;
		
		double elapsedSeconds = fltAbsOriginalSeconds;
		
		if (fltAbsSeconds == 0) {
	    	strElapsedTime = "0 sec";
		} else if (fltAbsSeconds < 1 * Math.pow(10, -9)) {
		   	strElapsedTime = "<1 ns";
		} else if (fltAbsSeconds < 10 * Math.pow(10, -9)) {
	    	strElapsedTime = roundValue(fltAbsSeconds * Math.pow(10, +9), 1) + " ns";
		} else if (fltAbsSeconds <= 999.9 * Math.pow(10, -9)) { // can't compare to 1000 b/c of a weird rounding issue
	    	strElapsedTime = roundValue(fltAbsSeconds * Math.pow(10, +9), 0) + " ns";
	    } else if (fltAbsSeconds < 10 * Math.pow(10, -6)) {
	    	strElapsedTime = roundValue(fltAbsSeconds * Math.pow(10, +6), 1) + " \u03bcs";
	    } else if (fltAbsSeconds < 1000 * Math.pow(10, -6)) {
	    	strElapsedTime = roundValue(fltAbsSeconds * Math.pow(10, +6), 0) + " \u03bcs";
	    } else if (fltAbsSeconds < 10 * Math.pow(10, -3)) {
	    	strElapsedTime = roundValue(fltAbsSeconds * Math.pow(10, +3), 1) + " ms";
	    } else if (fltAbsSeconds < 1000 * Math.pow(10, -3)) {
	    	strElapsedTime = roundValue(fltAbsSeconds * Math.pow(10, +3), 0) + " ms";
	    } else if (fltAbsSeconds < 10) {
	    	strElapsedTime = roundValue(fltAbsSeconds, 1) + " sec";
	    } else if (fltAbsSeconds < 60) {
	    	strElapsedTime = roundValue(fltAbsSeconds, 0) + " sec";
	    } else if (fltAbsSeconds < 30 *60) {
	    	strElapsedTime = (int) elapsedMinutes + " min " + roundValue(elapsedSeconds, 0) + " sec";
	    } else if (fltAbsSeconds < 60 *60) {
	    	strElapsedTime = roundValue(elapsedMinutes, 0) + " min";
	    } else if (fltAbsSeconds < 24 *60*60) {
	    	strElapsedTime = (int) elapsedHours + " hr " + roundValue(elapsedMinutes, 0) + " min";
	    } else if (fltAbsSeconds < 30 *24*60*60) {
	    	strElapsedTime = (int) elapsedDays + " days " + (int) elapsedHours + " hr " + roundValue(elapsedMinutes, 0) + " min";
	    } else {
	    	strElapsedTime = (int) elapsedDays + " days " + roundValue(elapsedHours, 0) + " hr";
	    }

		if (fltOriginalSeconds < 0)
			strElapsedTime = "-" + strElapsedTime;
		
		return strElapsedTime;
	}
	
	public static String elapsed_time_ms_one_decimal(double milliseconds) {
		return elapsed_time_sec_one_decimal(milliseconds / 1000);
	}
	
	public static String elapsed_time_sec_one_decimal(double seconds) {
		if (!Double.isFinite(seconds) || Math.abs(seconds) > 999d * 24d * 60d * 60d) {
			return seconds < 0 ? ">-999 days" : ">999 days";
		}
		String strElapsedTime;
		double fltOriginalSeconds = seconds;
		double fltAbsSeconds = Math.abs(seconds);
		double fltAbsOriginalSeconds = Math.abs(seconds);
		double minutesInSeconds = 60;
		double hoursInSeconds = minutesInSeconds * 60;
		double daysInSeconds = hoursInSeconds * 24;

		double elapsedDays = fltAbsOriginalSeconds / daysInSeconds;
		fltAbsOriginalSeconds = fltAbsOriginalSeconds % daysInSeconds;
		
		double elapsedHours = fltAbsOriginalSeconds / hoursInSeconds;
		fltAbsOriginalSeconds = fltAbsOriginalSeconds % hoursInSeconds;
		
		double elapsedMinutes = fltAbsOriginalSeconds / minutesInSeconds;
		fltAbsOriginalSeconds = fltAbsOriginalSeconds % minutesInSeconds;
		
		double elapsedSeconds = fltAbsOriginalSeconds;
		
		if (fltAbsSeconds == 0) {
	    	strElapsedTime = "0 sec";
		} else if (fltAbsSeconds < 1 * Math.pow(10, -9)) {
		   	strElapsedTime = "<1 ns";
		} else if (fltAbsSeconds < 10 * Math.pow(10, -9)) {
	    	strElapsedTime = roundValue(fltAbsSeconds * Math.pow(10, +9), 1) + " ns";
		} else if (fltAbsSeconds <= 999.9 * Math.pow(10, -9)) { // can't compare to 1000 b/c of a weird rounding issue
	    	strElapsedTime = roundValue(fltAbsSeconds * Math.pow(10, +9), 0) + " ns";
	    } else if (fltAbsSeconds < 10 * Math.pow(10, -6)) {
	    	strElapsedTime = roundValue(fltAbsSeconds * Math.pow(10, +6), 1) + " \u03bcs";
	    } else if (fltAbsSeconds < 1000 * Math.pow(10, -6)) {
	    	strElapsedTime = roundValue(fltAbsSeconds * Math.pow(10, +6), 1) + " \u03bcs";
	    //} else if (fltAbsSeconds < 10 * Math.pow(10, -3)) {
	    //	strElapsedTime = roundValue(fltAbsSeconds * Math.pow(10, +3), 1) + " ms";
	    } else if (fltAbsSeconds < 1000 * Math.pow(10, -3)) {
	    	//strElapsedTime = roundValue(fltAbsSeconds * Math.pow(10, +3), 0) + " ms";
	    	strElapsedTime = roundValue(fltAbsSeconds * Math.pow(10, +3), 1) + " ms";
	    //} else if (fltAbsSeconds < 10) {
	    //	strElapsedTime = roundValue(fltAbsSeconds, 1) + " sec";
	    } else if (fltAbsSeconds < 60) {
	    	strElapsedTime = roundValue(fltAbsSeconds, 1) + " sec";
	    } else if (fltAbsSeconds < 30 *60) {
	    	strElapsedTime = (int) elapsedMinutes + " min " + roundValue(elapsedSeconds, 0) + " sec";
	    } else if (fltAbsSeconds < 60 *60) {
	    	strElapsedTime = roundValue(elapsedMinutes, 0) + " min";
	    } else if (fltAbsSeconds < 24 *60*60) {
	    	strElapsedTime = (int) elapsedHours + " hr " + roundValue(elapsedMinutes, 0) + " min";
	    } else if (fltAbsSeconds < 30 *24*60*60) {
	    	strElapsedTime = (int) elapsedDays + " days " + (int) elapsedHours + " hr " + roundValue(elapsedMinutes, 0) + " min";
	    } else {
	    	strElapsedTime = (int) elapsedDays + " days " + roundValue(elapsedHours, 0) + " hr";
	    }

		if (fltOriginalSeconds < 0)
				strElapsedTime = "-" + strElapsedTime;
		
		return strElapsedTime;
	}
	
	public static ImageIcon createIcon(String path) {

		URL url = System.class.getResource(path);
		
		if (url == null) {
			System.err.println("Unable to load image: " + path);
		}
		
		ImageIcon icon = new ImageIcon(url);
		
		return icon;
	}

	public static Font createFont(String path) {

		System.out.println("font path: " + path);
		//URL url = System.class.getResource(path);
		//URL url = System.class.getClassLoader().getResource(path);
		//URL url = Class.getClassLoader().getResource(path);
		//URL url = Class.class.getResource(path);
		
		//if (url == null) {
		//	System.err.println("Unable to load font: " + path);
		//}
		
		Font font = null;
		FileInputStream fisPath = null;
		try {
			fisPath = new FileInputStream(path);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		if (fisPath == null) {
			System.err.println("font path not found: " + path);
		}
		try {
			//font = Font.createFont(Font.TRUETYPE_FONT, url.openStream());
			//font = Font.createFont(Font.TRUETYPE_FONT, Font.class.getResourceAsStream(path));
			//font = Font.createFont(Font.TRUETYPE_FONT, Font.class.getClassLoader().getResourceAsStream(path));
			font = Font.createFont(Font.TRUETYPE_FONT, new FileInputStream(path));
		} catch (FontFormatException e) {
			System.err.println("Bad format in font file: " + path);
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Unable to read font file: " + path);
			e.printStackTrace();
		}
		
		return font;
	}

}
