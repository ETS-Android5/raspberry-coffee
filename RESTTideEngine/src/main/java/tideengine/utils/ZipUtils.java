package tideengine.utils;

import org.xml.sax.InputSource;
import tideengine.BackEndXMLTideComputer;

import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtils {
    public static InputStream getZipInputStream(String zipStream, String entryName) throws Exception {
        ZipInputStream zip = new ZipInputStream(BackEndXMLTideComputer.class.getResourceAsStream(zipStream));
        InputStream is = null;
        boolean go = true;
        while (go) {
            ZipEntry ze = zip.getNextEntry();
            if (ze == null) {
                go = false;
            } else {
                if (ze.getName().equals(entryName)) {
                    is = zip;
                    go = false;
                }
            }
        }
        if (is == null) {
            throw new RuntimeException("Entry " + entryName + " not found in " + zipStream.toString());
        }
        return is;
    }

    public static InputSource getZipInputSource(String zipStream, String entryName) throws Exception {
        ZipInputStream zip = new ZipInputStream(BackEndXMLTideComputer.class.getResourceAsStream(zipStream));
        InputSource is = null;
        boolean go = true;
        while (go) {
            ZipEntry ze = zip.getNextEntry();
            if (ze == null) {
                go = false;
            } else {
                if (ze.getName().equals(entryName)) {
                    is = new InputSource(zip);
                    is.setEncoding("ISO-8859-1");
                    go = false;
                }
            }
        }
        if (is == null) {
            throw new RuntimeException("Entry " + entryName + " not found in " + zipStream.toString());
        }
        return is;
    }

}
