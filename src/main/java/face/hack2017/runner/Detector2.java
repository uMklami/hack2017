package face.hack2017.runner;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import face.hack2017.Recognition;

public class Detector2 {
	static String name;
	static String mood;
	private static Logger logger = Logger.getLogger(Recognizer.class.getName());
	
	public Detector2(){
		
	}
	
	public static void getDetect(String imagepath) throws IOException {
		final String api = "https://dev.sighthoundapi.com/v1/detections?type=face,person&faceOption=landmark,gender";
		  final String accessToken = "uY5kIX3zmFp4pqeUxVeCbgh6IF0HJoHCnpfI";
		  
		File outFolder = new File("Dectector_out");
		outFolder.mkdir();

		try {
			String imagedata = imageToBase64(new File(imagepath));
		    JsonObject jsonImage = Json.createObjectBuilder().add("image", imagedata).build();
		    URL apiURL = new URL(api);
		    HttpURLConnection connection = (HttpURLConnection) apiURL.openConnection();
		    connection.setRequestProperty("Content-Type", "application/json");
		    connection.setRequestProperty("X-Access-Token", accessToken);
		    
			connection.setRequestMethod("POST");
			
		    connection.setDoInput(true);
		    connection.setDoOutput(true);
		    byte[] body = jsonImage.toString().getBytes();
		    connection.setFixedLengthStreamingMode(body.length);
		    OutputStream os = connection.getOutputStream();
		    os.write(body);
		    os.flush();
		    int httpCode = connection.getResponseCode();
		    
		    if ( httpCode == 200 ){
		        JsonReader jReader = Json.createReader(connection.getInputStream());
		        JsonObject jsonBody = jReader.readObject();
		        getAnnotateImage(outFolder, new File(imagepath),
		        		jsonBody.getJsonArray("objects"));
		    } else {
		        JsonReader jReader = Json.createReader(connection.getErrorStream());
		        JsonObject jsonError = jReader.readObject();
		        System.out.println(jsonError);
		    }
		  } catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		  } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		  }
		
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
				JsonArray vertices = faceAnnotation.getJsonObject("boundingBox")
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
				mood = object.getString("emotion");
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
				g.drawString(mood, x, y + 56);
			}
			ImageIO.write(imageBuffer, "JPG", new File(outFolder
					+ File.separator + image.getName()));
		}
	}
	
	@SuppressWarnings("resource")
	private static String imageToBase64(File file){
        String encodedfile = null;
        try {
            FileInputStream fileInputStreamReader = new FileInputStream(file);
            byte[] bytes = new byte[(int)file.length()];
            fileInputStreamReader.read(bytes);
            encodedfile = Base64.getEncoder().encodeToString(bytes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return encodedfile;
    }
	
	public static void main(String[] args) throws IOException {
		  getDetect("Screenshot.jpg");
	  }
}
