package face.hack2017.runner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Base64;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class Detector {

	public Detector() {

	}

	public static void getDetect(String imagepath){
		final String api = "https://dev.sighthoundapi.com/v1/detections?type=person,face?faceOption=gender,landmarks,age,emotion";
		  final String accessToken = "uY5kIX3zmFp4pqeUxVeCbgh6IF0HJoHCnpfI";
//		  private static String imageUrl = "https://www.example.com/path/to/image.jpg";
		 
		
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
		        System.out.println(jsonBody);
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
