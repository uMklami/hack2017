package face.hack2017.runner;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.speech.*;

import com.sun.speech.freetts.Voice;

import java.util.*;

import javax.speech.synthesis.*; 

import com.sun.speech.freetts.VoiceManager;

import face.hack2017.Recognition;

public class Recognizer {
	static String name;
	static String mood;
	private static Logger logger = Logger.getLogger(Recognizer.class.getName());

	public Recognizer() {
	}

	public static void DisplayImage(File file) throws IOException {
		try {
			BufferedImage img = ImageIO.read(file);
			ImageIcon icon = new ImageIcon(img);
			JFrame frame = new JFrame();
			frame.setLayout(new FlowLayout());
			frame.setSize(500, 700);
			JLabel lbl = new JLabel();
			lbl.setIcon(icon);
			frame.add(lbl);
			frame.setVisible(true);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static void getRecognize(File image) throws IOException {
		final String api = Recognition.BASE_URL + "recognition?groupId="
				+ URLEncoder.encode("friends", "UTF-8");
		File outFolder = new File("out");
		outFolder.mkdir();
		// File returnfile = null;

		if (image.isFile() && !image.isHidden()) {
			final byte[] data = Files.readAllBytes(Paths.get(image
					.getCanonicalPath()));
			JsonObject result = Recognition.httpCall(api, "POST",
					Recognition.contentTypeStream, data);
			if (result != null) {
				getAnnotateImage(outFolder, image,
						result.getJsonArray("objects"));
			}
			// returnfile = new File(outFolder + "/out/" + image + ".jpg");
			// DisplayImage(new File(outFolder + "/out/" + image + ".jpg"));

		}
		// return returnfile;
	}

	public static void getAnnotateImage(File outFolder, File image,
			JsonArray objects) throws IOException {
		if (outFolder.isDirectory() && image.isFile() && objects != null
				&& objects.size() > 0) {
			BufferedImage imageBuffer = ImageIO.read(image);
			Graphics2D g = imageBuffer.createGraphics();// .getGraphics();
			g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND,
					BasicStroke.JOIN_ROUND));
			g.setFont(new Font("Courier", Font.BOLD, 20));
			for (int oi = 0; oi < objects.size(); oi++) {
				JsonObject object = objects.getJsonObject(oi);
				JsonObject faceAnnotation = object
						.getJsonObject("faceAnnotation");
				JsonArray vertices = faceAnnotation.getJsonObject("bounding")
						.getJsonArray("vertices");
				double confidence = faceAnnotation.getJsonNumber(
						"recognitionConfidence").doubleValue();
				int nPoints = vertices.size();
				int[] xPoints = new int[nPoints];
				int[] yPoints = new int[nPoints];
				for (int ni = 0; ni < nPoints; ni++) {
					JsonObject point = vertices.getJsonObject(ni);
					xPoints[ni] = point.getInt("x");
					yPoints[ni] = point.getInt("y");
				}
				name = object.getString("objectId");

				if (confidence < 0.5) {
					name = "Unknown";
					g.setColor(Color.YELLOW);
					logger.info("An 'Unknown' person was found since recognition "
							+ "confidence "
							+ confidence
							+ " is below the minimum threshold of " + 0.5);
				} else {
					g.setColor(Color.decode("#73c7f1"));
					logger.info("Recognized " + name + " with confidence "
							+ confidence);
				}
				g.drawPolygon(xPoints, yPoints, nPoints);
				int x = xPoints[nPoints - 1];
				int y = yPoints[nPoints - 1];
				g.drawString(name, x, y + 16);
				g.drawString(String.valueOf(confidence), x, y + 36);
			}
			ImageIO.write(imageBuffer, "JPG", new File(outFolder
					+ File.separator + image.getName()));
			Text2Speach.dospeak("Hi "+name+ "how are you doing?", "kevin16");
		}
	}
}
