package face.hack2017;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

public class Recognition {
	// TODO: Replace TOKEN with your own Sighthound Cloud Token
	public static final String TOKEN = "uY5kIX3zmFp4pqeUxVeCbgh6IF0HJoHCnpfI";
	public static final String BASE_URL = "https://dev.sighthoundapi.com/v1/";

	// Set minimum confidence threshold needed to have a positive recognition.
	// Any values below this number will be marked as 'Unknown' in the tutorial.
	public static final double recognitionConfidenceThreshold = 0.5;
	// contentType
	public static final String contentTypeStream = "application/octet-stream";
	private static final String contentTypeJson = "application/json";

	// image folder if different from default folder
	private static String imageFolder = null;
	// working folder if different from default folder
	public static String workingFolder = null;
	// java logging
	private static Logger logger = Logger.getLogger(Recognition.class.getName());

	// Create an array of the people we want to recognize. For this tutorial,
	// the person's name will be their Object ID, and it's also the folder name
	// containing their training images.
	private static final Set<File> peoples = new HashSet<File>();

	// Define a generic callback to be used for outputting responses and errors
	private static void genericCallback(boolean error, int statusCode, String body) {
		if (!error && (statusCode == 200 || statusCode == 204)) {
			logger.info(body);
		} else if (error) {
			logger.warning(statusCode + "\n" + body);
		} else {
			logger.info(statusCode + "\n" + body);
		}
	}

	public static JsonObject httpCall(String api, String method, String contentType, byte[] body) throws IOException {
		URL apiURL = new URL(api);
		HttpURLConnection connection = (HttpURLConnection) apiURL.openConnection();
		connection.setRequestProperty("Content-Type", contentType);
		connection.setRequestProperty("X-Access-Token", TOKEN);
		connection.setRequestMethod(method);
		connection.setDoInput(true);
		if (body != null) {
			connection.setDoOutput(true);
			connection.setFixedLengthStreamingMode(body.length);
			OutputStream os = connection.getOutputStream();
			os.write(body);
			os.flush();
		}
		int statusCode = connection.getResponseCode();
		if (statusCode < 400) {
			JsonReader jReader = Json.createReader(connection.getInputStream());
			JsonObject jsonBody = jReader.readObject();
			genericCallback(false, statusCode, jsonBody.toString());
			return jsonBody;
		} else if (statusCode == 401) {
			genericCallback(true, statusCode, "Invalidated TOKEN");
			return null;
		} else {
			JsonReader jReader = Json.createReader(connection.getErrorStream());
			JsonObject jsonError = jReader.readObject();
			genericCallback(true, statusCode, jsonError.toString());
			return jsonError;
		}
	}

	private static void step1_UploadImages() throws IOException, InterruptedException {
		logger.info("*** STEP 1 - Upload Images ***");

		// Set the maximum number of concurrent uploads
		int concurrentUploads = 1;
		int concurrentCount = 0;
		Thread[] pool = new Thread[concurrentUploads];
		for (File person : peoples) {
			String objectId = person.getName();
			String requestParams = "?train=manual&objectType=person&objectId=" + objectId;
			for (File image : person.listFiles()) {
				if (image.isFile() && !image.isHidden()) {
					final String api = BASE_URL + "image/" + URLEncoder.encode(image.getName(), "UTF-8")
							+ requestParams;
					final byte[] data = Files.readAllBytes(Paths.get(image.getCanonicalPath()));
					pool[concurrentCount] = new Thread() {
						public void run() {
							try {
								httpCall(api, "PUT", contentTypeStream, data);
							} catch (IOException e) {
								logger.warning(e.getMessage());
							}
						};
					};
					pool[concurrentCount].start();
					concurrentCount++;
					if (concurrentCount >= concurrentUploads) {
						for (int c = 0; c < concurrentCount; c++) {
							pool[c].join();
						}
						concurrentCount = 0;
					}
				}
			}
			for (int c = 0; c < concurrentCount; c++) {
				pool[c].join();
			}
		}
	}

	private static void step2_AddObjectsToGroup() throws IOException {
		logger.info("*** STEP 2 - Adding People to Group  ***");
		String groupId = "family";
		final String api = BASE_URL + "group/" + URLEncoder.encode(groupId, "UTF-8");
		JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
		for (File person : peoples) {
			jsonArrayBuilder.add(person.getName());
		}

		JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
		jsonObjectBuilder.add("objectIds", jsonArrayBuilder);
		byte[] data = jsonObjectBuilder.build().toString().getBytes("UTF-8");
		httpCall(api, "PUT", contentTypeJson, data);

	}

