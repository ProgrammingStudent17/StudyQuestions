package grade;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.function.ThrowingSupplier;

import tables.Table;

public abstract class DFSModule {
	/**
	 * Whether to log the table testing calls
	 * to a file at the root of the project.
	 * <p>
	 * The logged source code can be copied
	 * into a sandbox for manual execution.
	 * <p>
	 * You may reassign this when debugging.
	 */
	public static final boolean LOG_TO_FILE = true;

	/**
	 * A seed for the random number generator, or
	 * <code>null</code> to use a random seed.
	 * <p>
	 * The seed is used to generate a random sequence
	 * of table testing calls. To repeat a sequence,
	 * reuse the seed reported when testing it.
	 * <p>
	 * You may reassign this when debugging.
	 */
	public static final Integer RANDOM_SEED = null;

	/**
	 * The maximum time a table operation can
	 * execute in milliseconds before timeout.
	 * <p>
	 * You may reassign this when debugging.
	 */
	public static final int TIMEOUT_MILLIS = 10;

	protected static int passed;

	protected static String module_tag;
	protected static int calls_per_table;

	protected static Table subject_table;
	protected static Map<Object, List<Object>> exemplar_table;
	protected static int fingerprint;

	protected static Random RNG;
	protected static int actual_seed;
	protected static PrintStream LOG_FILE;

	@BeforeAll
	protected static final void initialize() throws IOException {
		passed = 0;

		subject_table = null;
		exemplar_table = null;
		fingerprint = 0;

		if (RANDOM_SEED != null) {
			actual_seed = RANDOM_SEED;
			RNG = new Random(actual_seed);
		}
		else {
			RNG = new Random();
			actual_seed = Math.abs(RNG.nextInt());
			RNG.setSeed(actual_seed);
		}
		System.out.printf("Random Seed: %d\n", actual_seed);
	}

	protected static final Table firstTestConstructor(ThrowingSupplier<Table> supplier) {
		Table table = null;
		try {
			table = assertTimeout(ofMillis(TIMEOUT_MILLIS*100),
				supplier,
				"Timeout in constructor (infinite loop/recursion likely)"
			);
		}
		catch (AssertionError e) {
			throw e;
		}
		catch (Exception e) {
			fail("Unexpected exception in constructor", e);
		}
		return table;
	}

	protected static final DynamicTest testTableName(String tableName) {
		final var call = "getTableName()";
		logCall(tableName, call);

		return dynamicTest(call, () -> {
			assertTimeout(ofMillis(TIMEOUT_MILLIS), () -> {
				assertEquals(
					tableName,
					subject_table.getTableName(),
					"%s has incorrect table name in schema".formatted(tableName)
				);
	        }, "Timeout in getTableName (infinite loop/recursion likely)");

			passed++;
		});
	}

	protected static final DynamicTest testColumnNames(String tableName, List<String> columnNames) {
		final var call = "getColumnNames()";
		logCall(tableName, call);

		return dynamicTest(call, () -> {
			assertTimeout(ofMillis(TIMEOUT_MILLIS), () -> {
				assertEquals(
					columnNames,
					subject_table.getColumnNames(),
					"%s has incorrect column names in schema".formatted(tableName)
				);
	        }, "Timeout in getColumnNames (infinite loop/recursion likely)");

			passed++;
		});
	}

	protected static final DynamicTest testColumnTypes(String tableName, List<String> columnTypes) {
		final var call = "getColumnTypes()";
		logCall(tableName, call);

		return dynamicTest(call, () -> {
			assertTimeout(ofMillis(TIMEOUT_MILLIS), () -> {
				assertEquals(
					columnTypes,
					subject_table.getColumnTypes(),
					"%s has incorrect column types in schema".formatted(tableName)
				);
	        }, "Timeout in getColumnTypes (infinite loop/recursion likely)");

			passed++;
		});
	}

	protected static final DynamicTest testPrimaryIndex(String tableName, Integer primaryIndex) {
		final var call = "getPrimaryIndex()";
		logCall(tableName, call);

		return dynamicTest(call, () -> {
			assertTimeout(ofMillis(TIMEOUT_MILLIS), () -> {
				assertEquals(
					primaryIndex,
					subject_table.getPrimaryIndex(),
					"%s has incorrect primary index in schema".formatted(tableName)
				);
	        }, "Timeout in getPrimaryIndex (infinite loop/recursion likely)");

			passed++;
		});
	}

