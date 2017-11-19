package face.hack2017;


import java.util.List;

import face.hack2017.models.Person;
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
        List<Person> persons = datauploader.getPeople();
        for(Person person: persons){
        System.out.println(person.toString());
        }
        
    }
}
