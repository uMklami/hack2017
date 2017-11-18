package face.hack2017.runner;

import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.json.JsonObject;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import face.hack2017.Recognition;

public class Recognizer {
	
	public Recognizer(){
	}
	public static void DisplayImage(File file) throws IOException
    {
        BufferedImage img=ImageIO.read(file);
        ImageIcon icon=new ImageIcon(img);
        JFrame frame=new JFrame();
        frame.setLayout(new FlowLayout());
        frame.setSize(500,700);
        JLabel lbl=new JLabel();
        lbl.setIcon(icon);
        frame.add(lbl);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
	
	public static File getRecognize(File image) throws IOException{
		final String api = Recognition.BASE_URL + "recognition?groupId=" + URLEncoder.encode("friends", "UTF-8");
		File outFolder = new File(Recognition.workingFolder + File.separator + "out");
		outFolder.mkdir();
		File returnfile = null;
		
		if (image.isFile() && !image.isHidden()) {
			final byte[] data = Files.readAllBytes(Paths.get(image.getCanonicalPath()));
			JsonObject result = Recognition.httpCall(api, "POST", Recognition.contentTypeStream, data);
			if (result != null) {
				Recognition.annotateImage(outFolder, image, result.getJsonArray("objects"));
			}
			returnfile = new File(outFolder+"/out/"+image+".jpg");
//			DisplayImage(returnfile);
			
		}
		return returnfile;
	}
	
//	public static void main(String[] args) throws IOException{
//		getRecognize(new File("D:\\Office Data\\OpenSourceProjects\\hack2017\\images\\reco-test\\IMG_9259.jpg"));
//	}

}