	protected static final DynamicTest testClear(String tableName, List<String> columnNames, List<String> columnTypes, Integer primaryIndex) {
		final var call = "clear()";
		logCall(tableName, call);

		return dynamicTest(call, () -> {
			exemplar_table.clear();
			fingerprint = hashSum(tableName, columnNames, columnTypes, primaryIndex);

			assertTimeoutPreemptively(ofMillis(TIMEOUT_MILLIS*10), () -> {
				subject_table.clear();
	        }, "Timeout in clear (infinite loop/recursion likely)");

			thenTestSize(call);

			passed++;
		});
	}

	protected static final DynamicTest testPut(String tableName, List<String> columnTypes, Integer primaryIndex) {
		final var row = row(columnTypes, primaryIndex);
		final var key = row.get(primaryIndex);
		final var call = "put(%s)".formatted(encode(row));
		logCall(tableName, call);

		return dynamicTest(title(call, key), () -> {
			var hit = exemplar_table.containsKey(key);
			if (hit) put_hits++;
			puts++;

			if (hit)
				fingerprint -= hashSum(exemplar_table.get(key));

			exemplar_table.put(key, row);
			fingerprint += hashSum(row);

			var result = assertTimeoutPreemptively(ofMillis(TIMEOUT_MILLIS), () -> {
	        	return subject_table.put(row);
	        }, "Timeout in put (infinite loop/recursion likely)");

			if (hit)
				assertTrue(result, "Expected %s to hit for key %s".formatted(call, key));
			else
				assertFalse(result, "Expected %s to miss for key %s".formatted(call, key));

			thenTestSize(call);

			passed++;
		});
	}

	protected static final DynamicTest testRemove(String tableName, List<String> columnTypes, Integer primaryIndex) {
		final var key = t(columnTypes.get(primaryIndex));
		final var call = "remove(%s)".formatted(encode(key));
		logCall(tableName, call);

		return dynamicTest(title(call, key), () -> {
			var hit = exemplar_table.containsKey(key);
			if (hit) rem_hits++;
			rems++;

			var row = exemplar_table.remove(key);

			if (hit)
				fingerprint -= hashSum(row);

			var result = assertTimeoutPreemptively(ofMillis(TIMEOUT_MILLIS), () -> {
	        	return subject_table.remove(key);
	        }, "Timeout in remove (infinite loop/recursion likely)");

			if (hit)
				assertTrue(result, "Expected %s to hit for key %s".formatted(call, key));
			else
				assertFalse(result, "Expected %s to miss for key %s".formatted(call, key));

			thenTestSize(call);

			passed++;
		});
	}

	protected static final DynamicTest testGet(String tableName, List<String> columnTypes, Integer primaryIndex) {
		final var key = t(columnTypes.get(primaryIndex));
		final var call = "get(%s)".formatted(encode(key));
		logCall(tableName, call);

		return dynamicTest(title(call, key), () -> {
			var hit = exemplar_table.containsKey(key);
			if (hit) get_hits++;
			gets++;

			var expected = exemplar_table.get(key);

			var actual = assertTimeoutPreemptively(ofMillis(TIMEOUT_MILLIS), () -> {
	        	return subject_table.get(key);
	        }, "Timeout in get (infinite loop/recursion likely)");

			if (hit)
				assertEquals(
					expected,
					actual,
					"Expected %s to hit for key %s and return row <%s>".formatted(call, key, expected)
				);
			else
				assertNull(actual, "Expected %s to miss for key %s and return null".formatted(key, call));

			thenTestSize(call);

			passed++;
		});
	}

	protected static final void thenTestSize(String after) {
		var expected = exemplar_table.size();

		var actual = assertTimeout(ofMillis(TIMEOUT_MILLIS), () -> {
        	return subject_table.size();
        }, "After %s, timeout in size (infinite loop/recursion likely)".formatted(after));

		assertEquals(
			expected,
			actual,
			"After %s, table size is off by %d".formatted(after, actual - expected)
		);
	}

