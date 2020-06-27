package phonekeyboard3x4;

import java.text.NumberFormat;

public class SampleMain {
	public static void main(String... args) {
		// For Tests
		System.setProperty("keypad.rows", "GPIO_1,GPIO_4,GPIO_5,GPIO_6");
		System.setProperty("keypad.cols", "GPIO_7,GPIO_0, GPIO_3");

		KeyboardController kbc = new KeyboardController(true);

		System.out.println("------------------------------");
		System.out.println("Hit the same key twice to exit");
		System.out.println("------------------------------");

		char prevchar = ' ';
		boolean go = true;
		while (go) {
			char c = kbc.getKey();
			System.out.println("At " + NumberFormat.getInstance().format(System.currentTimeMillis()) + ", Char: [" + c + "]");
			if (c == prevchar) {
				go = false;
			}
			prevchar = c;
			try {
				Thread.sleep(200L);
			} catch (Exception ex) {
			}
		}
		System.out.println("Bye");
		kbc.shutdown();
	}
}
