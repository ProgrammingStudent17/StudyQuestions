package drivers;

import apps.Database;

/**
 * Defines the protocols for a driver.
 * <p>
 * Do not modify existing protocols,
 * but you may add new protocols.
 */
public interface Driver {
	/**
	 * Executes the given query against the
	 * given database and returns a response
	 * indicating the result of the query.
	 *
	 * @param query the query to execute.
	 * @param db the database to execute against.
	 * @return the response of the query.
	 **/
	Response execute(String query, Database db);
}