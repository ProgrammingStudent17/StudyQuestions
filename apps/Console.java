package apps;

import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import drivers.Response;

/**
 * Implements a user console for
 * interacting with a database.
 * <p>
 * Do not modify existing protocols,
 * but you may add new protocols.
 */
public class Console {
	/**
	 * The entry point for execution
	 * with user input/output.
	 *
	 * @param args unused.
	 */
	public static void main(String[] args) {
		try (
			final Database db = new Database(true);
			final Scanner in = new Scanner(System.in);
			final PrintStream out = System.out;
		)
		{
			out.print(">> ");

			String text = in.nextLine();

			List<String> queries = new LinkedList<>();
			queries.add(text);

			List<Response> responses = db.interpret(queries);

			out.println("Query:   " + responses.get(0).query());
			out.println("Status:  " + responses.get(0).status());
			out.println("Message: " + responses.get(0).message());
			out.println("Table:   " + responses.get(0).table());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
