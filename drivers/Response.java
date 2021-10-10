package drivers;

import tables.Table;

/**
 * Defines the protocols for a response.
 * <p>
 * Do not modify the protocols.
 */
public record Response (
	String query,
	Status status,
	String message,
	Table table
) {}