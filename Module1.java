package grade;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestMethodOrder;

import tables.HashArrayTable;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Module1 extends DFSModule {
	@BeforeAll
	public static void setup() {
		module_tag = "M1";
		calls_per_table = 2500;
	}

	@TestFactory
    @DisplayName("Prerequisites")
    @Order(0)
    public final Stream<DynamicTest> audits() throws IllegalAccessException {
		return Stream.of(
			dynamicTest("Constructor (4-ary)", () -> {
				try {
					subject_table = firstTestConstructor(() -> {
						return new HashArrayTable(
							"m1_table00",
							List.of("a", "b", "c"),
							List.of("string", "integer", "boolean"),
							0
						);
			        });
				}
				catch (Exception e) {
					fail("Unexpected exception with 4-ary constructor", e);
				}
    		}),
			dynamicTest("Forbidden Classes", () -> {
				if (subject_table == null)
					fail("Depends on constructor prerequisite");

				testForbiddenClasses(
					subject_table,
					HashArrayTable.class,
					List.of(
						"tables",
						"java.lang",
						"java.util.ImmutableCollections",
						"java.util.LinkedList"
					)
				);
    		})
    	);
    }

	@TestFactory
	@DisplayName("Create m1_table01 [s*, i, b]")
	@Order(1)
	public final Stream<DynamicTest> createTable01() {
		return testTable(
			"m1_table01",
			List.of(n(), n(), n()),
			List.of("string", "integer", "boolean"),
			0
		);
	}

	@TestFactory
	@DisplayName("Create m1_table02 [i, b, b, i*, i, b]")
	@Order(1)
	public final Stream<DynamicTest> createTable02() {
		return testTable(
			"m1_table02",
			List.of(n(), n(), n(), n(), n(), n()),
			List.of("integer", "boolean", "boolean", "integer", "integer", "boolean"),
			3
		);
	}

	@TestFactory
	@DisplayName("Create m1_table03 [s, s, s, i, i, i, b, b, b, s*, s, i, i, b, b]")
	@Order(1)
	public final Stream<DynamicTest> createTable03() {
		return testTable(
			"m1_table03",
			List.of(n(), n(), n(), n(), n(), n(), n(), n(), n(), n(), n(), n(), n(), n(), n()),
			List.of("string", "string", "string", "integer", "integer", "integer", "boolean", "boolean", "boolean", "string", "string", "integer", "integer", "boolean", "boolean"),
			9
		);
	}

	public final Stream<DynamicTest> testTable(String tableName, List<String> columnNames, List<String> columnTypes, Integer primaryIndex) {
		startLog(tableName);

		subject_table = firstTestConstructor(() -> {
			return new HashArrayTable(
				tableName,
				columnNames,
				columnTypes,
				primaryIndex
			);
        });

		logRandomSeed();
		logConstructor("HashArrayTable", tableName, columnNames, columnTypes, primaryIndex);

		exemplar_table = new HashMap<>();

		return IntStream.range(0, calls_per_table).mapToObj(i -> {
			if (i == 0)
				return testTableName(tableName);
			else if (i == 1)
				return testColumnNames(tableName, columnNames);
			else if (i == 2)
				return testColumnTypes(tableName, columnTypes);
			else if (i == 3)
				return testPrimaryIndex(tableName, primaryIndex);

			if (i == 4 || i == calls_per_table-1)
				return testClear(tableName, columnNames, columnTypes, primaryIndex);

			if (i % 5 == 0)
				if (RNG.nextBoolean())
					return testIterator();
				else
					return testFingerprint();

			var p = RNG.nextDouble();
			if (p < 0.70)
				return testPut(tableName, columnTypes, primaryIndex);
			else if (p < 0.90)
				return testRemove(tableName, columnTypes, primaryIndex);
			else
				return testGet(tableName, columnTypes, primaryIndex);
		});
	}
}
