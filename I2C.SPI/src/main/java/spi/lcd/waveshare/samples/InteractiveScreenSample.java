package spi.lcd.waveshare.samples;

import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import spi.lcd.waveshare.LCD1in3;
import spi.lcd.waveshare.fonts.Font;
import spi.lcd.waveshare.fonts.Font24;
import utils.TimeUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

import static spi.lcd.waveshare.LCD1in3.DrawFill;

public class InteractiveScreenSample {

	private final static SimpleDateFormat SDF_1 = new SimpleDateFormat("E dd MMM yyyy");
	private final static SimpleDateFormat SDF_2 = new SimpleDateFormat("HH:mm:ss Z");
	private final static SimpleDateFormat SDF_3 = new SimpleDateFormat("HH:mm:ss z");

	private final static SimpleDateFormat SDF_HH = new SimpleDateFormat("HH");
	private final static SimpleDateFormat SDF_MM = new SimpleDateFormat("mm");
	private final static SimpleDateFormat SDF_SS = new SimpleDateFormat("ss");

	private static int currentIndex = 0; // Screen index, incremented/decremented with the buttons K1 (up) & K3 (down)

	private static boolean k1 = false, k2 = false, k3 = false, jUp = false, jDown = false, jRight = false, jLeft = false, jPressed = false;
	private static Consumer<GpioPinDigitalStateChangeEvent> key1Consumer = (event) -> {
		k1 = event.getState().isLow();
		if (k1) { // K1 is pushed down
			currentIndex++;
		}
	};
	private static Consumer<GpioPinDigitalStateChangeEvent> key2Consumer = (event) -> k2 = event.getState().isLow();
	private static Consumer<GpioPinDigitalStateChangeEvent> key3Consumer = (event) -> {
		k3 = event.getState().isLow();
		if (k3) { // K3 is pushed down
			currentIndex--;
		}
	};

	private static Consumer<GpioPinDigitalStateChangeEvent> jUpConsumer = (event) -> jUp = event.getState().isLow();
	private static Consumer<GpioPinDigitalStateChangeEvent> jDownConsumer = (event) -> jDown = event.getState().isLow();
	private static Consumer<GpioPinDigitalStateChangeEvent> jLeftConsumer = (event) -> jLeft = event.getState().isLow();
	private static Consumer<GpioPinDigitalStateChangeEvent> jRightConsumer = (event) -> jRight = event.getState().isLow();
	private static Consumer<GpioPinDigitalStateChangeEvent> jPressedConsumer = (event) -> jPressed = event.getState().isLow();

	private static boolean keepLooping = true;

	public static void main(String... args) {

		LCD1in3 lcd = new LCD1in3(LCD1in3.HORIZONTAL, LCD1in3.BLACK);
		// Key listeners
		lcd.setKey1Consumer(key1Consumer);
		lcd.setKey2Consumer(key2Consumer);
		lcd.setKey3Consumer(key3Consumer);
		lcd.setJUpConsumer(jUpConsumer);
		lcd.setJDownConsumer(jDownConsumer);
		lcd.setJLeftConsumer(jLeftConsumer);
		lcd.setJRightConsumer(jRightConsumer);
		lcd.setJPressedConsumer(jPressedConsumer);

		lcd.GUINewImage(LCD1in3.IMAGE_RGB, LCD1in3.LCD_WIDTH, LCD1in3.LCD_HEIGHT, LCD1in3.IMAGE_ROTATE_0, LCD1in3.IMAGE_COLOR_POSITIVE);

		// Display data, character mode
		if (!lcd.isSimulating()) {
			lcd.LCDClear(LCD1in3.BLACK);
			lcd.GUIClear(LCD1in3.BLACK);
		}

		// Double frame
		lcd.GUIDrawRectangle(2, 2, 238, 238, LCD1in3.YELLOW, DrawFill.DRAW_FILL_EMPTY, LCD1in3.DotPixel.DOT_PIXEL_1X1);
		lcd.GUIDrawRectangle(4, 4, 236, 236, LCD1in3.YELLOW, DrawFill.DRAW_FILL_EMPTY, LCD1in3.DotPixel.DOT_PIXEL_1X1);

		final int fontSize = 24;
		Font font = LCD1in3.findFontBySize(fontSize);
		int y = 8; // Top of the line

		String title = "Screen #1";
		int len = font.strlen(title);
		int lineStart = (LCD1in3.LCD_WIDTH / 2) - (len / 2);
		lcd.GUIDrawString(lineStart, y, title, font, LCD1in3.BLACK, LCD1in3.YELLOW);
		y += fontSize;
		lcd.GUIDrawString(8, y, "N  37 44.93'", font, LCD1in3.BLACK, LCD1in3.YELLOW);
		y += fontSize;
		lcd.GUIDrawString(8, y, "W 122 30.42'", font, LCD1in3.BLACK, LCD1in3.YELLOW);
		y += fontSize;

		int date1 = 0, date2 = 0, indexPos = 0; // For refresh
		Date date = new Date();
		lcd.GUIDrawString(8, y, SDF_1.format(date), font, LCD1in3.BLACK, LCD1in3.YELLOW);
		date1 = y;
		y += fontSize;
		lcd.GUIDrawString(8, y, SDF_3.format(date), font, LCD1in3.BLACK, LCD1in3.YELLOW);
		date2 = y;
		y += fontSize;
		lcd.GUIDrawString(8, y, String.format("Index: %d", currentIndex), font, LCD1in3.BLACK, LCD1in3.YELLOW);
		indexPos = y;
		// y += fontSize;

		if (!lcd.isSimulating()) {
			lcd.LCDDisplay();
		}

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("Ctlr+C !");
			keepLooping = false;
			TimeUtil.delay(5_000);// Wait for the screen to shut off
		}));

		// Display Data loop
		while (keepLooping) {
			TimeUtil.delay(10);

			Date now = new Date();
			lcd.GUIDrawString(8, date1, SDF_1.format(now), font, LCD1in3.BLACK, LCD1in3.RED);
			lcd.GUIDrawString(8, date2, SDF_3.format(now), font, LCD1in3.BLACK, LCD1in3.RED);
			lcd.GUIDrawString(8, indexPos, String.format("Index: %d  ", currentIndex), font, LCD1in3.BLACK, LCD1in3.GREEN);

			lcd.LCDDisplayWindows(8, date1, 235, date1 + (3 * fontSize));
		}
		System.out.println("End of loop");

		if (!lcd.isSimulating()) {
			lcd.LCDClear(LCD1in3.BLACK);
			lcd.GUIClear(LCD1in3.BLACK);
			String bye = "Bye!";
			Font f24 = Font24.getInstance();
			int strlen = f24.getWidth() * bye.length();
			int strX = 120 - (strlen / 2);
			int strY = 120 - (f24.getHeight() / 2);

			int bg = LCD1in3.rgb(200, 200, 200);
			lcd.GUIDrawCircle(120, 120, (int)((strlen * 1.5) / 2), bg, DrawFill.DRAW_FILL_FULL, LCD1in3.DotPixel.DOT_PIXEL_1X1);
			lcd.GUIDrawString(strX, strY, bye, f24, bg, LCD1in3.BLACK);
			lcd.LCDDisplay();
			TimeUtil.delay(1_000);

			lcd.LCDClear(LCD1in3.BLACK);
			lcd.GUIClear(LCD1in3.BLACK);
			lcd.LCDDisplay();

//		TimeUtil.delay(1_000);
			lcd.shutdown();
		}
		System.out.println("End of Sample");
		System.out.println("Bye.");
	}
}
