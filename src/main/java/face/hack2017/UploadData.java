package face.hack2017;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UploadData {
	public static Logger logger;
	public static final String TOKEN = "YourSighthoundCloudToken";
    public static final String BASE_URL = "https://dev.sighthoundapi.com/v1/";

	UploadData() {
		logger = LogManager.getRootLogger();
	}

	

}
