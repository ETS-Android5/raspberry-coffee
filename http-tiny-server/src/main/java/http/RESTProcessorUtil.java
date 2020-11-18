package http;

import com.google.gson.Gson;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class RESTProcessorUtil {

	/**
	 * Make sure no operation is duplicated. Check on Verb anf Path
	 * Throws an Exception isf a duplicate operation is found.
	 * @param opList
	 */
	public static void checkDuplicateOperations(List<HTTPServer.Operation> opList) {
		IntStream.range(0, opList.size())
				.boxed()
				.forEach(i -> IntStream.range(i + 1, opList.size())
						.boxed()
						.forEach(j -> {
							if (opList.get(i).getVerb().equals(opList.get(j).getVerb()) &&
									RESTProcessorUtil.pathsAreIdentical(opList.get(i).getPath(), opList.get(j).getPath())) {
								throw new RuntimeException(String.format("Duplicate entry in operations list %s %s", opList.get(i).getVerb(), opList.get(i).getPath()));
							}
						}));
	}

	/**
	 * Check path identity, even if they contains prms.
	 * Ex: /one/{one}/two/{two} and /one/{a}/two/{b} are identical
	 *
	 * @param one path to compare with two
	 * @param two path to compare with one
	 * @return true if paths match.
	 */
	public static boolean pathsAreIdentical(String one, String two) {
		boolean match = true;
		String[] oneElem = one.split("/");
		String[] twoElem = two.split("/");

		if (oneElem.length == twoElem.length) {
			for (int i = 0; match && i < oneElem.length; i++) {
				if (oneElem[i].startsWith("{") && oneElem[i].endsWith("}") &&
								twoElem[i].startsWith("{") && twoElem[i].endsWith("}")) {
					match = true;
				} else {
					match = oneElem[i].equals(twoElem[i]);
				}
			}
		} else {
			match = false;
		}
		return match;
	}

	/**
	 * Compare a path definition with a path occurrence
	 *
	 * @param pattern like /first/{val}
	 * @param path    actual path, like /first/a
	 * @return true if the path matches the pattern
	 */
	public static boolean pathMatches(String pattern, String path) {
		boolean match = true;
		String[] patternElem = pattern.split("/");
		String[] pathElem = path.split("/");

		if (patternElem.length == pathElem.length) {
			for (int i = 0; match && i < patternElem.length; i++) {
				if (patternElem[i].startsWith("{") && patternElem[i].endsWith("}")) {
					match = true;
				} else {
					match = patternElem[i].equals(pathElem[i]);
				}
			}
		} else {
			match = false;
		}
		return match;
	}

	/**
	 * Extract parameter(s) value(s) from a path occurrence matching a pattern
	 *
	 * @param pattern /one/{a}/two/{b}
	 * @param path    /one/x/two/y
	 * @return x, y
	 */
	public static List<String> getPathPrmValues(String pattern, String path) {
		List<String> returned = new ArrayList<>();
		String[] patternElem = pattern.split("/");
		String[] pathElem = path.split("/");

		if (patternElem.length == pathElem.length) {
			for (int i = 0; i < patternElem.length; i++) {
				if (patternElem[i].startsWith("{") && patternElem[i].endsWith("}")) {
					returned.add(pathElem[i]);
				}
			}
		}
		return returned;
	}

	/**
	 * Extract parameter(s) name(s) from a path occurrence
	 *
	 * @param pattern /one/{a}/two/{b}
	 * @return a, b
	 */
	public static List<String> getPathPrmNames(String pattern) {
		List<String> returned = new ArrayList<>();
		String[] patternElem = pattern.split("/");

		for (String s : patternElem) {
			if (s.startsWith("{") && s.endsWith("}")) {
				returned.add(s);
			}
		}
		return returned;
	}

	/**
	 * WiP
	 *
	 * @param contentType
	 * @param bytePayload
	 * @return
	 */
	public static Map<String, Object> getFormDataParameters(String contentType, byte[] bytePayload) throws InvalidParameterException {
		Map<String, Object> parameterMap = new HashMap<>();
		/*
		Content-Type:
		multipart/form-data; boundary=------------------ABCDEF...XYZ
		 */
		if (contentType.startsWith("multipart/form-data; boundary=")) { // Good
			/*
formData would look like this:
----------------------------690508146199201755172091
Content-Disposition: form-data; name="status"

on
----------------------------690508146199201755172091--
*/
			final String FULL_PRM_PREFIX = "Content-Disposition: form-data; name=";
			String payload = new String(bytePayload); // Only Strings for now.
			String separator = contentType.substring("multipart/form-data;".length()).split("=")[1]; // The part after 'boundary='
			String[] formPayloadElements = payload.split(separator + "\r\n");
			for (String oneFormParam : formPayloadElements) {
				String[] paramElements = oneFormParam.split("\r\n");
				if (paramElements.length > 1) {
					if (paramElements[0].startsWith(FULL_PRM_PREFIX)) {
						String prmName = paramElements[0].substring(FULL_PRM_PREFIX.length());
						if (prmName.startsWith("\"") && prmName.endsWith("\"")) {
							prmName = prmName.substring(1, prmName.length() - 1);
						} else if (prmName.startsWith("'") && prmName.endsWith("'")) {
							prmName = prmName.substring(1, prmName.length() - 1);
						}
						String value = paramElements[2];
						parameterMap.put(prmName, value);
					}
				}
			}
		} else {
			throw new InvalidParameterException(String.format("Content-Type prm should begin with [multipart/form-data; boundary=], found [%s]", contentType));
		}
		return parameterMap;
	}

	/* Utility(ies) */

	public static void generateResponseHeaders(HTTPServer.Response response, int contentLength) {
		generateResponseHeaders(response, HttpHeaders.APPLICATION_JSON, contentLength);
	}

	public static void generateResponseHeaders(HTTPServer.Response response, String contentType, int contentLength) {
		Map<String, String> responseHeaders = new HashMap<>();
		responseHeaders.put(HttpHeaders.CONTENT_TYPE, (contentType != null ? contentType : HttpHeaders.APPLICATION_JSON));
		responseHeaders.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
		responseHeaders.put("Access-Control-Allow-Origin", "*");
		response.setHeaders(responseHeaders);
	}

	public static void addErrorMessageToResponse(HTTPServer.Response response, String errMess) {
		String content = new Gson().toJson(new ErrorMessage(errMess)).toString();
		RESTProcessorUtil.generateResponseHeaders(response, content.length());
		response.setPayload(content.getBytes());
	}

	/**
	 * Used along with HTTP Error codes to contain a more significant message.
	 */
	public static class ErrorMessage {
		private final String message;

		public ErrorMessage(String mess) {
			this.message = mess;
		}
	}
}
