package face.hack2017;

import face.hack2017.runner.DataUploader;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "FaceHack2017 started!" );
        System.out.println("Training.....");
       
        DataUploader datauploader = new DataUploader("data.txt");
    }
}
