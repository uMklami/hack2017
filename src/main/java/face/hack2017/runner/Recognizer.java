package face.hack2017.runner;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import face.hack2017.Recognition;
import face.hack2017.models.Person;

public class Recognizer {
	static String mood;
	private static Logger logger = Logger.getLogger(Recognizer.class.getName());
	public static List<Person> persons = new DataUploader("data.txt"). getPeople() ;

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
		String name;
		List<String> object_names = new ArrayList<String>();
		List<String> object_likes = new ArrayList<String>();
		
		if (outFolder.isDirectory() && image.isFile() && objects != null
				&& objects.size() > 0) {
			BufferedImage imageBuffer = ImageIO.read(image);
			Graphics2D g = imageBuffer.createGraphics();// .getGraphics();
			g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND,
					BasicStroke.JOIN_ROUND));
			g.setFont(new Font("Courier", Font.BOLD, 20));
			
			for (int oi = 0; oi < objects.size(); oi++) {
				
				JsonObject object = objects.getJsonObject(oi);
				JsonObject faceAnnotation = object.getJsonObject("faceAnnotation");
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
				object_names.add(object.getString("objectId"));
				

				if (confidence < 0.5) {
					object_names.add("Unknown");
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
		
			String string = null;
			if(object_names.size() == 1){
				for(Person person: persons){
					if(person.getName().toLowerCase().trim().equals(object_names.get(0).toLowerCase().trim())){
						string = person.toString();
					}
				}
				Text2Speach.dospeak("Hi "+object_names.get(0)+ " how are you doing? Welcome home! "+string, "kevin16");
			}
			else if(object_names.size() == 2){
				Text2Speach.dospeak("Hi "+object_names.get(0)+" and "+ object_names.get(1)+ " how are you doing?", "kevin16");
			}
			else if(object_names.size() == 3){
				Text2Speach.dospeak("Hi "+object_names.get(0)+", "+ object_names.get(1)+ " and "+ object_names.get(2)+ " how are you doing?", "kevin16");
			}
			else {
				Text2Speach.dospeak("Hi Everybody how are you doing?", "kevin16");
			}
			
		}
	}
}
