package movement;

import java.util.List;

import core.SettingsError;
import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.ExternalMapRoute;
import core.Coord;
import core.Settings;
import core.SettingsError;
import core.SimError;
import core.SimClock;

/**
 * Map based movement model that uses predetermined paths within the map area.
 * Nodes using this model (can) stop on every route waypoint and find their
 * way to next waypoint using {@link DijkstraPathFinder}.
 */
public class ExternalMapMovement extends MapBasedMovement implements 
	SwitchableMovement {
	/** Router's setting namespace ({@value})*/
	public static final String EXTMAPMOV_NS = "ExternalMapMovement";
	/** Per node group setting used for selecting a route file ({@value}) */
	public static final String EMM_FILE_S = "extmapFile";
	/** what column is each setting at the file, starting from zero (node id, time, x, y)*/
	public static final String EMM_ID_COL_S = "idCol";
	public static final String EMM_TIME_COL_S = "timeCol";
	public static final String EMM_X_COL_S = "xCol";
	public static final String EMM_Y_COL_S = "yCol";
	/** format of time: date or seconds (or epochs, doesn't matter). acceptable values: "date", "sec" */
	public static final String EMM_TIME_FORMAT_S = "inputTimeFormat";
	/** Movement mode: 	0 - calculate the speed of the node for each movement using distance and time difference. Default.
						1 - use the given speed and if there's any time left, it's used as a wait time at the destination.
							if speed is too low, it will throw an error. */
	public static final String EMM_MODE_S = "extMovMode";
	
	/**default columns for settings*/
	public static final int EMM_DEFAULT_ID_COL = 0;
	public static final int EMM_DEFAULT_TIME_COL = 1;
	public static final int EMM_DEFAULT_X_COL = 2;
	public static final int EMM_DEFAULT_Y_COL = 3;
	/** default time format */
	public static final String EMM_DEFAULT_TIME_FORMAT = "sec";
	/** default movement mode */
	public static final int EMM_DEFAULT_MODE = 0;
	
	/** the Dijkstra shortest path finder */
	private DijkstraPathFinder pathFinder;

	/** Prototype's reference to all routes read for the group */
	private List<ExternalMapRoute> allRoutes = null;
	/** next route's index to give by prototype */
	private Integer nextRouteIndex = null;
	
	/** Route of the movement model's instance */
	private ExternalMapRoute route;
	private double waitTime = 0;
	private double speed = 0;
	private double availTime = 0;
	private double nextMoveTime = 0;
	
	private int idCol;
	private int timeCol;
	private int xCol;
	private int yCol;
	private String timeFormat;
	private int emmMode;
	
	/**
	 * Creates a new movement model based on a Settings object's settings.
	 * @param settings The Settings object where the settings are read from
	 */
	public ExternalMapMovement(Settings settings) {
		super(settings);
		//Settings extmapSettings = new Settings(EXTMAPMOV_NS);
		String fileName = settings.getSetting(EMM_FILE_S);
		
		if (settings.contains(EMM_TIME_COL_S) && settings.contains(EMM_ID_COL_S) && 
			settings.contains(EMM_X_COL_S) && settings.contains(EMM_Y_COL_S)) {
			idCol = settings.getInt(EMM_ID_COL_S);
			timeCol = settings.getInt(EMM_TIME_COL_S);
			xCol = settings.getInt(EMM_X_COL_S);
			yCol = settings.getInt(EMM_Y_COL_S);
			System.out.println("Using manually set columns settings");
		}
		else {
			idCol = EMM_DEFAULT_ID_COL;
			timeCol = EMM_DEFAULT_TIME_COL;
			xCol = EMM_DEFAULT_X_COL;
			yCol = EMM_DEFAULT_Y_COL;
			System.out.println("Using default columns settings");
		}
		System.out.println("timeCol = " + timeCol + "\tidCol = " + idCol + "\txCol = " + xCol + "\tyCol = " + yCol + "\n");
		
		if (settings.contains(EMM_TIME_FORMAT_S)) {
			timeFormat = settings.getSetting(EMM_TIME_FORMAT_S);
			if (!timeFormat.equalsIgnoreCase("sec") && !timeFormat.equalsIgnoreCase("date")) {
				throw new SettingsError("Time format is not right. It must be either \"date\" or \"sec\".\n");
			}
			else {
				System.out.println("Using manually set input time format");
			}
		}
		else { 
			timeFormat = EMM_DEFAULT_TIME_FORMAT;
			System.out.println("Using default time format");
		}
		System.out.println("inputTimeFormat = " + timeFormat + "\n");
		
		if (settings.contains(EMM_MODE_S)) {
			this.emmMode = settings.getInt(EMM_MODE_S);
			if (emmMode != 0 && emmMode != 1) {
				throw new SettingsError("extMovMode must be either 0 or 1\n");
			}
			else {
				System.out.println("Using manually set movement mode");
			}
		}
		else { 
			this.emmMode = EMM_DEFAULT_MODE;
			System.out.println("Using default movement mode");
		}
		System.out.println("movement mode = " + this.emmMode + "\n");
		
		allRoutes = ExternalMapRoute.readRoutes(fileName, timeCol, idCol, xCol, yCol, timeFormat, this.emmMode, getMap());
		nextRouteIndex = 0;
		pathFinder = new DijkstraPathFinder(getOkMapNodeTypes());
		this.route = this.allRoutes.get(this.nextRouteIndex).replicate();
		if (this.nextRouteIndex >= this.allRoutes.size()) {
			this.nextRouteIndex = 0;
		}
	}
	
	/**
	 * Copyconstructor. Gives a route to the new movement model.
	 * @param proto The MapRouteMovement prototype
	 */
	protected ExternalMapMovement(ExternalMapMovement proto) {
		super(proto);
		this.route = proto.allRoutes.get(proto.nextRouteIndex).replicate();	
		this.pathFinder = proto.pathFinder;
		this.nextMoveTime = route.getActiveStart();
		
		this.idCol = proto.idCol;
		this.timeCol = proto.timeCol;
		this.xCol = proto.xCol;
		this.yCol = proto.yCol;
		this.timeFormat = proto.timeFormat;
		this.emmMode = proto.emmMode;
		System.out.println("proto emmMode = " + this.emmMode);
		
		proto.nextRouteIndex++; // give routes in order
		if (proto.nextRouteIndex >= proto.allRoutes.size()) {
			proto.nextRouteIndex = 0;
		}
	}

	@Override
	public Path getPath() {
		Path p = new Path(generateSpeed());
		MapNode to = route.nextStop();
		
		List<MapNode> nodePath = pathFinder.getShortestPath(lastMapNode, to);
		
		// this assertion should never fire if the map is checked in read phase
		assert nodePath.size() > 0 : "No path from " + lastMapNode + " to " +
			to + ". The simulation map isn't fully connected";
		
		double pathDist = 0;
		for (int i=0; i<nodePath.size(); i++) {
			MapNode node = nodePath.get(i);			
			p.addWaypoint(node.getLocation());
			if (i<nodePath.size()-1) {
				pathDist += node.getLocation().distance(nodePath.get(i+1).getLocation());
			}
		}
		
		/** Set the speed and wait time for the path to the next stop **/
		this.availTime = route.nextDifTime();
		if (this.availTime == 0) {
			throw new SimError("A node can't be at two different places at the same time!\n\n" + 
			"simulation time: " + this.nextMoveTime + "\t node " + route.getNodeID() + " at " + lastMapNode + " and " + to + "\n");
		}

		if (pathDist == 0) {
			this.speed = 0;
			this.waitTime = this.availTime;
		}
		else {
			//System.out.println("emmMode = " + this.emmMode);
			if (this.emmMode == 0) {
				this.speed = calcSpeed(this.availTime, pathDist);
				//System.out.println("auto speed = " + this.speed);
			}
			else {
				this.speed = p.getSpeed();
				System.out.println("p.speed = " + this.speed);
			}
			this.waitTime = calcWaitTime(route.getNodeID(), lastMapNode, to, this.availTime, this.speed, pathDist);
		}
		
		System.out.println("\n" + SimClock.getTime() + "\tnode " + route.getNodeID() + " from = " + lastMapNode + " to = " + to + "\t, speed =" + this.speed + " will wait " + this.waitTime);
		lastMapNode = to;
		
		return p;
	}	
	
	
	public double calcSpeed(double difTime, double distance) {
		double sp = distance/difTime;
		//System.out.println("calculating speed. diftime = " + difTime + "\tdistance = " + distance + "\tspeed = " + sp);
		return sp;
	}
	
	public double calcWaitTime(String nodeID, MapNode from, MapNode to, double difTime, double speed, double distance) {
		//System.out.println("calculating wait time. diftime = " + difTime + "\tdistance = " + distance + "\tspeed = " + speed + "\tdist/speed = " + (distance/speed));
		double wT = difTime - (distance/speed);
		//System.out.println("wait time = " + waitTime);
		if (wT < 0) {
			throw new SimError("Node " + nodeID + " is too slow to get from (" 
				+ from.getLocation().getX() + ", " + from.getLocation().getY() + ") to "
				+ to.getLocation().getX() + ", " + to.getLocation().getY() + ") !!");
		}
		//System.out.println("waitTime = " + waitTime);
		return wT;
	}

	
	/** return the time for the next movement **/
	@Override
	public double nextPathAvailable() {
	//System.out.println();
	//System.out.println("node " + route.getNodeID() + "\tnextMoveTime = " + this.nextMoveTime + "\t availTime = " + this.availTime);
	//System.out.println("node " + route.getNodeID() + " is at " + lastMapNode);
		//return SimClock.getTime() + availTime;
		this.nextMoveTime += this.availTime;
		System.out.println("\tnextMoveTime = " + this.nextMoveTime);
		return this.nextMoveTime;
	}
	/**
	 * Returns the first stop on the route
	 */
	@Override
	public Coord getInitialLocation() {
		if (lastMapNode == null) {
			lastMapNode = route.nextStop();
		}
		return lastMapNode.getLocation().clone();
	}
	
	@Override
	public Coord getLastLocation() {
		if (lastMapNode != null) {
			return lastMapNode.getLocation().clone();
		} else {
			return null;
		}
	}
	
	
	@Override
	public ExternalMapMovement replicate() {
		return new ExternalMapMovement(this);
	}	

	/**
	 * Returns the list of stops on the route
	 * @return The list of stops
	 */
	public List<MapNode> getStops() {
		return route.getStops();
	}
	
	@Override
	public boolean isActive() {	
		double time = SimClock.getTime();
		
		if (time < route.getActiveStart() || time > route.getActiveEnd() ) {
				return false; // out of range
		}
		return true;
	}
}