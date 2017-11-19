package face.hack2017.runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import face.hack2017.models.Person;

public class DataUploader {
	
	private List<Person> people = new ArrayList<Person>();

	public DataUploader(String jsonfile) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			List<String> peoples =  loadData(jsonfile);
			
			for(String line : peoples){
				people.add(mapper.readValue(line, Person.class));
			}
			
			

		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public List<Person> getPeople() {
		return people;
	}

	public void setPeople(List<Person> people) {
		this.people = people;
	}

	public static List<String> loadData(String filePath) {
		int counter = 0;
		BufferedReader br = null;
		List<String> data = null;
		try {
			br = new BufferedReader(new FileReader(new File(filePath)));
			data = new ArrayList<String>();
			String line = null;
			while ((line = br.readLine()) != null) {
				if ((line = line.trim()).length() < 2) {
					continue;
				}
				data.add(line);
				counter++;
			}
			br.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
			}
		}
		return data;
	}
}
