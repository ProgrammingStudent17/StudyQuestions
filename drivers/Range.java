package drivers;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import apps.Database;
import tables.SearchTable;
import tables.Table;

/*
 * Examples:
 * 	 RANGE 5
 * 	 RANGE 3 AS x
 *
 * Response 1:
 *   query: RANGE 5
 * 	 successful
 * 	 no message
 * 	 result table:
 * 	   primary integer column "number"
 *	   rows [0]; [1]; [2]; [3]; [4]
 *
 * Response 2:
 *   query: RANGE 3 AS x
 * 	 successful
 * 	 no message
 * 	 result table:
 * 	   primary integer column "x"
 *	   rows [0]; [1]; [2]
 */
public class Range implements Driver {
	static final Pattern pattern = Pattern.compile(
		"RANGE\\s+([0-9]+)(?:\\s+AS\\s+(\\w+))?",
		Pattern.CASE_INSENSITIVE
	);

	@Override
	public Response execute(String query, Database db) {
		Matcher matcher = pattern.matcher(query.strip());
		if (!matcher.matches())
			return new Response(query, Status.UNRECOGNIZED, null, null);

		int upper = Integer.parseInt(matcher.group(1));
		String name = matcher.group(2) != null ? matcher.group(2) : "number";

		Table result_table = new SearchTable(
			"_range",
			List.of(name),
			List.of("integer"),
			0
		);

		for (int i = 0; i < upper; i++) {
			List<Object> row = new LinkedList<>();
			row.add(i);
			result_table.put(row);
		}

		return new Response(query, Status.SUCCESSFUL, null, result_table);
	}
}
