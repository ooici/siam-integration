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
	
	/**
	 * Simple utility to generate colored output with sequences understood by typical terminals.
	 * Control sequences are actually output only if the env variable tcolor=y is set.
	 */
	public static class TC {
		private static final boolean useColor = "y".equals(System.getenv("tcolor"));
	    private static final String RED = "\u001b[0;31m";
	    private static final String GREEN = "\u001b[0;32m";
	    private static final String BLUE = "\u001b[0;34m";
	    private static final String YELLOW = "\u001b[1;33m";
	    private static final String DEFAULT = "\u001b[1;00m";
	    
	    public static String red(String s)   { return useColor ? RED + s + DEFAULT : s; }
	    public static String green(String s) { return useColor ? GREEN + s + DEFAULT : s; }
	    public static String blue(String s)  { return useColor ? BLUE + s + DEFAULT : s; }
	    public static String yellow(String s){ return useColor ? YELLOW + s + DEFAULT : s; }
	}
}
