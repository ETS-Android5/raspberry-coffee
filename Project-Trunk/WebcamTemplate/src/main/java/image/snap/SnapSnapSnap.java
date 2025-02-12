package image.snap;

import image.server.SnapshotServer;
import utils.TimeUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * -Dsnap.verbose
 * -Dsnapshot.command=RASPISTILL|FSWEBCAM
 */
public class SnapSnapSnap extends Thread {

	private final static SimpleDateFormat DURATION_FMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	static {
		DURATION_FMT.setTimeZone(TimeZone.getTimeZone("etc/UTC"));
	}

	public static class SnapConfig {
		private int rot = 0;
		private int width = 640;
		private int height = 480;
		private long wait = 1_000L;
		private String snapName = "snap.jpg";
		private boolean timeBasedSnapName = false;

		public SnapConfig() {
		}

		public int getRot() {
			return rot;
		}

		public void setRot(int rot) {
			this.rot = rot;
		}

		public int getWidth() {
			return width;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public int getHeight() {
			return height;
		}

		public void setHeight(int height) {
			this.height = height;
		}

		public long getWait() {
			return wait;
		}

		public void setWait(long wait) {
			this.wait = wait;
		}

		public String getSnapName() {
			return snapName;
		}

		public void setSnapName(String snapName) {
			this.snapName = snapName;
		}

		public void setTimeBasedSnapName(boolean b) {
			this.timeBasedSnapName = b;
		}

		public boolean isTimeBasedSnapName() {
			return this.timeBasedSnapName;
		}

		@Override
		public String toString() {
			return String.format("Rot: %d, Width: %d, Height: %d, Wait: %d, Name: %s, time-based: %d",
					this.rot, this.width, this.height, this.wait, this.snapName, this.timeBasedSnapName);
		}
	}
	private SnapConfig config = new SnapConfig();

	private static NumberFormat nf = NumberFormat.getInstance();

	public static class SnapStatus {
		private int rot = 0;
		private int width = 640;
		private int height = 480;
		private long wait = 1_000L;
		private String snapName = "snap.jpg";
		private boolean timeBaseSnapName = false;
		private boolean threadRunning = false;
		private String state = "";

		public String getState() {
			return state;
		}

		public void setState(String state) {
			this.state = state;
		}

		public SnapStatus() {
		}

		public int getRot() {
			return rot;
		}

		public void setRot(int rot) {
			this.rot = rot;
		}

		public int getWidth() {
			return width;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public int getHeight() {
			return height;
		}

		public void setHeight(int height) {
			this.height = height;
		}

		public long getWait() {
			return wait;
		}

		public void setWait(long wait) {
			this.wait = wait;
		}

		public String getSnapName() {
			return snapName;
		}

		public void setSnapName(String snapName) {
			this.snapName = snapName;
		}

		public boolean isThreadRunning() {
			return threadRunning;
		}

		public void setThreadRunning(boolean threadRunning) {
			this.threadRunning = threadRunning;
		}

		public boolean isTimeBaseSnapName() {
			return timeBaseSnapName;
		}
		public void setTimeBaseSnapName(boolean b) {
//			System.out.println(">>> Setting timeBasedSnapName to " + b);
//			Throwable whoCalledMe = new Throwable();
//			whoCalledMe.printStackTrace();
//			System.out.println(">>> ---------------------------------");
			this.timeBaseSnapName = b;
		}
	}

	public SnapConfig getConfig() {
		return config;
	}

	public void setConfig(SnapConfig config) {
		this.config = config;
	}

	public String getSnapName() {
		return config.snapName;
	}

	public void setSnapName(String snapName) {
		this.config.snapName = snapName;
	}

	public int getRot() {
		return config.rot;
	}

	public void setRot(int rot) {
		this.config.rot = rot;
	}

	public int getWidth() {
		return config.width;
	}

	public void setWidth(int width) {
		this.config.width = width;
	}

	public int getHeight() {
		return config.height;
	}

	public void setHeight(int height) {
		this.config.height = height;
	}

	public long getWait() {
		return config.wait;
	}

	public void setWait(long wait) {
		this.config.wait = wait;
	}

	public void setKeepSnapping(boolean keepSnapping) {
		this.keepSnapping = keepSnapping;
	}

	public enum SnapshotOptions {
		// The --timeout seem to degrade the quality of the picture, specially outside...
		RASPISTILL("raspistill --rotation %d --width %d --height %d  %s --output %s --nopreview"), // --timeout 1
		// For a webcam (and also for the RPi Camera)
		// Requires sudo apt-get install fswebcam
		// See http://www.raspberrypi.org/documentation/usage/webcams/ for some doc.
		// and fswebcam --help
		// Default device is /dev/video0. Use `additionalArguments` to modify it, with a content like '--device /dev/video1'
		FSWEBCAM("fswebcam --rotate %d --resolution %dx%d --no-banner %s %s"); // 1280x720 works good.

		private final String command;

		SnapshotOptions(String command) {
			this.command = command;
		}

		public String command() {
			return this.command;
		}
	}
	private static SnapshotOptions snapshotCommandOption = SnapshotOptions.RASPISTILL; // Default
	private static String additionalArguments = "";
	private boolean timeBasedSnapshotName = false;

	private SnapshotServer parent;


