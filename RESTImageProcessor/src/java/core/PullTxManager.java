package core;

import images.ImageHTTPClient;
import images.ImageUtil;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class PullTxManager {

	public static String downloadAndTransform(
			String urlIn,
			String locationOut,
			String finalLocation,
			Color toMakeTransparent,
			Color changeThis,
			Color intoThat,
			String imgType,
			int tx)
		throws Exception {
		// Create output directory if they don't exist
		String outputDir = locationOut.substring(0, locationOut.lastIndexOf(File.separator));
		File dir = new File(outputDir);
		if (!dir.exists()) {
			boolean ok = dir.mkdirs();
			System.out.println(String.format("Directory(ies) %s created:", outputDir) + ok);
		}
		outputDir = finalLocation.substring(0, finalLocation.lastIndexOf(File.separator));
		dir = new File(outputDir);
		if (!dir.exists()) {
			boolean ok = dir.mkdirs();
			System.out.println(String.format("Directory(ies) %s created:", outputDir) + ok);
		}

		Image fax = ImageHTTPClient.getFax(urlIn, locationOut);
		BufferedImage bimg = ImageUtil.toBufferedImage(fax);;
		if (changeThis == null && intoThat == null) {
			bimg = ImageUtil.makeColorTransparent(bimg, toMakeTransparent, tx);
		} else {
			bimg = ImageUtil.switchColorAndMakeColorTransparent(bimg, changeThis, intoThat, toMakeTransparent, tx);
		}
		ImageUtil.writeImageToFile(bimg, imgType, finalLocation);
		return finalLocation;
	}

	public enum ImageColor {

		WHITE("white", Color.white),
		BLACK("black", Color.black),
		RED("red", Color.red),
		BLUE("blue", Color.blue),
		GREEN("green", Color.green),
		PINK("white", Color.pink);

		private final Color color;
		private final String colorName;

		ImageColor(String name, Color color) {
			this.color = color;
			this.colorName = name;
		}

		public String colorName() {
			return this.colorName;
		}
		public Color color() {
			return this.color;
		}
	}

	public enum TxType {
		BLUR(ImageUtil.BLUR, "blur"),
		SHARP(ImageUtil.SHARPEN, "sharpen"),
		NONE(ImageUtil.NO_CHANGE, "none");

		private final int type;
		private final String txName;

		TxType(int type, String txName) {
			this.type = type;
			this.txName = txName;
		}

		public int type() {
			return this.type;
		}
		public String txName() {
			return this.txName;
		}
	}

	public static class TxRequest {
		String url;
		String storage;
		String returned;
		ImageColor transparent;
		ImageColor from;
		ImageColor to;
		String imgType;
		TxType tx;

		public TxRequest url(String url) {
			this.url = url;
			return this;
		}
		public TxRequest storage(String storage) {
			this.storage = storage;
			return this;
		}
		public TxRequest returned(String returned) {
			this.returned = returned;
			return this;
		}
		public TxRequest imgType(String imgType) {
			this.imgType = imgType;
			return this;
		}
		public TxRequest transparent(ImageColor transparent) {
			this.transparent = transparent;
			return this;
		}
		public TxRequest from(ImageColor from) {
			this.from = from;
			return this;
		}
		public TxRequest to(ImageColor to) {
			this.to = to;
			return this;
		}
		public TxRequest tx(TxType tx) {
			this.tx = tx;
			return this;
		}

		public String getUrl() {
			return url;
		}

		public String getStorage() {
			return storage;
		}

		public String getReturned() {
			return returned;
		}

		public ImageColor getTransparent() {
			return transparent;
		}

		public ImageColor getFrom() {
			return from;
		}

		public ImageColor getTo() {
			return to;
		}

		public TxType getTx() {
			return tx;
		}

		public String getImgType() {
			return imgType;
		}
	}

	/**
	 * For tests.
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(String... args)
			throws Exception {
		final String FAX_NAME_1 = "http://www.opc.ncep.noaa.gov/P_sfc_full_ocean.gif";  // Surface, current
		final String FAX_NAME_2 = "http://www.opc.ncep.noaa.gov/shtml/P_06hr500bw.gif"; // 500mb, current
		final String FAX_NAME_3 = "http://www.prh.noaa.gov/hnl/graphics/stream.gif";    // Streamlines

		final String IMG_NAME_1 = "NOAA_sfc_1.png";
		final String IMG_NAME_2 = "NOAA_500_2.png";
		final String IMG_NAME_3 = "NOAA_Stream_2.png";

		String loc1 = downloadAndTransform(
				FAX_NAME_1,
				"web" + File.separator + IMG_NAME_1,
				"web" + File.separator + "_" + IMG_NAME_1,
				Color.white,
				Color.black,
				Color.blue,
				"png",
				ImageUtil.BLUR);

		String loc2 = downloadAndTransform(
				FAX_NAME_2,
				"web" + File.separator + IMG_NAME_2,
				"web" + File.separator + "_" + IMG_NAME_2,
				Color.white,
				Color.black,
				Color.red,
				"png",
				ImageUtil.BLUR);

		String loc3 = downloadAndTransform(
				FAX_NAME_3,
				"web" + File.separator + IMG_NAME_3,
				"web" + File.separator + "_" + IMG_NAME_3,
				Color.white,
				null,
				null,
				"png",
				ImageUtil.BLUR);


		// Transform template
		BufferedReader br = new BufferedReader(new FileReader("web" + File.separator + "template.html"));
		BufferedWriter bw = new BufferedWriter(new FileWriter("web" + File.separator + "index.html"));
		String line = "";
		while (line != null) {
			line = br.readLine();
			if (line != null) {
				if (line.contains("<FAX_1>")) {
					line = line.replace("<FAX_1>", "_" + IMG_NAME_1);
				}
				if (line.contains("<FAX_2>")) {
					line = line.replace("<FAX_2>", "_" + IMG_NAME_2);
				}
				if (line.contains("<FAX_3>")) {
					line = line.replace("<FAX_3>", "_" + IMG_NAME_3);
				}
				bw.write(line + "\n");
			}
		}
		br.close();
		bw.close();

		Desktop.getDesktop().browse(new File("./web/index.html").toURI());
	}
}
