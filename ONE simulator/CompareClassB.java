/** Class used for sorting data by time (date) **/

package input;

import java.util.*;
import java.util.Comparator;
import java.util.Date;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class CompareClassB implements Comparator<List<String>> {
	
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
			long r = timeDif(one, two);
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

	public long timeDif(String one, String two) {
        DateFormat df = new SimpleDateFormat("MM/dd/yy HH:mm");
		Date d1 = new Date();
		Date d2 = new Date();

        try {
            d1 = df.parse(one);
			d2 = df.parse(two);
		} catch (ParseException e) {
            e.printStackTrace();
		}
		
		long t1 = d1.getTime();
		long t2 = d2.getTime();
		
		Calendar c1 = Calendar.getInstance();
		c1.clear();
		c1.setTime(d1);
		
		Calendar c2 = Calendar.getInstance();
		c2.clear();
		c2.setTime(d2);
		
		long time1 = c1.getTimeInMillis();
        long time2 = c2.getTimeInMillis();

        // Calculate difference in milliseconds
        long diff = time2 - time1;

        // Difference in seconds
        long diffSec = diff / 1000;
		return diffSec;
	}
}