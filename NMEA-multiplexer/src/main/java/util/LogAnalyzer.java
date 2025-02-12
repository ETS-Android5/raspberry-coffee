package util;

import calc.GeoPoint;
import calc.GeomUtil;
import calc.GreatCircle;
import calc.GreatCirclePoint;
import calc.calculation.AstroComputer;
import nmea.parser.GeoPos;
import nmea.parser.RMC;
import nmea.parser.StringParsers;
import util.swing.SwingFrame;

import java.awt.HeadlessException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static nmea.parser.StringParsers.GGA_ALT_IDX;

/**
 * Analyze a log file
 * Time, distance, min-max, type and number of strings
 *
 * TODO: From a file in a zip
 */
public class LogAnalyzer {

	private static SimpleDateFormat SDF = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss z");
	static {
		SDF.setTimeZone(TimeZone.getTimeZone("etc/UTC"));
	}

	private final static long SEC = 1_000L;
	private final static long MIN = 60 * SEC;
	private final static long HOUR = 60 * MIN;
	private final static long DAY = 24 * HOUR;

	private static String msToHMS(long ms) {
		String str = "";
		long remainder = ms;
		int days = (int) (remainder / DAY);
		remainder -= (days * DAY);
		int hours = (int) (remainder / HOUR);
		remainder -= (hours * HOUR);
		int minutes = (int) (remainder / MIN);
		remainder -= (minutes * MIN);
		float seconds = (float) (remainder / SEC);
		if (days > 0) {
			str = days + " day(s) ";
		}
		if (hours > 0 || !str.trim().isEmpty()) {
			str += hours + " hour(s) ";
		}
		if (minutes > 0 || !str.trim().isEmpty()) {
			str += minutes + " minute(s) ";
		}
		str += seconds + " sec(s)";
		return str.trim();
	}

	enum SpeedUnit {
		KN(1, "kn"),
		KMH(1.852f, "km/h");

		private final float knToUnit;
		private final String unitLabel;

		SpeedUnit(float adjust, String label) {
			this.knToUnit = adjust;
			this.unitLabel = label;
		}

		public float convert() {
			return this.knToUnit;
		}

		public String label() {
			return this.unitLabel;
		}
	}

	private static SpeedUnit unitToUse = SpeedUnit.KN;

	public static class DatedPosition {
		GeoPos position;
		Date date;
		long recId;

		DatedPosition position(GeoPos position) {
			this.position = position;
			return this;
		}

		DatedPosition date(Date date) {
			this.date = date;
			return this;
		}

		DatedPosition recId(long recId) {
			this.recId = recId;
			return this;
		}

		public GeoPos getPosition() {
			return this.position;
		}

		public Date getDate() {
			return this.date;
		}

		public long getRecId() { return this.recId; }
	}

	private static void appendToMap(Map<String, Long> map, String id) {
		Long nb = map.get(id);
		if (nb == null) {
			nb = Long.valueOf(0L);
		}
		map.put(id, nb + 1);
	}

	private final static double MAX_CALCULATED_SPEED = 20 * 1852 / 3600; // 10 knots in m/s

