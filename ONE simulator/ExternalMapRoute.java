package movement.map;

import input.ExternalMapReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import core.Coord;
import core.SettingsError;

/**
 * A route that consists of map nodes.
 */
public class ExternalMapRoute {
	
	private List<MapNode> stops;
	private List<Double> tempTimes;
	private String nodeID = null;
	private int index, index1; // index of the previous returned map node
	/** When does the node first and last appear in the data. 
	 * It doesn't move or exchange data, if out of range. 
	 */ 
	private double activeStart, activeEnd;
	
	/**
	 * Creates a new external map route
	 * @param stops: The stops of this route in a list
	 * @param tempTimes: The time in seconds between stops
	 */
	public ExternalMapRoute(List<MapNode> stops, List<Double> tempTimes, String nodeID) {
		assert stops.size() > 0 : "Route needs stops";
		assert index < stops.size() : "Too big start index for route";		
		this.stops = stops;
		this.tempTimes = tempTimes;
		this.nodeID = nodeID;
		this.index = 0;
		this.index1 = 1; //node becomes active after the first time.
		
		/** Calculate active times. The time it first and last appears in the data */
		int sum = 0;
		for (double t : tempTimes) {
			sum += t;
		}
		activeStart = tempTimes.get(0);
		activeEnd = sum;
	}
		
	/**
	 * Sets the next index for this route
	 * @param index The index to set
	 */
	public void setNextIndex(int index) {
		if (index > stops.size()) {
			index = stops.size();
		}
		
		this.index = index;
	}
	
	/**
	 * Returns the number of stops on this route
	 * @return the number of stops on this route
	 */
	public int getNrofStops() {
		return stops.size();
	}
	
	/**
	 * Returns the stops on this route
	 * @return the stops on this route
	 */
	public List<MapNode> getStops() {
		return this.stops;
	}
	
	/**
	 * Returns the next stop on the route
	 * @return the next stop on the route
	 */
	public MapNode nextStop() {
		MapNode next = stops.get(index);
		
		index++;
		if (index >= stops.size()) { // reached last stop
			index = stops.size()-1; // go next to prev to last stop
		}
		
		return next;		
	}
	
	/**
	 * Returns the time for the next route
	 * @return the time for the next route
	 */
	public double nextDifTime() {
		double next = tempTimes.get(index1);
		index1++;
		
		if (index1 >= tempTimes.size()) {
				index1 = tempTimes.size()-1;

		}
		
		return next;
	}
	
	/**
	 * Returns the time when the node first becomes active
	 * @return the time when node becomes active
	 */
	public double getActiveStart() {
		return activeStart;
	}
	
	/**
	 * Returns the last time when the node is active
	 * @return the time when node becomes inactive
	 */
	public double getActiveEnd() {
		return activeEnd;
	}
	
	
	public String getNodeID() {
		return this.nodeID;
	}
	
	/**
	 * Returns a new route with the same settings
	 * @return a replicate of this route
	 */
	public ExternalMapRoute replicate() {
		return new ExternalMapRoute(stops, tempTimes, nodeID);
	}
	
	public String toString() {
		return ("External Map route with " + getNrofStops() + " stops");
	}
	
	/**
	 * Reads routes from files defined in Settings
	 * @param fileName: name of the file where to read routes
	 * @param timeCol, idCol, xCol, yCol: the columns of the data
	 * @param timeFormat: Is time in seconds or date format? 
	 * @param emmMode: mode of the external movement model
	 * @param map SimMap: where corresponding map nodes are found
	 * @return A list of ExternalMapRoutes that were read
	 */
	public static List<ExternalMapRoute> readRoutes(String fileName, int timeCol, int idCol, int xCol, int yCol, String timeFormat, int emmMode, SimMap map) {
		List<ExternalMapRoute> routes = new ArrayList<ExternalMapRoute>();
		ExternalMapReader reader = new ExternalMapReader(fileName, timeCol, idCol, xCol, yCol, timeFormat, emmMode);
		List<List<Coord>> coords;
		List<List<Double>> times;
		List<String> nodesIDs;
		int l_index = 0;
		File extMapFile = null;
		boolean mirror = map.isMirrored();
		double xOffset = map.getOffset().getX();
		double yOffset = map.getOffset().getY();
		
		extMapFile = new File(fileName);
		coords = reader.getLocations();
		times = reader.getDifTimes();
		nodesIDs = reader.getNodesIDs();
		
		for (List<Coord> l : coords) {			
			List<MapNode> nodes = new ArrayList<MapNode>();
			for (Coord c : l) {
				// make coordinates match sim map data
				if (mirror) {
					c.setLocation(c.getX(), -c.getY());
				}
				c.translate(xOffset, yOffset);
				MapNode node = map.getNodeByCoord(c);
				if (node == null) {
					Coord orig = c.clone();
					orig.translate(-xOffset, -yOffset);
					orig.setLocation(orig.getX(), -orig.getY());
					
					throw new SettingsError("ExternalMapRoute in file " + extMapFile + 
							" contained invalid coordinate " + c + " orig: " +
							orig);
				}
				nodes.add(node);
			}
			
			routes.add(new ExternalMapRoute(nodes, times.get(l_index), nodesIDs.get(l_index)));
			l_index++;
		}
		
		return routes;
	}
}