	// Slow motion (fswebcam also has the feature)
	private final static String SNAPSHOT_COMMAND_3 = "raspivid -w 640 -h 480 -fps 90 -t 30000 -o vid.h264";

	public SnapStatus getSnapStatus() {
		SnapStatus snapStatus = new SnapStatus();
		snapStatus.setHeight(this.config.getHeight());
		snapStatus.setWidth(this.config.getWidth());
		snapStatus.setRot(this.config.getRot());
		snapStatus.setSnapName(this.config.getSnapName());
		snapStatus.setTimeBaseSnapName(this.config.isTimeBasedSnapName());
		snapStatus.setWait(this.config.getWait());
		snapStatus.setThreadRunning(this.isAlive());
		snapStatus.setState(this.getState().toString());
		return snapStatus;
	}

	public static String snap(String name, int rot, int width, int height)
			throws Exception {
		Runtime rt = Runtime.getRuntime();
		// NOTE: The web directory has to exist on the Raspberry Pi
		String snapshotName = name; // String.format("web/%s.jpg", name);
		try {
			String command = String.format(snapshotCommandOption.command(), rot, width, height, additionalArguments, snapshotName);
			if ("true".equals(System.getProperty("snap.verbose", "false"))) {
				System.out.println("Running command:" + command);
			}
			long before = System.currentTimeMillis();
			Process snap = rt.exec(command);
			final int exitValue = snap.waitFor(); // Sync
			long after = System.currentTimeMillis();
			if (exitValue != 0) {
				System.err.println("Failed to execute the following command: " + command + " due to the following error(s):");
				try (final BufferedReader b = new BufferedReader(new InputStreamReader(snap.getErrorStream()))) {
					String line;
					if ((line = b.readLine()) != null) {
						System.err.println(line);
					}
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
			if ("true".equals(System.getProperty("snap.verbose", "false"))) {
				System.out.println(String.format("%s Executing [%s], snapshot is in %s, (%s ms)",
						SnapSnapSnap.class.getName(),
						command,
						snapshotName,
						nf.format(after - before)));
			}
		} catch (InterruptedException ie) {
			throw ie;
		} catch (IOException ioe) {
			throw ioe;
		}
		return snapshotName;
	}

	// Config in the setConfig and such.
	public SnapSnapSnap() {
		super();
	}
	public SnapSnapSnap(String threadName) {
		this(threadName, false, null);
	}
	public SnapSnapSnap(String threadName, boolean timeBasedImageName, SnapshotServer parent) {
		super(threadName);
		this.parent = parent;
		this.timeBasedSnapshotName = timeBasedImageName;
		this.config.setTimeBasedSnapName(this.timeBasedSnapshotName);
		String snapshotCommand = System.getProperty("snapshot.command", "RASPISTILL");
		Optional<SnapshotOptions> snapOpt = Arrays.asList(SnapshotOptions.values())
				.stream()
				.filter(snap -> snap.toString().equals(snapshotCommand))
				.findFirst();
		if (!snapOpt.isPresent()) {
			System.out.println(String.format("WARNING: Invalid snapshot option %s", snapshotCommand));
			System.out.println(String.format("Can only be one of:\n%s",
					Arrays.asList(SnapshotOptions.values()).stream()
							.map(Object::toString)
							.collect(Collectors.joining("\n"))));
		} else {
			snapshotCommandOption = snapOpt.get();
		}
		String radix = "additional.arguments.%d";
//      Expect variables like
//		-Dadditional.arguments.1=--device"
//		-Dadditional.arguments.2=/dev/video1"
//        etc...
		int aaRnk = 0;
		while (true) {
			aaRnk++;
			String oneAdditionalArguments = System.getProperty(String.format(radix, aaRnk));
			if (oneAdditionalArguments != null) {
				additionalArguments += String.format("%s ", oneAdditionalArguments);
			} else {
				break;
			}
		}
	}

	private boolean keepSnapping = true;

	public void stopSnapping() {
		this.keepSnapping = false;
	}

	@Override
	public void run() {
		if ("true".equals(System.getProperty("snap.verbose", "false"))) {
			System.out.println(String.format(">>> In %s, starting thread (run)", this.getClass().getName()));
		}
		while (this.keepSnapping) {
			try {
				if (this.config.isTimeBasedSnapName()) {
					this.config.setSnapName(String.format("%s/snap-%s.jpg",
							SnapshotServer.stripSeparators(this.parent.getHTTPServer().getStaticDocumentsLocation().get(0)),
							DURATION_FMT.format(new Date())));
				}
				if ("true".equals(System.getProperty("snap.verbose", "false"))) {
					System.out.printf(">> Snapping %s.\n", this.config.getSnapName());
				}
				SnapSnapSnap.snap(this.config.getSnapName(), this.config.rot, this.config.width, this.config.height);
			} catch (Exception ex) {
				if ("true".equals(System.getProperty("snap.verbose", "false"))) {
					ex.printStackTrace();
				} else {
					System.err.println(ex.getMessage());
				}
			}
			// Wait...
			if ("true".equals(System.getProperty("snap.verbose", "false"))) {
				System.out.printf(">> Waiting for %d ms.\n", this.config.getWait());
			}
			TimeUtil.delay(this.config.getWait());
		}
	}

	public String getLastSnapshotName() {
		return this.config.getSnapName();
	}

}
