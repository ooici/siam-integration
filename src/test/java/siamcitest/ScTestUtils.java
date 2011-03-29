package siamcitest;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Some test utilities
 * 
 * @author carueda
 */
public class ScTestUtils {
	private ScTestUtils() {}

	private static final Pattern PARSE_STRING_PATTERN = Pattern.compile("([^ =]+) *= *(\"[^\"]*\"|[^ ,]*)");
	
	/**
	 * Helper to parse a string like "a=1 bb=45, cc=akajshd".
	 * @param string the string to parse; if null, this method returns null.
	 * @return a map with the parsed entries; null if the string is null.
	 */
	public static Map<String, String> parseString(String string) {
		if ( string == null ) {
			return null;
		}
		Map<String, String> map = new HashMap<String, String>();
		Matcher m = PARSE_STRING_PATTERN.matcher(string);
		while ( m.find() ) {
			for ( int g = 1, count = m.groupCount(); g <= count; g += 2 ) {
				map.put(m.group(g), m.group(g+1));
			}
		}
		return map;
	}
}
