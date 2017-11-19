package face.hack2017.models;

public class Person {

	private String name;
	private String drink;
	private String eat;

	public Person() {

	}

	public Person(String name, String drink, String eat) {
		this.name = name;
		this.eat = eat;
		this.drink = drink;
	}
	public void addNewPerson(String name, String drink, String eat){
		this.name = name;
		this.drink = drink;
		this.eat = eat;
	}

	public String getEat() {
		return eat;
	}

	public void setEat(String eat) {
		this.eat = eat;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDrinks() {
		return drink;
	}

	public void setDrinks(String drinks) {
		this.drink = drinks;
	}

	@Override
	public String toString() {
		return "you likes drinking =" + drink + ", and eating =" + eat + "";
	}

}
