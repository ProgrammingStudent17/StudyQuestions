package drivers;

import static drivers.Status.*;

import java.util.regex.Pattern;

import apps.Database;
import tables.Table;

/*
 * Example:
 *   DUMP TABLE example_table
 *
 * Response:
 * 	 query: DUMP TABLE table_name
 *   successful
 *   message: "Table <example_table> has 3 rows."
 *   result table: the example_table in the database
 */
public class DumpTable implements Driver {
	static final Pattern pattern = Pattern.compile(
		"DUMP\\s+TABLE\\s+([a-z][a-z0-9_]*)",
		Pattern.CASE_INSENSITIVE
	);

	@Override
	public Response execute(String query, Database db) {
		var matcher = pattern.matcher(query.strip());
		if (!matcher.matches())
			return new Response(query, UNRECOGNIZED, null, null);

		String table_name = matcher.group(1);

		if (!db.exists(table_name)) {
			return new Response(query, FAILED, "Table <%s> does not exist".formatted(table_name), null);
		}
		else {
			Table table = db.find(table_name);
			return new Response(query, SUCCESSFUL, "Table <%s> has %d rows".formatted(
				table.getTableName(),
				table.size()
			), table);
		}
	}
}