	protected static final DynamicTest testIterator() {
		final var call = "iterator traverses";

		return dynamicTest(title(call), () -> {
			var size = exemplar_table.size();
			assertEquals(
				size,
				assertTimeoutPreemptively(ofMillis(TIMEOUT_MILLIS*10), () -> {
					var iter = subject_table.iterator();

					assertNotNull(iter, "Iterator must not be null");

					var rows = 0;
					while (true) {
						var has = false;
						try {
							has = iter.hasNext();
					    }
						catch (Exception e) {
							fail("Iterator's hasNext must not throw exceptions", e);
						}

						if (!has) break;

						Object row = null;
						try {
							row = iter.next();
					    }
						catch (Exception e) {
							fail("Iterator's next must not throw exceptions", e);
						}

						assertNotNull(
							row,
							"Iterator's next must not return null"
						);

						rows++;
					}
					return rows;
		        }, "Timeout in iterator (infinite loop/recursion likely)"),
				"Iterator must traverse the correct number of rows"
			);

			passed++;
		});
	}

	protected static final DynamicTest testFingerprint() {
		final var call = "fingerprint matches";

		return dynamicTest(title(call), () -> {
			var result = 0;
			try {
				result = assertTimeoutPreemptively(ofMillis(TIMEOUT_MILLIS*10), () -> {
		        	return subject_table.hashCode();
		        }, "Timeout in fingerprint (infinite loop/recursion likely)");
			}
			catch (AssertionError e) {
				throw e;
			}
			catch (NullPointerException e) {
				fail("Fingerprint must not throw null pointer exception from iterator", e);
			}
			catch (Exception e) {
				fail("Fingerprint must not throw exception", e);
			}

			assertEquals(
				fingerprint,
				result,
				"Fingerprint is off by %d".formatted(result - fingerprint)
			);

			passed++;
		});
	}

	protected static final void testForbiddenClasses(Object subject, Class<?> cls, List<String> exempt) throws IllegalArgumentException, IllegalAccessException, SecurityException {
		if (subject_table == null)
			fail("Depends on constructor prerequisite");

		final var forbidden = new HashSet<Class<?>>();

		for (Class<?> clazz = cls; clazz != null; clazz = clazz.getSuperclass()) {
			final var fields = new HashSet<Field>();
			Collections.addAll(fields, clazz.getFields());
			Collections.addAll(fields, clazz.getDeclaredFields());

			for (Field f: fields) {
				f.setAccessible(true);
				if (f.get(subject) != null) {
					var type = f.get(subject).getClass();

					while (type.isArray())
						type = type.getComponentType();

					if (type.isPrimitive() )
						continue;

					if (exempt.contains(type.getTypeName()))
						continue;

					if (exempt.contains(type.getPackage().getName()))
						continue;

					if (type.getEnclosingClass() != null)
						if(exempt.contains(type.getEnclosingClass().getName()))
							continue;

					forbidden.add(type);
				}
				f.setAccessible(false);
			}
		};

		if (forbidden.size() > 0) {
			System.err.println("Unexpected forbidden classes:");
			forbidden.forEach(System.err::println);

			subject_table = null;
			fail("Unexpected forbidden classes <%s>".formatted(forbidden));
		}
	}

	private static int puts, put_hits;
	private static int rems, rem_hits;
	private static int gets, get_hits;

	@AfterAll
	public static final void report() {
		final var TABLE_COUNT = 3;

		var put_hit_rate = ((double) put_hits / puts) * 100;
		System.out.printf(
			"Puts: %,d (%.0f%% Hit, %.0f%% Miss)\n",
			puts,
			put_hit_rate,
			100 - put_hit_rate
		);

		var rem_hit_rate = ((double) rem_hits / rems) * 100;
		System.out.printf(
			"Removes: %,d (%.0f%% Hit, %.0f%% Miss)\n",
			rems,
			rem_hit_rate,
			100 - rem_hit_rate
		);

		var get_hit_rate = ((double) get_hits / gets) * 100;
		System.out.printf(
			"Gets: %,d (%.0f%% Hit, %.0f%% Miss)\n",
			gets,
			get_hit_rate,
			100 - get_hit_rate
		);

		System.out.println();

		System.out.printf(
			"[%s PASSED %d%% OF UNIT TESTS]\n",
			module_tag,
			(int) Math.ceil(passed / (double) (calls_per_table * TABLE_COUNT) * 100)
		);
	}

