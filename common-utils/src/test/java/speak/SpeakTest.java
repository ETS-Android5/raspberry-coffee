package speak;

import org.junit.Test;
import utils.TextToSpeech;

/*
 * Run with  ../gradlew test --tests "SpeakTest"
 */

public class SpeakTest {
    public static void main(String... args) {
        System.out.println("OS is [" + System.getProperty("os.name") + "]");
//	speak("You got a message from 415-745-5209. Do you wan to read it?");
        TextToSpeech.speak("Oh hello Pussycat, what's you doing up there?");
    }

    @Test
    public void speakToMe() {
        SpeakTest.main();
    }
}
