Copy the files in the ONE folders:

folder : file
---------------
movement : ExternalMapMovement.java
movement/map: ExternalMapRoute.java
input: 	ExternalMapReader.java
		CompareClassA.java
		CompareClassB.java
		
Compile!

The date format is currently: MM/dd/yy HH:mm
If you want to change that, edit CompareClassB (line 45) and ExternalMapReader (line 238).

Simulation time starts when the first node appears in the data. All the nodes are initially on map at their first position,
but become active when they first appear in the data. They also become inactive after their last appearance in the data.
If you want to keep them active until the end of the simulation, edit activeEnd in ExternalMapRoute (line 47). 
The model -at this point- doesn't use the active times set in the settings file, if exist.

The data at each row are separated by comma. If you want to change the separator, edit ExternalMapReader (line 94).
Code assumes no header in the file. If you have one, you should uncomment line 84 in ExternalMapReader.
Empy and comment lines are skipped. Comment prefix is currently "#" and is set at line 23 of ExternalMapReader.

The model has two modes: 0 and 1.
When mode is 0, the speed for the movement from a location to another is calculated by the the formula: speed = distance/time, 
where time is the difference between the timestamps of the locations.
In mode 1, the speed given in the settings is used and the rest of the time is used as a wait time at the last location.
If the speed is not enough, the simulation throws an error.