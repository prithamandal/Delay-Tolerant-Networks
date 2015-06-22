/** Class used for sorting data by id **/

package input;

import java.util.*;
import java.util.Comparator;

public class CompareClassA implements Comparator<List<String>> {
	
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