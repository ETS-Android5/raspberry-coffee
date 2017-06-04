package i2c.samples;

import com.pi4j.io.i2c.I2CFactory;
import i2c.sensor.LSM303;
import i2c.sensor.listener.LSM303Listener;
import i2c.sensor.listener.SensorLSM303Context;
import java.net.URI;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

/**
 * Reads the LSM303 sensor, and feeds a WebSocket server
 * A WegGL UI listens to the WS event, and displays a cube accordingly.
 */
public class LSM303HeelPitchWebGL {

	private static boolean verbose =  false;
	private static WebSocketClient webSocketClient = null;

	private static void initWebSocketConnection(String serverURI) {
		try {
			webSocketClient = new WebSocketClient(new URI(serverURI)) {
				@Override
				public void onOpen(ServerHandshake serverHandshake) {
					// TODO Implement this method
					System.out.println("Connected");
				}

				@Override
				public void onMessage(String string) {
					// TODO Implement this method
				}

				@Override
				public void onClose(int i, String string, boolean b) {
					// TODO Implement this method
				}

				@Override
				public void onError(Exception exception) {
					// TODO Implement this method
				}
			};
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void main(String... args) throws I2CFactory.UnsupportedBusNumberException {

		String wsUri = System.getProperty("ws.uri", "ws://localhost:9876/");
		initWebSocketConnection(wsUri);
		webSocketClient.connect();

		verbose = "true".equals(System.getProperty("lsm303.verbose", "false"));
		System.out.println("Verbose: " + verbose);
		LSM303 sensor = new LSM303();
		LSM303Listener dataListener = new LSM303Listener() {
			public void dataDetected(float accX, float accY, float accZ, float magX, float magY, float magZ, float heading, float pitch, float roll) {
				try {
					webSocketClient.send(String.format("{ \"heading\": %f, \"pitch\": %f, \"roll\": %f }", heading, pitch, roll));
					if (verbose) {
						System.out.println(String.format("Pitch:%d, Roll:%f", pitch, roll));
					}
				} catch (/* NotYetConnected */ Exception e) {
					System.err.println("Ooops:" + e.toString());
//			e.printStackTrace();
				}
			}
		};

		SensorLSM303Context.getInstance().addReaderListener(dataListener);
		sensor.setDataListener(dataListener);
		sensor.setWait(250L);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("\nBye.");
			webSocketClient.close();
			synchronized (sensor) {
				sensor.setKeepReading(false);
				try {
					Thread.sleep(500L);
				} catch (InterruptedException ie) {
					System.err.println(ie.getMessage());
				}
			}
		}));
		System.out.println("Starting listening...");
		sensor.startReading();
	}

}
