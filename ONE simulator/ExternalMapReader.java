package input;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import core.Coord;
import core.SettingsError;

import java.util.Comparator;
import java.lang.NumberFormatException;

/**
 * Class for reading the external movement file in the format of comma separated columns.
 * There must be at least four columns: id, time, x, y (0-3). 
 * The default order of the columns is as stated above, but it can be changed through settings (idCol, timeCol, xCol, yCol).
 * The time can be given in date format or in seconds. Current date format is MM/dd/yy HH:mm.
 * Time zero in the simulation is the earliest time in the file, unless defined otherwise in the settings file (startPoint).
 */
public class ExternalMapReader {
	/** Prefix for comment lines (lines starting with this are ignored) */
	public static final String COMMENT_PREFIX = "#";
	/** Date format */
	public static final DateFormat df = new SimpleDateFormat("MM/dd/yy HH:mm");
	/** List containing lists for stops. A list for each node */
	List<List<Coord>> locations = new ArrayList<List<Coord>>();
	/** List containing lists for time space between stops. A list for each node */
	List<List<Double>> difTimes = new ArrayList<List<Double>>();
	/** List containing the nodes' IDs */
	List<String> nodesIDs = new ArrayList<String>();
	
	/** New column positions for data in the list: id, time, x, y (0-3) */
	private int NidCol;
	private int NtimeCol;
	private int NxCol;
	private int NyCol;

