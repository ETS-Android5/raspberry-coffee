package sunflower;

import calc.DeadReckoning;
import calc.GeomUtil;
import calc.calculation.AstroComputer;
import com.pi4j.io.i2c.I2CFactory;
import i2c.motor.adafruitmotorhat.AdafruitMotorHAT;
import nmea.parser.GeoPos;
import utils.TimeUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static utils.TimeUtil.delay;

/*
 * See https://learn.adafruit.com/adafruit-dc-and-stepper-motor-hat-for-raspberry-pi/using-stepper-motors
 *
 * RPM : Revolution Per Minute, speed of the rotation.
 * Steps per Revolution: How many steps for 360 degrees.
 * nbSteps: How many steps should the shaft do when started.
 */
public class SunFlowerDriver {

	private SunFlowerDriver instance = this;

	private static GeoPos devicePosition = null; // Can be fed from a GPS, or manually (System variable).
	private static double sunAzimuth   = 180d;
	private static double sunElevation = -1d;

	private boolean simulating = false;

	private AdafruitMotorHAT mh;

	private AdafruitMotorHAT.AdafruitStepperMotor azimuthMotor;
	private AdafruitMotorHAT.AdafruitStepperMotor elevationMotor;

	private final static double PARKED_ELEVATION = 90d;
	private final static double PARKED_AZIMUTH   = 180d;

	private double currentDeviceElevation = PARKED_ELEVATION;
	private double currentDeviceAzimuth = PARKED_AZIMUTH;

	private CelestialComputerThread astroThread = null;
	private MotorThread elevationMotorThread = null;
	private MotorThread azimuthMotorThread = null;

	private AdafruitMotorHAT.Style motorStyle = AdafruitMotorHAT.Style.MICROSTEP;  // Default

	private static double azimuthMotorRatio   = 1d / 40d; // Set with System variable "azimuth.ratio"
	private static double elevationMotorRatio = 1d / 7.11111; // 18:128, Set with System variable "elevation.ratio"

	private boolean keepGoing = true;
	private final static int DEFAULT_RPM = 30;
	private final static int DEFAULT_STEPS_PER_REV = AdafruitMotorHAT.AdafruitStepperMotor.DEFAULT_NB_STEPS;

	private final static boolean MOTOR_HAT_VERBOSE = "true".equals(System.getProperty("motor.hat.verbose"));
	private final static boolean ASTRO_VERBOSE = "true".equals(System.getProperty("astro.verbose", "false"));

	public void setDevicePosition(double lat, double lng) {
		this.devicePosition = new GeoPos(lat, lng);
	}

	private class MotorThread extends Thread {
		private AdafruitMotorHAT.AdafruitStepperMotor stepper;
		private int nbSteps;
		private AdafruitMotorHAT.MotorCommand motorCommand;
		private AdafruitMotorHAT.Style motorStyle;

		MotorThread(AdafruitMotorHAT.AdafruitStepperMotor stepper,
		            int nbSteps,
		            AdafruitMotorHAT.MotorCommand motorCommand,
		            AdafruitMotorHAT.Style motorStyle) {
			this.stepper = stepper;
			this.nbSteps = nbSteps;
			this.motorCommand = motorCommand;
			this.motorStyle = motorStyle;
		}

	  @Override
		public void run() {
			try {
				long before = System.currentTimeMillis();
				this.stepper.step(nbSteps, motorCommand, motorStyle, t -> {
					if (MOTOR_HAT_VERBOSE) {
						// t.printStackTrace();
						System.out.println(String.format("\t\tToo long! %s", t));
					}
				});
				long after = System.currentTimeMillis();
				String mess = String.format("Move (%d steps) completed in %s",
						this.nbSteps,
						TimeUtil.fmtDHMS(TimeUtil.msToHMS(after - before)));
				instance.publish(this.stepper.getMotorNum() == 1 ? EventType.MOVING_AZIMUTH_END : EventType.MOVING_ELEVATION_END, mess);
				if (MOTOR_HAT_VERBOSE) {
					System.out.println(String.format("\t%s on motor #%d",
							mess,
							this.stepper.getMotorNum()));
				}
			} catch (IOException ie) {
				ie.printStackTrace();
			}
		}
	}

	private static class CelestialComputerThread extends Thread {
		private boolean keepCalculating = true;

		public void stopCalculating() {
			keepCalculating = false;
		}

