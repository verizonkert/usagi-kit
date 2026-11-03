package tsuki.exception

import org.jsoup.HttpStatusException
import java.net.HttpURLConnection

/**
 * Thrown when a remote resource (e.g. manga details page, chapter list, or image asset)
 * returns an HTTP 404 (Not Found) response status.
 *
 * @param message Descriptive explanation of the missing resource.
 * @param url The resource URL that returned the 404 status.
 */
public class NotFoundException(
	message: String,
	url: String,
) : HttpStatusException(message, HttpURLConnection.HTTP_NOT_FOUND, url)
