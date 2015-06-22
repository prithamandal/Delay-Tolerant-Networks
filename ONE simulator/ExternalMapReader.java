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

public class ExternalMapReader {
	/** Prefix for comment lines (lines starting with this are ignored) **/
	public static final String COMMENT_PREFIX = "#";
	/**List containing lists for stops. A list for each node*/
	List<List<Coord>> locations = new ArrayList<List<Coord>>();
	/**List containing lists for wait times at each stop. A list for each node*/
	List<List<Double>> difTimes = new ArrayList<List<Double>>();
	List<String> nodesIDs = new ArrayList<String>();
	
	/**new positions for data in the list (0-3)*/
	private int NidCol;
	private int NtimeCol;
	private int NxCol;
	private int NyCol;

	public ExternalMapReader (String fileName, int timeCol, int idCol, int xCol, int yCol, String timeFormat, int emmMode) {
		System.out.println("R timeCol = " + timeCol + "\tidCol = " + idCol + "\txCol = " + xCol + "\tyCol = " + yCol);
		int[] cols = {idCol, timeCol, xCol, yCol};
		System.out.println("cols id 0 = " + cols[0] + "\tcols time 1 = " + cols[1] + "\tcols x 2 = " + cols[2] + "\tcols y 3 = " + cols[3]);

		for (int i=0; i<cols.length-1; i++)
		{
			for (int j=i+1; j<cols.length; j++)
			{
				if (cols[i] == cols[j])
				{
					throw new SettingsError("Settings columns must be different for Node id, time, x, y");
				}
			}
		}

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
		
		System.out.println("A NtimeCol = " + NtimeCol + "\tNidCol = " + NidCol + "\tNxCol = " + NxCol + "\tNyCol = " + NyCol);
				
		File extMapFile = null;
		String dataRow = null;
		List<List<String>> nData = new ArrayList<List<String>>();
		
		try {
			extMapFile = new File(fileName);
			BufferedReader reader = new BufferedReader(new FileReader(extMapFile));
			
			int row = 0;
			//dataRow = reader.readLine(); //if first line contains header		
			dataRow = reader.readLine();
			while (dataRow != null) {
				if (dataRow.trim().length() == 0 || dataRow.startsWith(COMMENT_PREFIX)) {
					dataRow = reader.readLine();
					continue; /* skip empty and comment lines */
				}
						
				nData.add(new ArrayList<String>());
			
				String[] dataArray = dataRow.split(","); //comma separated fields
				//String[] dataArray = dataRow.split(" ");
				int column = 0;
			
				for (String item:dataArray) {	
					if ((column == idCol) || (column == timeCol) || (column == xCol) || (column == yCol)) {
						nData.get(row).add(item);
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
		
		
		/**Dumping duplicates (same host, same time, same location*/
		HashSet<List<String>> hs = new HashSet<List<String>>();
		hs.addAll(nData);
		nData.clear();
		nData.addAll(hs);
			
		/**sorting by time*/
		Collections.sort(nData, new CompareClassB(NtimeCol));
			
		/**keeping starting point of time*/
		String startingPoint = nData.get(0).get(NtimeCol);
			
		/**sorting by node id*/
		Collections.sort(nData, new CompareClassA(NidCol));

		/** Keeping only first and last of subsequent appearances at a specific location 
		(we need both to calculate wait time)*/ 
		ArrayList<Integer> toDel = new ArrayList<Integer>();
		
		/*
		for(int i=0; i<nData.size()-2; i++) {
			String first = nData.get(i).get(NidCol);
			String second = nData.get(i+1).get(NidCol);
			String third = nData.get(i+2).get(NidCol);
			String fourth = nData.get(i).get(NxCol);
			String fifth = nData.get(i+1).get(NxCol);
			String sixth = nData.get(i+2).get(NxCol);
			String seventh = nData.get(i).get(NyCol);
			String eighth = nData.get(i+1).get(NyCol);
			String nineth = nData.get(i+2).get(NyCol);

			if ((first.compareTo(second) == 0) && (first.compareTo(third) == 0) && 
				(fourth.compareTo(fifth) == 0) && (fourth.compareTo(sixth) == 0) &&
				(seventh.compareTo(eighth) == 0) && (seventh.compareTo(nineth) == 0)) {
					toDel.add(i+1);
			}
		}
			
		for(int i=0; i<toDel.size(); i++) {
			int remrow = toDel.get(i) - i;
			nData.remove(remrow); 
		}*/
		
		/**printing all data collected from file**/		
		/*
		System.out.println();
		for(int i=0; i<nData.size(); i++) {  
			for(int j=0; j<nData.get(i).size(); j++) {  
				System.out.print(nData.get(i).get(j) + "\t");  
			}  
		System.out.println();  
		}
		*/
		
		/**time1, time2: subsequent times to calculate wait times*/
		String time1 = null;
		String time2 = null;
		int cur_node = 0;		
		String node_id1 = nData.get(0).get(NidCol);
		locations.add(new ArrayList<Coord>());
		difTimes.add(new ArrayList<Double>());
		nodesIDs.add(nData.get(0).get(NidCol));

		for(int i=0; i<nData.size(); i++) {
			/**ud1, ud2: subsequent times to calculate wait times*/
			String node_id2 = nData.get(i).get(NidCol);
			
			if (!node_id2.equals(node_id1)) //moving onto next node
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
			
			/*doubling first location and set as time the starting point*/
			int dl = locations.get(cur_node).size();
			if (dl == 1) {
				//locations.get(cur_node).add(crd.clone()); 
				time1 = startingPoint;
			}
			
			double dif;
			time2 = nData.get(i).get(NtimeCol);
			if (timeFormat.equals("date")) {
				dif = dateDiff(time1, time2);
			}
			else { dif = Double.parseDouble(time2) - Double.parseDouble(time1); };
			time1 = time2;
			difTimes.get(cur_node).add(dif);
			node_id1 = node_id2;
		}
			
		/**printing locations and times lists*/
				
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
		
	}
	
	/**method to calculate date difference in seconds*/
	public double dateDiff(String one, String two) {
        DateFormat df = new SimpleDateFormat("MM/dd/yy HH:mm");
		Date d1 = new Date();
		Date d2 = new Date();

        try {
            d1 = df.parse(one);
			d2 = df.parse(two);			
		} catch (ParseException e) {
            e.printStackTrace();
		}
		
		Calendar c1 = Calendar.getInstance();
		c1.clear();
		c1.setTime(d1);
		
		Calendar c2 = Calendar.getInstance();
		c2.clear();
		c2.setTime(d2);
		
		double time1 = c1.getTimeInMillis();
        double time2 = c2.getTimeInMillis();

        // Calculate difference in milliseconds
        double diff = time2 - time1;

        // Difference in seconds
        double diffSec = diff / 1000;
		return diffSec;
	}
	
	public List<List<Coord>> getLocations () {
		return locations;
	}
	
	public List<List<Double>> getDifTimes() {
		return difTimes;
	}
	
	public List<String> getNodesIDs() {
		return nodesIDs;
	}
}