		@Override
		public void run() {
			while (keepCalculating) {

				Date at = new Date();
				Calendar date = Calendar.getInstance(TimeZone.getTimeZone("Etc/UTC"));
				date.setTime(at);
//				if (ASTRO_VERBOSE) {
//					System.out.println("Starting Sun data calculation at " + date.getTime());
//				}
				// TODO Make it non-static, and synchronized ?
				AstroComputer.calculate(date.get(Calendar.YEAR),
																date.get(Calendar.MONTH) + 1,
																date.get(Calendar.DAY_OF_MONTH),
																date.get(Calendar.HOUR_OF_DAY), // and not HOUR !!!!
																date.get(Calendar.MINUTE),
																date.get(Calendar.SECOND));
				if (devicePosition != null) {
					DeadReckoning dr = new DeadReckoning(AstroComputer.getSunGHA(),
																							 AstroComputer.getSunDecl(),
																							 devicePosition.lat,
																							 devicePosition.lng)
														 .calculate();
					sunAzimuth = dr.getZ();
					sunElevation = dr.getHe();
					if (ASTRO_VERBOSE) {
						System.out.println(String.format("At %s, from %s, Z: %.02f, Elev: %.02f ", date.getTime(), devicePosition, sunAzimuth, sunElevation));
					}
				} else {
					System.out.println("No position yet!");
				}
				delay(1_000L);
			}
		}
	}

	public enum EventType {
		CELESTIAL_DATA,
		DEVICE_DATA,
		MOVING_ELEVATION_START,
		MOVING_AZIMUTH_START,
		MOVING_ELEVATION_START_2,
		MOVING_AZIMUTH_START_2,
		MOVING_ELEVATION_END,
		MOVING_AZIMUTH_END,
		MOVING_ELEVATION_INFO,
		MOVING_AZIMUTH_INFO,
		DEVICE_INFO
	}
	private static abstract class SunFlowerEventListener {
		public abstract void newMessage(EventType messageType, String messageContent);
	}

	private List<SunFlowerEventListener> listeners = new ArrayList<>();

	public void subscribe(SunFlowerEventListener listener) {
		listeners.add(listener);
	}
	public void unsubscribe(SunFlowerEventListener listener) {
		if (listeners.contains(listener)) {
			listeners.remove(listener);
		}
	}

	private void publish(EventType messageType, String messageContent) {
		listeners.forEach(listener -> listener.newMessage(messageType, messageContent));
	}

	private SunFlowerDriver() {

		System.out.println("Starting Program");
		int rpm = Integer.parseInt(System.getProperty("rpm", String.valueOf(DEFAULT_RPM))); // 30
		System.out.println(String.format("RPM set to %d.", rpm));

		try {
			this.mh = new AdafruitMotorHAT(DEFAULT_STEPS_PER_REV); // Default addr 0x60

			this.azimuthMotor = mh.getStepper(AdafruitMotorHAT.AdafruitStepperMotor.PORT_M1_M2);
			this.azimuthMotor.setSpeed(rpm); // Default 30 RPM

			this.elevationMotor = mh.getStepper(AdafruitMotorHAT.AdafruitStepperMotor.PORT_M3_M4);
			this.elevationMotor.setSpeed(rpm); // Default 30 RPM

		} catch (I2CFactory.UnsupportedBusNumberException ubne) {
			simulating = true;
		}
	}

	private final static class MotorPayload {
		AdafruitMotorHAT.MotorCommand motorCommand = AdafruitMotorHAT.MotorCommand.FORWARD;
		int nbSteps = 0;
	}

	private static MotorPayload getMotorPayload(double from, double to, double ratio) {
		MotorPayload motorPayload = new MotorPayload();
		motorPayload.motorCommand = (to > from) ? AdafruitMotorHAT.MotorCommand.FORWARD : AdafruitMotorHAT.MotorCommand.BACKWARD;
	  // Motor: 200 steps: 360 degrees.
		// Device: 360 degrees = (200 / ratio) steps.
		motorPayload.nbSteps = (int)Math.round((Math.abs(from - to) / 360d) * 200d / ratio);
		return motorPayload;
	}