	protected static int hashSum(Object... objects) {
		var sum = 0;
		for (Object o: objects) {
			if (o instanceof List<?> list)
				sum += hashSum(list.toArray());
			else if (o != null)
				sum += o.hashCode();
		}
		return sum;
	}

	protected static final String s() {
		return Integer.toString((int) (Math.pow(RNG.nextGaussian(), 8) * Math.pow(16, 1.5)), 16);
	}

	protected static final String n() {
		while (true) {
			var s = s();
			if (Character.isLetter(s.charAt(0)))
				return s;
		}
	}

	protected static final Integer i() {
		return (int) (Math.pow(RNG.nextGaussian(), 8) * 100);
	}

	protected static final Boolean b() {
		return RNG.nextBoolean();
	}

	protected static final Object t(String type) {
		return switch (type) {
			case "string" -> n();
			case "integer" -> i();
			case "boolean" -> b();
			default -> null;
		};
	}

	protected static final List<Object> row(List<String> columnTypes, Integer primaryIndex) {
		final var row = new LinkedList<>();
		for (var i = 0; i < columnTypes.size(); i++) {
			if (i != primaryIndex && RNG.nextDouble() < 0.01)
				row.add(null);
			else
				row.add(t(columnTypes.get(i)));
		}
		return row;
	}

	protected static final String encode(List<Object> row) {
		return encode(row, true);
	}

	protected static final String encode(List<?> row, boolean checkNulls) {
		final var sb = new StringBuilder();
		if (checkNulls && row.contains(null))
			sb.append("Arrays.asList(");
		else
			sb.append("List.of(");
		for (var i = 0; i < row.size(); i++) {
			var field = row.get(i);
			if (i > 0)
				sb.append(", ");
			sb.append(encode(field));
		}
		sb.append(")");
		return sb.toString();
	}

	protected static final String encode(Object obj) {
		if (obj == null)
			return "null";
		else if (obj instanceof String)
			return "\"" + obj + "\"";
		else
			return obj.toString();
	}

	protected static final String title(String call) {
		try {
			return assertTimeout(ofMillis(TIMEOUT_MILLIS), () -> {
				return "%s when \u03B1=%d/%d=%.3f".formatted(
					call,
					subject_table.size(),
					subject_table.capacity(),
					subject_table.loadFactor()
				);
	        });
		}
		catch (AssertionError e) {
			return "%s".formatted(
				call
			);
		}
	}

	protected static final String title(String call, Object key) {
		try {
			return assertTimeout(ofMillis(TIMEOUT_MILLIS), () -> {
				return "%s %s %s when \u03B1=%d/%d=%.3f".formatted(
					call,
					exemplar_table.containsKey(key) ? "hits" : "misses",
					encode(key),
					subject_table.size(),
					subject_table.capacity(),
					subject_table.loadFactor()
				);
	        });
		}
		catch (AssertionError e) {
			return "%s %s %s".formatted(
				call,
				exemplar_table.containsKey(key) ? "hits" : "misses",
				encode(key)
			);
		}
	}

	protected static final void startLog(String tableName) {
		if (LOG_TO_FILE) {
			try {
				LOG_FILE = new PrintStream("%s.log.java".formatted(tableName));
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		else {
			LOG_FILE = null;
		}
	}

	protected static final void logRandomSeed() {
		logLine("// Random Seed: %d".formatted(actual_seed));
	}

	protected static final void logConstructor(String className, String tableName, List<String> columnNames, List<String> columnTypes, Integer primaryIndex) {
		logLine("Table %s = new %s(%s, %s, %s, %s);".formatted(
			tableName,
			className,
			encode(tableName),
			encode(columnNames, false),
			encode(columnTypes, false),
			encode(primaryIndex)
		));
	}

	protected static final void logConstructor(String className, String tableName) {
		logLine("%s = new %s(%s);".formatted(
			tableName,
			className,
			encode(tableName)
		));
	}

	protected static final void logLine(String line) {
		if (LOG_FILE != null)
			LOG_FILE.println(line);
	}

	protected static final void logCall(String tableName, String call) {
		if (LOG_FILE != null)
			LOG_FILE.printf("%s.%s;\n", tableName, call);
	}
}
