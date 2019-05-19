package nmea.forwarders;

import http.client.HTTPClient;
import nmea.parser.StringGenerator;
import nmea.parser.StringParsers;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This is a {@link Forwarder}, forwarding chosen data to a REST server (POST).
 * In this case this is Adafruit.IO
 * <br>
 *   Data are (can be)
 *   - Air Temperature
 *   - Atmospheric pressure
 *   - Humidity
 *   - Wind Speed
 *   - Wind direction
 *   - Rain (precipitation rate)
 *   - Dew point temperature
 * <br>
 * It must be loaded dynamically. As such, it can be set only from the properties file
 * used at startup. It - for now - cannot be managed from the Mux Web UI.
 * The REST api (of the /mux resource) is not aware of it.
 * <br>
 * To load it, use the properties file at startup:
 * <pre>
 *   forward.XX.cls=nmea.forwarders.RESTPublisher
 *   forward.XX.properties=rest.server.properties
 * </pre>
 * A jar containing this class and its dependencies must be available in the classpath.
 */
public class RESTPublisher implements Forwarder {

	private static String AIR_TEMP   = "air-temperature";
	private static String ATM_PRESS  = "atm-press";
	private static String HUMIDITY   = "humidity";
	private static String TWS        = "tws";
	private static String TWD        = "twd";
	private static String PRATE      = "prate";
	private static String DEWPOINT   = "dewpoint";

	private Properties properties;

	private final static long ONE_MINUTE = 60 * 1_000;

	private static long pushInterval = ONE_MINUTE;

	private long previousTempLog = 0;
	private long previousDewLog = 0;
	private long previousHumLog = 0;
	private long previousPressLog = 0;
	private long previousPRateLog = 0;
	private long previousTWSLog = 0;
	private long previousTWDLog = 0;

	/*
	 * @throws Exception
	 */
	public RESTPublisher() throws Exception {
	}

	private void setFeedValue(String key, String baseUrl, String feed, String value) throws Exception {
		String url = baseUrl + "/api/feeds/" + feed + "/data";
		Map<String, String> headers = new HashMap<>(1);
		headers.put("X-AIO-Key", key);
		JSONObject json = new JSONObject();
		json.put("value", new Double(value));
		if ("true".equals(this.properties.getProperty("aio.verbose"))) {
			System.out.println(String.format("URL:%s, key:%s", baseUrl, key));
			System.out.println(String.format("->->-> POSTing to feed [%s]: %s to %s", feed, json.toString(2), url));
			System.out.println("Headers:");
			headers.forEach((a, b) -> {
				System.out.println(String.format("%s: %s", a, b));
			});
		}
		if ("true".equals(this.properties.getProperty("aio.push.to.server", "true"))) {
			HTTPClient.HTTPResponse response = HTTPClient.doPost(url, headers, json.toString());
			if (response.getCode() > 299) {
				System.out.println(String.format("POST Ret: %d, %s", response.getCode(), response.getPayload()));
			}
		}
	}

