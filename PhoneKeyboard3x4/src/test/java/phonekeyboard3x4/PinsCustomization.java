package phonekeyboard3x4;

import org.junit.Test;

import java.security.InvalidParameterException;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertTrue;

public class PinsCustomization {

	@Test
	public void badOne() {

		System.setProperty("keypad.rows", "");
		try {
			new KeyboardController();
		} catch (Exception ex) {
			assertTrue(ex instanceof InvalidParameterException);
			System.out.println(ex.getMessage());
			assertTrue(ex.getMessage().startsWith("Please provide both "));
			System.out.println(String.format("As expected [%s]", ex.toString()));
		}
	}

	@Test
	public void badTwo() {

		System.setProperty("keypad.rows", "");
		System.setProperty("keypad.cols", "");
		try {
			new KeyboardController();
		} catch (Exception ex) {
			assertTrue(ex instanceof InvalidParameterException);
			System.out.println(ex.getMessage());
			assertTrue(ex.getMessage().equals("keypad.rows should contain 4 elements, comma-separated."));
			System.out.println(String.format("As expected [%s]", ex.toString()));
		}
	}

	@Test
	public void badThree() {

		System.setProperty("keypad.rows", "A,B,C,D");
		System.setProperty("keypad.cols", "A,E,F");
		try {
			new KeyboardController();
		} catch (Exception ex) {
			assertTrue(ex instanceof InvalidParameterException);
			System.out.println(ex.getMessage());
			assertTrue(ex.getMessage().contains("cannot appear more than once"));
			System.out.println(String.format("As expected [%s]", ex.toString()));
		}
	}

	@Test
	public void badFour() {

		System.setProperty("keypad.rows", "A,B,C,D");
		System.setProperty("keypad.cols", "E,F,G");
		try {
			new KeyboardController();
		} catch (Exception ex) {
			assertTrue(ex instanceof InvalidParameterException);
			System.out.println(ex.getMessage());
			assertTrue(ex.getMessage().startsWith("Unknown row pin name "));
			System.out.println(String.format("As expected [%s]", ex.toString()));
		}
	}

	@Test
	public void goodFive() {

		System.setProperty("keypad.rows", "GPIO_1,GPIO_4,GPIO_5,GPIO_6");
		System.setProperty("keypad.cols", "GPIO_7,GPIO_0, GPIO_3");
		try {
			new KeyboardController();
		} catch (Exception ex) {
			fail(String.format("This should have worked [%s]", ex.toString()));
		}
	}
}