	private static void step3_TrainGroup(String groupId) throws IOException {
		logger.info("*** Step 3 - Training Group '${groupId}' ***");
		final String api = BASE_URL + "group/" + URLEncoder.encode(groupId, "UTF-8") + "/training2";
		httpCall(api, "POST", contentTypeJson, null);

	}

	private static void step4_TestReco(String groupId) throws IOException {
		logger.info("*** Step 4 - Test the Face Recognition ***");
		final String api = BASE_URL + "recognition?groupId=" + URLEncoder.encode(groupId, "UTF-8");
		File outFolder = new File(workingFolder + File.separator + "out");
		outFolder.mkdir();
		File testFolder = new File(imageFolder + File.separator + "reco-test");
		if (testFolder.exists()) {
			for (File recoFile : testFolder.listFiles()) {
				if (recoFile.isFile() && !recoFile.isHidden()) {
					final byte[] data = Files.readAllBytes(Paths.get(recoFile.getCanonicalPath()));
					JsonObject result = httpCall(api, "POST", contentTypeStream, data);
					if (result != null) {
						annotateImage(outFolder, recoFile, result.getJsonArray("objects"));
					}
				}
			}
		} else {
			logger.info("Failed to find images at " + testFolder.getCanonicalPath());
		}
	}

	// markup the image with bounding boxes, names, and confidence scores.
	public static void annotateImage(File outFolder, File image, JsonArray objects) throws IOException {
		if (outFolder.isDirectory() && image.isFile() && objects != null && objects.size() > 0) {
			BufferedImage imageBuffer = ImageIO.read(image);
			Graphics2D g = imageBuffer.createGraphics();// .getGraphics();
			g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setFont(new Font("Courier", Font.BOLD, 20));
			for (int oi = 0; oi < objects.size(); oi++) {
				JsonObject object = objects.getJsonObject(oi);
				JsonObject faceAnnotation = object.getJsonObject("faceAnnotation");
				JsonArray vertices = faceAnnotation.getJsonObject("bounding").getJsonArray("vertices");
				double confidence = faceAnnotation.getJsonNumber("recognitionConfidence").doubleValue();
				int nPoints = vertices.size();
				int[] xPoints = new int[nPoints];
				int[] yPoints = new int[nPoints];
				for (int ni = 0; ni < nPoints; ni++) {
					JsonObject point = vertices.getJsonObject(ni);
					xPoints[ni] = point.getInt("x");
					yPoints[ni] = point.getInt("y");
				}
				String name = object.getString("objectId");
				if (confidence < recognitionConfidenceThreshold) {
					name = "Unknown";
					g.setColor(Color.YELLOW);
					logger.info("An 'Unknown' person was found since recognition " + "confidence " + confidence
							+ " is below the minimum threshold of " + recognitionConfidenceThreshold);
				} else {
					g.setColor(Color.decode("#73c7f1"));
					logger.info("Recognized " + name + " with confidence " + confidence);
				}
				g.drawPolygon(xPoints, yPoints, nPoints);
				int x = xPoints[nPoints - 1];
				int y = yPoints[nPoints - 1];
				g.drawString(name, x, y + 16);
				g.drawString(String.valueOf(confidence), x, y + 36);
			}
			ImageIO.write(imageBuffer, "JPG",
					new File(outFolder.getCanonicalPath() + File.separator + image.getName()));
		}
	}

	/*** Main Function **/

	public static void main(String[] args) throws IOException, InterruptedException {
		if (workingFolder == null) {
			workingFolder = new File(".").getCanonicalPath();
		}
		if (imageFolder == null) {
	//		imageFolder = workingFolder + File.separator + ".." + File.separator + ".." + File.separator + "images";
			imageFolder = "images";
		}
		logger.info(imageFolder);
		File images = new File(imageFolder + File.separator + "training");
		if (images.exists()) {
			for (File person : images.listFiles()) {
				if (person.isDirectory()) {
					peoples.add(person);
				}
			}
			step1_UploadImages();
			step2_AddObjectsToGroup();
//			step3_TrainGroup("family");
//			step3_TrainGroup("friends");
//			step4_TestReco("family");
		} else {
			logger.info("Failed to find images at " + images.getCanonicalPath());
		}
	}
}