	private void parkDevice() {
		if (currentDeviceElevation != PARKED_ELEVATION || currentDeviceAzimuth != PARKED_AZIMUTH) {
			this.publish(EventType.DEVICE_INFO, "Parking the device");
			// Put Z to 0, Elev. to 90.
			MotorPayload parkElev = getMotorPayload(currentDeviceElevation, PARKED_ELEVATION, elevationMotorRatio);
			System.out.println(String.format(">> (Elev) This will be %d steps %s", parkElev.nbSteps, parkElev.motorCommand));
			if (!simulating) {
				elevationMotorThread = new MotorThread(this.elevationMotor, parkElev.nbSteps, parkElev.motorCommand, motorStyle);
				elevationMotorThread.start();
			}
			currentDeviceElevation = PARKED_ELEVATION; // TODO In the thread?

			MotorPayload parkZ = getMotorPayload(currentDeviceAzimuth, PARKED_AZIMUTH, azimuthMotorRatio);
			System.out.println(String.format(">> (Z) This will be %d steps %s", parkZ.nbSteps, parkZ.motorCommand));
			if (!simulating) {
				azimuthMotorThread = new MotorThread(this.azimuthMotor, parkZ.nbSteps, parkZ.motorCommand, motorStyle);
				azimuthMotorThread.start();
			}
			currentDeviceAzimuth = PARKED_AZIMUTH; // TODO In the thread?
		} else {
			this.publish(EventType.DEVICE_INFO, "Parked");
			// System.out.println("Parked");
		}
	}
	private void go() {
		keepGoing = true;

		astroThread = new CelestialComputerThread();
		astroThread.start(); // Start calculating

		final double MIN_DIFF_FOR_MOVE = 0.5;

		while (keepGoing) {

			String deviceData = String.format("Azimuth: %.02f, Elevation: %.02f",
					currentDeviceAzimuth,
					currentDeviceElevation);
			String celestialData = String.format("Azimuth: %.02f, Elevation: %.02f",
					sunAzimuth,
					sunElevation);
			this.publish(EventType.DEVICE_DATA, deviceData);
			this.publish(EventType.CELESTIAL_DATA, celestialData);

			if (ASTRO_VERBOSE) {
				System.out.println(String.format("Device : %s\n" + "Sun : %s",
						deviceData, celestialData));
			}

			if (astroThread.isAlive() && sunElevation >= 0) {
				boolean hasMoved = false;
				if (Math.abs(currentDeviceAzimuth - sunAzimuth) >= MIN_DIFF_FOR_MOVE) { // Start a new thread each time a move is requested
					hasMoved = true;
					String mess1 = String.format("At %s, setting device from %.02f to %.02f degrees (a %.02f degrees move)", new Date(), currentDeviceAzimuth, sunAzimuth, Math.abs(currentDeviceAzimuth - sunAzimuth));
					this.publish(EventType.MOVING_AZIMUTH_START, mess1);
					MotorPayload data = getMotorPayload(currentDeviceAzimuth, sunAzimuth, azimuthMotorRatio);
					if (!simulating) {
						String mess2 = String.format("This will be %d steps %s on motor #%d", data.nbSteps, data.motorCommand, this.azimuthMotor.getMotorNum());
						this.publish(EventType.MOVING_AZIMUTH_START_2, mess2);
						if (azimuthMotorThread == null || (azimuthMotorThread != null && !azimuthMotorThread.isAlive())) {
							azimuthMotorThread = new MotorThread(this.azimuthMotor, data.nbSteps, data.motorCommand, motorStyle);
							azimuthMotorThread.start();
						} else {
							String mess3 = "Thread is already busy at work.";
							this.publish(EventType.MOVING_AZIMUTH_INFO, mess3);
						}
					}
					currentDeviceAzimuth = sunAzimuth; // TODO Do this in the thread?
				}
				if (Math.abs(currentDeviceElevation - sunElevation) >= MIN_DIFF_FOR_MOVE) {
					hasMoved = true;
					String mess1 = String.format("At %s, setting device from %.02f to %.02f degrees (a %.02f degrees move)", new Date(), currentDeviceElevation, sunElevation, Math.abs(currentDeviceElevation - sunElevation));
					this.publish(EventType.MOVING_ELEVATION_START, mess1);
					MotorPayload data = getMotorPayload(currentDeviceElevation, sunElevation, elevationMotorRatio);
					if (!simulating) {
						String mess2 = String.format("This will be %d steps %s on motor #%d", data.nbSteps, data.motorCommand, this.elevationMotor.getMotorNum());
						this.publish(EventType.MOVING_ELEVATION_START_2, mess2);
					}
					if (!simulating) {
						if (elevationMotorThread == null || (elevationMotorThread != null && !elevationMotorThread.isAlive())) {
							elevationMotorThread = new MotorThread(this.elevationMotor, data.nbSteps, data.motorCommand, motorStyle);
							elevationMotorThread.start();
						} else {
							String mess3 = "Thread is already busy at work.";
							this.publish(EventType.MOVING_ELEVATION_INFO, mess3);
						}
					}
					currentDeviceElevation = sunElevation; // TODO Do this in the thread?
				}
				if (hasMoved && ASTRO_VERBOSE) {
					System.out.println(String.format("Sun's position is now: Elev: %s, Z: %.02f", GeomUtil.decToSex(sunElevation, GeomUtil.NO_DEG, GeomUtil.NONE), sunAzimuth));
				}
			} else { // Park device
				parkDevice();
			}
			// Bottom of the loop
			delay(1_000L);
		}
		System.out.println("... Done with the program ...");
//	try { Thread.sleep(1_000); } catch (Exception ex) {} // Wait for the motors to be released.
	}

