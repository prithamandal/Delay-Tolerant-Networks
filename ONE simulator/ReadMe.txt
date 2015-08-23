Copy the files in the ONE folders:

folder : file
---------------
movement : ExternalMapMovement.java
movement/map: ExternalMapRoute.java
input: 	ExternalMapReader.java
		
Compile!

The input files must be in the format of comma separated columns. If you want to change the separator, 
edit ExternalMapReader (line 110). There must be at least four columns: node id, time, x, y. If not in this order,
set the right column numbers (starting from 0) using idCol, timeCol, xCol, yCol settings. If more columns exist,
they are ignored.

Code assumes no header in the file. If you have one, you should uncomment line 100 in ExternalMapReader.
Empy and comment lines are skipped. Comment prefix is currently "#" and is set at line 30 of ExternalMapReader.

Time column can be either in date format or in seconds (or epochs). Use inputTimeFormat setting to set "date" or "sec".
The date format is currently: MM/dd/yy HH:mm
If you want to change that, edit ExternalMapReader (line 35).

Simulation time starts when the first node appears in the data (earliest time = 0 sec sim time). 
If you want to start the simulation at an earlier point, use the setting startPoint. It must be less or equal 
than the earliest time in the input file.
For the actual times appearing on the file, use Group.startPoint = 0 (when seconds).

		example 1									||		example 2
		-----------------							||		-----------------
		no starting point 							||		startPoint =  5/3/11 20:37
			&&										||			&&
		time, node, x, y							||		time, node, x, y
		-----------------							||		-----------------
		5/3/11 20:40, 0, 100, -100					||		5/3/11 20:40, 0, 100, -100
		5/3/11 20:45, 1, 200, 0						||		5/3/11 20:45, 1, 200, 0
		5/3/11 20:39, 2, 50, 150					||		5/3/11 20:39, 2, 50, 150
			=> 	starting point = 5/3/11 20:39 		||			=> 	starting point = 5/3/11 20:37 
				sim time (seconds):					||				sim time (seconds):
				0: node 2 at (50, 150)				||				2: node 2 at (50, 150)
				1: node 0 at (100, -100)			||				3: node 0 at (100, -100)
				6: node 1 at (200, 0) 				||				8: node 1 at (200, 0)
		------------------------------------		||		------------------------------------
		
		
		example 3									||		example 4
		-----------------							||		-----------------
		no starting point							||		startPoint =  0
			&&										||			&&
		time, node, x, y							||		time, node, x, y
		-----------------							||		-----------------
		1, 0, 100, -100								||		1, 0, 100, -100
		5, 1, 200, 0								||		5, 1, 200, 0
		2, 2, 50, 150								||		2, 2, 50, 150
			=> 	starting point = 1					||			=> 	starting point = 0
				sim time (seconds):					||				sim time (seconds):
				0: node 0 at (100, -100)			||				1: node 0 at (100, -100)
				1: node 2 at (50, 150)				||				2: node 2 at (50, 150)
				4: node 1 at (200, 0) 				||				5: node 1 at (200, 0)
		------------------------------------		||		------------------------------------
		
		
All the nodes are initially on the map at their first position,
but become active when they first appear in the data. They also become inactive after their last appearance in the data.
If you want to keep them active until the end of the simulation, edit activeEnd in ExternalMapRoute (line 46). 
The model -at this point- doesn't use the active times set in the settings file, if exist.

The model has two modes: 0 and 1.
When mode is 0, the speed for the movement from a location to another is calculated by the the formula: speed = distance/time, 
where time is the difference between the timestamps of the locations.
In mode 1, the speed given in the settings is used and the rest of the time (if any) is used as a wait time at the last location.
If the speed is not enough to get at the next stop on time, max speed is used and if again not enough, the simulation throws an error.

Keep in mind that the whole file is read at the beginning and all the data are kept in memory, 
so the model might not be suitable for huge files. This is because it accepts unsorted and with no fixed time interval lines.

Feel free to report any bug or make modifications!
email: gpapaneof[at]gmail[dot]com
