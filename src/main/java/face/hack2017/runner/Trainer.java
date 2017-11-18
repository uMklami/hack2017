package face.hack2017.runner;

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

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import face.hack2017.Recognition;

public class Trainer {

	public void train() {
		try {
			if (workingFolder == null) {
				workingFolder = new File(".").getCanonicalPath();
			}
			if (imageFolder == null) {
				// imageFolder = workingFolder + File.separator + ".." +
				// File.separator + ".." + File.separator + "images";
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

				UploadImages();
				AddObjectsToGroup();
				TrainGroup("family");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

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

	public static String group = null;

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

	private static void UploadImages() throws IOException, InterruptedException {
		logger.info("Uploading Images");

		// Set the maximum number of concurrent uploads
		int concurrentUploads = 3;
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

	private static void AddObjectsToGroup() throws IOException {
		logger.info("Adding Images to " + group);
		String groupId = "friends";
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

	private static void TrainGroup(String groupId) throws IOException {
		logger.info("Training Session for '${groupId}'");
		final String api = BASE_URL + "group/" + URLEncoder.encode(groupId, "UTF-8") + "/training";
		httpCall(api, "POST", contentTypeJson, null);

	}

}