	public ExternalMapReader (String fileName, int timeCol, int idCol, int xCol, int yCol, String timeFormat, String startingPoint, int emmMode) {
		
		/** columns for id, time, x, y */
		int[] cols = {idCol, timeCol, xCol, yCol};
		/** input file */
		File extMapFile = null;
		/** a line in the file */
		String dataRow = null;
		/** List of lists for all the data */
		List<List<String>> nData = new ArrayList<List<String>>();
		/** earliest time in the input file */
		String firstTime = null;

		/** all columns must be different */
		for (int i=0; i<cols.length-1; i++) {
			for (int j=i+1; j<cols.length; j++) {
				if (cols[i] == cols[j]) {
					throw new SettingsError("Settings columns must be different for Node id, time, x, y");
				}
			}
		}

		/** Setting the column positions */
		Arrays.sort(cols);
		if (idCol == cols[0]) { NidCol = 0; }
		else if (idCol == cols[1]) { NidCol = 1; }
		else if (idCol == cols[2]) { NidCol = 2; }
		else { NidCol = 3; }
		
		if (timeCol == cols[0]) { NtimeCol = 0; }
		else if (timeCol == cols[1]) { NtimeCol = 1; }
		else if (timeCol == cols[2]) { NtimeCol = 2; }
		else { NtimeCol = 3; }
		
		if (xCol == cols[0]) { NxCol = 0; }
		else if (xCol == cols[1]) { NxCol = 1; }
		else if (xCol == cols[2]) { NxCol = 2; }
		else { NxCol = 3; }
		
		if (yCol == cols[0]) { NyCol = 0; }
		else if (yCol == cols[1]) { NyCol = 1; }
		else if (yCol == cols[2]) { NyCol = 2; }
		else { NyCol = 3; }
		
		/** read the file and save the data needed */
		try {
			extMapFile = new File(fileName);
			BufferedReader reader = new BufferedReader(new FileReader(extMapFile));
			
			int row = 0;
			/** uncomment if first line contains header */
			//dataRow = reader.readLine();	
			dataRow = reader.readLine();
			while (dataRow != null) {
				if (dataRow.trim().length() == 0 || dataRow.startsWith(COMMENT_PREFIX)) {
					dataRow = reader.readLine();
					continue; /* skip empty and comment lines */
				}
						
				nData.add(new ArrayList<String>());
		
				String[] dataArray = dataRow.split(","); /* comma separated fields */
				
				int column = 0;
				for (String item:dataArray) {	
					if ((column == idCol) || (column == timeCol) || (column == xCol) || (column == yCol)) {
						nData.get(row).add(item.trim());
					}
					column++;
				}
			
				dataRow = reader.readLine();
				row++;
			}
		} catch (IOException ioe){
			throw new SettingsError("Couldn't read ExternalMapRoute-data file " + 
					fileName + 	" (cause: " + ioe.getMessage() + ")");
		}
		
		System.out.println("Done Reading!\n\n");
		
		
		/** Dumping duplicates (same host, same time, same location */
		HashSet<List<String>> hs = new HashSet<List<String>>();
		hs.addAll(nData);
		nData.clear();
		nData.addAll(hs);
			
		/** Sorting by time */
		if (timeFormat.equals("date")) {
			Collections.sort(nData, new CompareClassB(NtimeCol));
		}
		else {
			Collections.sort(nData, new CompareClassC(NtimeCol));
		}
			
		/** Check and set the startingPoint. Either from settings or input file */
		firstTime = nData.get(0).get(NtimeCol);
		if (startingPoint != null) {
			double startDiff;
			if (timeFormat.equals("date")) {
				try {
					df.parse(startingPoint);
				} catch (ParseException e) {
					e.printStackTrace();
					throw new SettingsError("\nStarting point is not in the right format");
				}
				startDiff = dateDiff(startingPoint, firstTime);
			}
			else {
				startDiff = Double.parseDouble(firstTime) - Double.parseDouble(startingPoint);
			}
			
			if (startDiff < 0) {
				throw new SettingsError("startPoint must be less or equal to the earliest time in the input file." +
					"\nThe earliest time is: " + firstTime);
			}
		}
		else {
			startingPoint = firstTime;
		}

		/** Sorting by node id */
		Collections.sort(nData, new CompareClassA(NidCol));
		
		/** Printing all data collected from file */		
		/*
		System.out.println();
		for(int i=0; i<nData.size(); i++) {  
			for(int j=0; j<nData.get(i).size(); j++) {  
				System.out.print(nData.get(i).get(j) + "\t");  
			}  
		System.out.println();  
		}
		*/
		
		/** time1, time2: subsequent times to calculate time space between locations */
		String time1 = null;
		String time2 = null;
		int cur_node = 0;		
		String node_id1 = nData.get(0).get(NidCol);
		locations.add(new ArrayList<Coord>());
		difTimes.add(new ArrayList<Double>());
		nodesIDs.add(nData.get(0).get(NidCol));

		/** Creating locations and difTimes lists */
		for(int i=0; i<nData.size(); i++) {
			String node_id2 = nData.get(i).get(NidCol);
			
			if (!node_id2.equals(node_id1)) /* moving onto next node */
			{
				nodesIDs.add(nData.get(i).get(NidCol));
				locations.add(new ArrayList<Coord>());
				difTimes.add(new ArrayList<Double>());
				cur_node++;
			}		
						
			double x = Double.parseDouble(nData.get(i).get(NxCol));
			double y = Double.parseDouble(nData.get(i).get(NyCol));
			Coord crd = new Coord(x, y);
			locations.get(cur_node).add(crd);
			
			/** first time1 is the starting point */
			int dl = locations.get(cur_node).size();
			if (dl == 1) { time1 = startingPoint; }
			
			double dif;
			time2 = nData.get(i).get(NtimeCol);
			if (timeFormat.equals("date")) {
				dif = dateDiff(time1, time2);
			}
			else { dif = Double.parseDouble(time2) - Double.parseDouble(time1); }
			time1 = time2;
			difTimes.get(cur_node).add(dif);
			
			/** A node can be only at one place at a time */
			if (dif == 0) {
				int lastEntry = locations.get(cur_node).size();
				if (lastEntry > 1) {
					if (((locations.get(cur_node).get(lastEntry-1)).getX() != (locations.get(cur_node).get(lastEntry-2)).getX())
						|| ((locations.get(cur_node).get(lastEntry-1)).getY() != (locations.get(cur_node).get(lastEntry-2)).getY())) {
							throw new SettingsError("A node can't be at two different places at the same time!\n\n" + 
								"node " + node_id2 + " at " + time1 + "\n");
					}
				}
			}
			node_id1 = node_id2;
		}
				
		/** Printing locations and difTimes lists */		
		/*
		System.out.println();
		for(int i=0; i<locations.size(); i++) {  
			for(int j=0; j<locations.get(i).size(); j++) {  
				System.out.print("(" + (locations.get(i).get(j)).getX() + ", " + (locations.get(i).get(j)).getY() + ") \t");  
			}  
			System.out.println("\n\n");  
		}
		
		System.out.println();
		for(int i=0; i<difTimes.size(); i++) {  
			for(int j=0; j<difTimes.get(i).size(); j++) {  
				System.out.print(difTimes.get(i).get(j) + "\t");  
			}  
			System.out.println();  
		}
		*/
	}	
	