	public static void main(String... args) {

		boolean verbose = "true".equals(System.getProperty("verbose"));
		String speedUnit = System.getProperty("speed.unit");
		if (speedUnit != null) {
			switch (speedUnit) {
				case "KMH":
					unitToUse = SpeedUnit.KMH;
					break;
				case "KN":
					unitToUse = SpeedUnit.KN;
					break;
				default:
					System.out.println(String.format("Unknown unit [%s], defaulting to knots.", speedUnit));
					unitToUse = SpeedUnit.KN;
					break;
			}
		}

		if (args.length == 0) {
			throw new IllegalArgumentException("Please provide the name of the file to analyze as first parameter");
		}
		List<DatedPosition> positions = new ArrayList<>();
		try {
			InputStream fis;
			Map<String, Long> validStrings = new HashMap<>();
			Map<String, Long> invalidStrings = new HashMap<>();

			String dataFileName = args[0];
			if (dataFileName.endsWith(".zip")) {
				if (args.length != 2) {
					throw new IllegalArgumentException("Please provide the file path in archive as second parameter");
				}
				String pathInArchive = args[1]; // Required
				ZipFile zipFile = new ZipFile(dataFileName);
				ZipEntry zipEntry = zipFile.getEntry(pathInArchive);
				if (zipEntry == null) { // Path not found in the zip, take first entry.
					zipEntry = zipFile.entries().nextElement();
				}
				fis = zipFile.getInputStream(zipEntry);
			} else {
				fis = new FileInputStream(dataFileName);
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));

			// TODO Option to check chronology and continuity
			BufferedWriter bw = new BufferedWriter(new FileWriter("stat.csv"));

			// Accumulators and others
			GeoPos previousPos = null;
			double distanceInKm = 0d;
			double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
			double minLng = Double.MAX_VALUE, maxLng = -Double.MAX_VALUE;

			Date minLatDate = null, maxLatDate = null,
				 minLngDate = null, maxLngDate = null;

			double maxSpeed = -Double.MAX_VALUE, minSpeed = Double.MAX_VALUE;
			double minAlt = Double.MAX_VALUE, maxAlt = -Double.MAX_VALUE;
			double maxSpeedCalc = -Double.MAX_VALUE;
			// For stat file
			long previousDate = -1L;
			long statLineNo = 0;

			bw.write("Idx;time (epoch);deltaT;deltaDist;deltaT(2);cog;sog\n");
			statLineNo += 1;

			long minLatIdx = -1,
				 minLngIdx = -1,
				 maxLatIdx = -1,
				 maxLngIdx = -1;

			long nbRec = 0L, totalNbRec = 0L, originalFileRecNo = 0L;
			long nbRMCRec = 0L;
			Date start = null;
			Date arrival = null;
			Date prevRMCTime = null;
			String line;
			boolean keepReading = true;
			while (keepReading) {
				line = br.readLine();
				if (line == null) {
					keepReading = false;
				} else {
					originalFileRecNo++;
					if (StringParsers.validCheckSum(line)) {
						totalNbRec++;
						String id = StringParsers.getSentenceID(line);
					    appendToMap(validStrings, id);
						if (id.equals("RMC")) {
							nbRec++;
							nbRMCRec++;
							RMC rmc = StringParsers.parseRMC(line);
							assert (rmc != null);
							if (rmc.isValid()) {
								// Get date, speed, position (for distance)
								Date rmcDate = rmc.getRmcDate();
								Date rmcTime = rmc.getRmcTime();

								if (start == null) {
									start = rmcTime;
								} else {
									arrival = rmcTime;
								}
								GeoPos gp = rmc.getGp();
								if (gp != null) {
									double distance = 0;
									double calcSpeed = 0;
									if (previousPos != null) {
										distance = GeomUtil.haversineKm(previousPos.lat, previousPos.lng, gp.lat, gp.lng);
										if (rmcTime != null && prevRMCTime != null) {
											double deltaT = rmcTime.getTime() - prevRMCTime.getTime();
											if (deltaT > 0) {
												calcSpeed = (distance * 1000d) / (deltaT / 1000d); // ms
//												if (calcSpeed > MAX_CALCULATED_SPEED) {
//													System.out.println(String.format("Speed: %.02f", calcSpeed));
//												}
												maxSpeedCalc = Math.max(maxSpeedCalc, calcSpeed);
											}
										}
									}
									if (calcSpeed < MAX_CALCULATED_SPEED) {
										if (rmcTime != null) {
											positions.add(new DatedPosition()
													.date(rmcTime)
													.position(gp)
													.recId(originalFileRecNo));
										}
										if (gp.lat < minLat) {
											minLat = gp.lat;
											minLatIdx = totalNbRec - 1;
											minLatDate = rmcTime;
										}
										if (gp.lat > maxLat) {
											maxLat = gp.lat;
											maxLatIdx = totalNbRec - 1;
											maxLatDate = rmcTime;
										}
										if (gp.lng < minLng) {
											minLng = gp.lng;
											minLngIdx = totalNbRec - 1;
											minLngDate = rmcTime;
										}
										if (gp.lng > maxLng) {
											maxLng = gp.lng;
											maxLngIdx = totalNbRec - 1;
											maxLngDate = rmcTime;
										}
										assert (rmcTime != null);
										// TODO Speed
										double cog = rmc.getCog();
										double sog = rmc.getSog();
										if (previousPos != null) {
											distance = GeomUtil.haversineKm(previousPos.lat, previousPos.lng, gp.lat, gp.lng);
											if (verbose && distance == 0) {
												System.out.println(String.format("Rec # %d (RMC# %d), Step: %.03f km between %s and %s (%s, previous was %s)",
														originalFileRecNo,
														nbRMCRec,
														distance,
														previousPos.toString(),
														gp.toString(),
														rmcTime != null ? SDF.format(rmcTime) : "",
														prevRMCTime != null ? SDF.format(prevRMCTime) : "-"));
											}
											distanceInKm += distance;
											bw.write(String.format("%d;%d;%d;%f;=(B%d-B%d);%s;%s\n",
													(totalNbRec - 1),
													rmcTime != null ? rmcTime.getTime() : 0,
													rmcTime != null ? (rmcTime.getTime() - previousDate) : 0,
													distance,
													(statLineNo + 1),
													statLineNo,
													cog,
													sog));
											prevRMCTime = rmcTime;
										} else {
											bw.write(String.format("%d;%d;%d;;;%s;%s\n",
													(totalNbRec - 1),
													rmcTime.getTime(),
													(rmcTime.getTime() - previousDate),
													cog,
													sog));
										}
										statLineNo += 1;
										previousPos = gp;
										if (rmcTime != null) {
											previousDate = rmcTime.getTime();
										}
										maxSpeed = Math.max(maxSpeed, rmc.getSog());
										minSpeed = Math.min(minSpeed, rmc.getSog());
									} else {
										System.out.println(String.format("Skipping too fast record: %.04f ms", calcSpeed));
									}
								} else {
									System.out.println(String.format(">> NO RMC Position at rec#%d", originalFileRecNo));
								}
							}
						} else if (id.equals("GGA")) {
							nbRec++;
							List<Object> gga = StringParsers.parseGGA(line);
							if (gga != null) {
								double alt = (Double) gga.get(GGA_ALT_IDX);
								maxAlt = Math.max(maxAlt, alt);
								minAlt = Math.min(minAlt, alt);
							}
						}
						// More Sentence IDs ?..
					} else {
						if (verbose) {
							System.out.println(String.format("Invalid data [%s]", line));
						}
						String strId = "---";
						try {
							strId = StringParsers.getSentenceID(line);
						} catch (Throwable t) {
							// Absorb
						}
						appendToMap(invalidStrings, strId);
					}
				}
			}
			br.close();
			bw.close();
			System.out.println("+-------------------------------------+");
			System.out.println("| Checkout the spreadsheet stat.csv.  |");
			System.out.println("+-------------------------------------+");

			// Display summary
			assert (start != null && arrival != null);
			System.out.println(String.format("Started %s", SDF.format(start)));
			System.out.println(String.format("Arrived %s", SDF.format(arrival)));
			System.out.println(String.format("Used %s record(s) out of %s. Total distance: %.03f %s, in %s. Avg speed:%.03f %s",
					NumberFormat.getInstance().format(nbRec),
					NumberFormat.getInstance().format(totalNbRec),
					distanceInKm / (unitToUse.equals(SpeedUnit.KMH) ? 1 : 1.852),
					unitToUse.equals(SpeedUnit.KMH) ? "km" : "nm",
					msToHMS(arrival.getTime() - start.getTime()),
					(distanceInKm / ((arrival.getTime() - start.getTime()) / ((double) HOUR))) / (unitToUse.equals(SpeedUnit.KMH) ? 1 : 1.852),
					unitToUse.label()));
			System.out.println(String.format("Max Speed (SOG): %.03f %s", maxSpeed * unitToUse.convert(), unitToUse.label()));
			System.out.println(String.format("Min Speed (SOG): %.03f %s", minSpeed * unitToUse.convert(), unitToUse.label()));
			double deltaAlt = (maxAlt - minAlt);
			if (!Double.isInfinite(deltaAlt)) {
				System.out.println(String.format("Min alt: %.02f m, Max alt: %.02f m, delta %.02f m", minAlt, maxAlt, (maxAlt - minAlt)));
			}
			System.out.println(String.format("Top-Left    :%s (%f / %f)", new GeoPos(maxLat, minLng).toString(), maxLat, minLng));
			System.out.println(String.format("Bottom-Right:%s (%f / %f)", new GeoPos(minLat, maxLng).toString(), minLat, maxLng));
			System.out.println(String.format("Min Lat (%s) record idx (in %s): %d, at %s", GeomUtil.decToSex(minLat, GeomUtil.SWING, GeomUtil.NS), args[0], minLatIdx, SDF.format(minLatDate)));
			System.out.println(String.format("Max Lat (%s) record idx (in %s): %d, at %s", GeomUtil.decToSex(maxLat, GeomUtil.SWING, GeomUtil.NS), args[0], maxLatIdx, SDF.format(maxLatDate)));
			System.out.println(String.format("Min Lng (%s) record idx (in %s): %d, at %s", GeomUtil.decToSex(minLng, GeomUtil.SWING, GeomUtil.EW), args[0], minLngIdx, SDF.format(minLngDate)));
			System.out.println(String.format("Max Lng (%s) record idx (in %s): %d, at %s", GeomUtil.decToSex(maxLng, GeomUtil.SWING, GeomUtil.EW), args[0], maxLngIdx, SDF.format(maxLngDate)));
			System.out.println();

			System.out.println(String.format("Max Calc Speed: %.03f ms", maxSpeedCalc));

			// Width, height
			GreatCircle gc = new GreatCircle(new GreatCirclePoint(new GeoPoint(Math.toRadians(minLat),
					                                                           Math.toRadians(minLng))), // bottom left
					                         new GreatCirclePoint(new GeoPoint(Math.toRadians(maxLat),
													                           Math.toRadians(maxLng)))); // top right
			double distBLTR = gc.getDistanceInNM();
			gc = new GreatCircle(new GreatCirclePoint(new GeoPoint(Math.toRadians(maxLat),
					                                               Math.toRadians(minLng))), // top left
					             new GreatCirclePoint(new GeoPoint(Math.toRadians(minLat),
										                           Math.toRadians(maxLng)))); // bottom right
			double distTLBR = gc.getDistanceInNM();
			distBLTR *= unitToUse.convert(); // (unitToUse.equals(SpeedUnit.KMH) ? 1.852 : 1);
			distTLBR *= unitToUse.convert(); // (unitToUse.equals(SpeedUnit.KMH) ? 1.852 : 1);
			System.out.println(String.format("Bottom-Left to top-right: %.03f %s", distBLTR, (unitToUse.equals(SpeedUnit.KMH) ? "km" : "nm")));
			System.out.println(String.format("Top-Left to bottom-right: %.03f %s", distTLBR, (unitToUse.equals(SpeedUnit.KMH) ? "km" : "nm")));

			// Maps
			if (verbose) {
				System.out.println("Valid Strings:");
				validStrings.keySet().stream().forEach(key -> System.out.println(String.format("%s : %d elements", key, validStrings.get(key))));
				System.out.println("Invalid Strings:");
				invalidStrings.keySet().stream().forEach(key -> System.out.println(String.format("%s : %d elements", key, invalidStrings.get(key))));
			}
			try {
				// A Map on a canvas?
				SwingFrame frame = new SwingFrame(positions);
				frame.setVisible(true);
				frame.plot();
			} catch (HeadlessException he) {
				System.out.println("Headless Exception. Try in a graphical environment to visualize the data.");
			}
		} catch (IOException ioe) {
			System.err.println("From " + System.getProperty("user.dir"));
			ioe.printStackTrace();
		}
	}
}