	private void logAirTemp(double value) {
		String feed = AIR_TEMP;
		String url = this.properties.getProperty("aio.url");
		long now = System.currentTimeMillis();
		if (Math.abs(now - previousTempLog) > pushInterval) {
			try {
				setFeedValue(this.properties.getProperty("aio.key"), url, feed, String.valueOf(value));
				previousTempLog = now;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private void logDewTemp(double value) {
		String feed = DEWPOINT;
		String url = this.properties.getProperty("aio.url");
		long now = System.currentTimeMillis();
		if (Math.abs(now - previousDewLog) > pushInterval) {
			try {
				setFeedValue(this.properties.getProperty("aio.key"), url, feed, String.valueOf(value));
				previousDewLog = now;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private void logHumidity(double value) {
		String feed = HUMIDITY;
		String url = this.properties.getProperty("aio.url");
		long now = System.currentTimeMillis();
		if (Math.abs(now - previousHumLog) > pushInterval) {
			try {
				setFeedValue(this.properties.getProperty("aio.key"), url, feed, String.valueOf(value));
				previousHumLog = now;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private void logPressure(double value) {
		String feed = ATM_PRESS;
		String url = this.properties.getProperty("aio.url");
		long now = System.currentTimeMillis();
		if (Math.abs(now - previousPressLog) > pushInterval) {
			try {
				setFeedValue(this.properties.getProperty("aio.key"), url, feed, String.valueOf(value));
				previousPressLog = now;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private void logPRate(double value) {
		String feed = PRATE;
		String url = this.properties.getProperty("aio.url");
		long now = System.currentTimeMillis();
		if (Math.abs(now - previousPRateLog) > pushInterval) {
			try {
				setFeedValue(this.properties.getProperty("aio.key"), url, feed, String.valueOf(value));
				previousPRateLog = now;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private void logTWS(double value) {
		String feed = TWS;
		String url = this.properties.getProperty("aio.url");
		long now = System.currentTimeMillis();
		if (Math.abs(now - previousTWSLog) > pushInterval) {
			try {
				setFeedValue(this.properties.getProperty("aio.key"), url, feed, String.valueOf(value));
				previousTWSLog = now;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private void logTWD(double value) {
		String feed = TWD;
		String url = this.properties.getProperty("aio.url");
		long now = System.currentTimeMillis();
		if (Math.abs(now - previousTWDLog) > pushInterval) {
			try {
				setFeedValue(this.properties.getProperty("aio.key"), url, feed, String.valueOf(value));
				previousTWDLog = now;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	@Override
	public void write(byte[] message) {
		String str = new String(message);
//	System.out.println(">>>> Mess:" + str);
		if (StringParsers.validCheckSum(str)) {
//		String deviceId = StringParsers.getDeviceID(str);
			String sentenceId = StringParsers.getSentenceID(str);
			if ("true".equals(this.properties.getProperty("aio.verbose"))) {
				System.out.println(String.format("->->-> From NMEA data [%s]", str.trim()));
			}
			if ("MDA".equals(sentenceId)) {
				StringParsers.MDA mda = StringParsers.parseMDA(str);
//				System.out.println(String.format(
//						"MDA >> Hum: %.02f%%, AirT: %.02f\272, PRMSL: %.02f mB, TWD: %.02f\272, TWS: %.02f kts",
//						mda.relHum,
//						mda.airT,
//						mda.pressBar * 1_000,
//						mda.windDirM,
//						mda.windSpeedK));
				if (mda.relHum != null) {
					logHumidity(mda.relHum);
				}
				if (mda.airT != null) {
					logAirTemp(mda.airT);
				}
				if (mda.dewC != null) {
					logDewTemp(mda.dewC);
				}
				if (mda.pressBar != null) {
					logPressure(mda.pressBar * 1_000);
				}
				if (mda.windDirT != null) {
					logTWD(mda.windDirT);
				}
				if (mda.windSpeedK != null) {
					logTWS(mda.windSpeedK);
				}
			} else if ("XDR".equals(sentenceId)) {
				List<StringGenerator.XDRElement> xdrElements = StringParsers.parseXDR(str);
				xdrElements.stream().forEach(xdr -> {
//				System.out.println(String.format("XDR: %s -> %s", xdr.getTypeNunit(), xdr.toString()));
					if (xdr.getTypeNunit().equals(StringGenerator.XDRTypes.TEMPERATURE)) {
						logAirTemp(xdr.getValue());
					} else if (xdr.getTypeNunit().equals(StringGenerator.XDRTypes.HUMIDITY)) {
						logHumidity(xdr.getValue());
					} else if (xdr.getTypeNunit().equals(StringGenerator.XDRTypes.PRESSURE_P)) {
						logPressure(xdr.getValue() / 100);
					} else if (xdr.getTypeNunit().equals(StringGenerator.XDRTypes.GENERIC)) { // Consider it as prate...
						logPRate(xdr.getValue());
					}
				});
			} // Other sentences ignored (like GLL)
		}
	}

	@Override
	public void close() {
		System.out.println("- Stop writing (REST-> Adafruit-IO) to " + this.getClass().getName());
	}

	public static class RESTBean {
		private String cls;
		private String type = "REST-forwarder";

		public RESTBean(RESTPublisher instance) {
			cls = instance.getClass().getName();
		}
	}

	@Override
	public Object getBean() {
		return new RESTBean(this);
	}

	@Override
	public void setProperties(Properties props) {
		this.properties = props;
		try {
			pushInterval = 1_000L * Long.parseLong(this.properties.getProperty("aio.push.interval", "60"));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