	/** Method to calculate date difference in seconds */
	public double dateDiff(String one, String two) {
        //DateFormat df = new SimpleDateFormat("MM/dd/yy HH:mm");
		Date d1 = new Date();
		Date d2 = new Date();

        try {
            d1 = df.parse(one);
			d2 = df.parse(two);			
		} catch (ParseException e) {
            e.printStackTrace();
			throw new SettingsError("\nCheck your dates! Not in right format:\n" + one + "\n" + two + "\n");
		}
		
		Calendar c1 = Calendar.getInstance();
		c1.clear();
		c1.setTime(d1);
		
		Calendar c2 = Calendar.getInstance();
		c2.clear();
		c2.setTime(d2);
		
		double time1 = c1.getTimeInMillis();
        double time2 = c2.getTimeInMillis();

        /** Calculate difference in milliseconds */
        double diff = time2 - time1;

        /** Difference in seconds */
        double diffSec = diff / 1000;
		return diffSec;
	}
	
	/** Method to get locations list of lists */
	public List<List<Coord>> getLocations () {
		return locations;
	}
	
	/** Method to get difTimes list of lists */
	public List<List<Double>> getDifTimes() {
		return difTimes;
	}
	
	/** Method to get IDs list */
	public List<String> getNodesIDs() {
		return nodesIDs;
	}


	/** Class used for sorting data by id */
	private class CompareClassA implements Comparator<List<String>> {
	
		public int idC;
	
		public CompareClassA(int column) {
			this.idC = column;
		}
	
		public int compare(List<String> ena, List<String> dio) {
			ArrayList one = (ArrayList)ena;
			ArrayList two = (ArrayList)dio;
			int result = 0;
			if (one.size() == 0 || two.size() == 0) {
				result = 0;
			} else {  
				String st1 = (String)one.get(idC);	
				String st2 = (String)two.get(idC);
				int r = st1.compareTo(st2);
				if (r == 0) {
					result = 0;
				}
				else if (r < 0) {
					result = -1;
				}
				else {
					result = 1;
				}
			}
			return result;  
		}  
	}


	/** Class used for sorting data by time (date) */
	private class CompareClassB implements Comparator<List<String>> {
	
		public int tC;
	
		public CompareClassB(int column) {
			tC = column;
		}
	
		public int compare(List<String> e, List<String> d) {
			int result = 0;
			ArrayList ena = (ArrayList)e;
			ArrayList dio = (ArrayList)d;
			if (ena.size() == 0 || dio.size() == 0) {
				result = 0;
			} else {
				String one = (String)ena.get(tC);
				String two = (String)dio.get(tC);
				double r = dateDiff(one, two);
				if (r == 0) {
					result = 0;
				}
				else if (r < 0) {
					result = 1;
				}
				else {
					result = -1;
				}
			}	
			return result;  
		}
	}


	/** Class used for sorting data by time (seconds) */
	private class CompareClassC implements Comparator<List<String>> {
	
		public int timeC;
	
		public CompareClassC(int column) {
			timeC = column;
		}
	
		public int compare(List<String> ena, List<String> dio) {
			ArrayList one = (ArrayList)ena;
			ArrayList two = (ArrayList)dio;
			int result = 0;
			if (one.size() == 0 || two.size() == 0) {
				result = 0;
			} else {  
				String time1 = (String)ena.get(timeC);	
				String time2 = (String)dio.get(timeC);
				double r = 0;
				/** Calculate difference in seconds */
				try {
					double t1 = Double.parseDouble(time1);
					double t2 = Double.parseDouble(time2);
					r = t2 - t1;
				} catch (NumberFormatException e) {
					e.printStackTrace();
					throw new SettingsError("\nCheck your time column! Not a number in lines (column " + timeC + "):\n" + ena + "\n" + dio + "\n");
				}
			
				if (r == 0) {
					result = 0;
				}
				else if (r < 0) {
					result = 1;
				}
				else {
					result = -1;
				}
			}
			return result;  
		}  
	}
}