	private void stop() {
		this.keepGoing = false;
		// Park the device
		parkDevice();
		while ((azimuthMotorThread != null && azimuthMotorThread.isAlive()) || (elevationMotorThread != null && elevationMotorThread.isAlive())) {
			System.out.println("Waiting for the device to be parked");
			delay(1_000L);
		}
		if (mh != null) {
			try { // Release all
				mh.getMotor(AdafruitMotorHAT.Motor.M1).run(AdafruitMotorHAT.MotorCommand.RELEASE);
				mh.getMotor(AdafruitMotorHAT.Motor.M2).run(AdafruitMotorHAT.MotorCommand.RELEASE);
				mh.getMotor(AdafruitMotorHAT.Motor.M3).run(AdafruitMotorHAT.MotorCommand.RELEASE);
				mh.getMotor(AdafruitMotorHAT.Motor.M4).run(AdafruitMotorHAT.MotorCommand.RELEASE);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		if (astroThread != null) {
			astroThread.stopCalculating();
		}
	}

	/**
	 * System properties:
	 * rpm, default 30
	 * hat.debug, default false
	 * TODO: the others, deltaT and friends.
	 *
	 * @param args Not used
	 * @throws Exception if anything fails...
	 */
	public static void main(String... args) throws Exception {
		SunFlowerDriver sunFlowerDriver = new SunFlowerDriver();

		sunFlowerDriver.subscribe(new SunFlowerEventListener() {

			private EventType lastMessageType = null;

			@Override
			public void newMessage(EventType messageType, String messageContent) {
				// Basic, just an example, a verbose spit.
				if (messageType != lastMessageType) {
					if (messageType != EventType.DEVICE_INFO && 
							messageType != EventType.CELESTIAL_DATA &&
							messageType != EventType.DEVICE_DATA) {
						System.out.println(String.format("Listener: %s: %s", messageType, messageContent));
					}
					lastMessageType = messageType;
				}
			}
		});

		System.out.println("Hit Ctrl-C to stop the program");

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("\nShutting down, releasing resources.");
			sunFlowerDriver.stop();
			try { Thread.sleep(5_000); } catch (Exception absorbed) {
				System.err.println("Ctrl-C: Oops!");
				absorbed.printStackTrace();
			}
		}, "Shutdown Hook"));

		String strLat = System.getProperty("device.lat");
		String strLng = System.getProperty("device.lng");
		if (strLat != null && strLng != null) {
			try {
				double lat = Double.parseDouble(strLat);
				double lng = Double.parseDouble(strLng);
				sunFlowerDriver.setDevicePosition(lat, lng);
			} catch (NumberFormatException nfe) {
				nfe.printStackTrace();
			}
		}

		// Ratios:
		String zRatioStr = System.getProperty("azimuth.ratio");
		if (zRatioStr != null) {
			String[] zData = zRatioStr.split(":");
			if (zData.length != 2) {
				throw new IllegalArgumentException(String.format("Expecting a value like '1:234', not %s", zRatioStr));
			}
			try {
				double num = Double.parseDouble(zData[0]);
				double den = Double.parseDouble(zData[1]);
				azimuthMotorRatio = num / den;
			} catch (NumberFormatException nfe) {
				System.err.println("Bad value");
				throw nfe;
			}
		}
		String elevRatioStr = System.getProperty("elevation.ratio");
		if (elevRatioStr != null) {
			String[] elevData = elevRatioStr.split(":");
			if (elevData.length != 2) {
				throw new IllegalArgumentException(String.format("Expecting a value like '1:234', not %s", elevRatioStr));
			}
			try {
				double num = Double.parseDouble(elevData[0]);
				double den = Double.parseDouble(elevData[1]);
				elevationMotorRatio = num / den;
			} catch (NumberFormatException nfe) {
				System.err.println("Bad value");
				throw nfe;
			}
		}

		sunFlowerDriver.go();

		System.out.println("Bye!");
	}
}
