package tsuki.exception

import okio.IOException
import org.json.JSONArray
import tsuki.InternalParsersApi
import tsuki.util.json.mapJSONNotNull
import tsuki.util.nullIfEmpty

/**
 * Thrown when an upstream GraphQL API returns an `errors` array in its response payload.
 *
 * Extracts and consolidates all individual error messages reported by the GraphQL server.
 *
 * @param errors The JSON array of error objects returned under the `errors` response key.
 */
public class GraphQLException @InternalParsersApi constructor(errors: JSONArray) : IOException() {

	/**
	 * List of individual error message strings parsed from the GraphQL response.
	 */
	public val messages: List<String> = errors.mapJSONNotNull { jo ->
		jo.optString("message").nullIfEmpty() ?: jo.toString()
	}

	override val message: String
		get() = messages.joinToString("\n").nullIfEmpty() ?: "GraphQL request failed"
}
