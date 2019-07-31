package org.pavelreich.zoo;

public class User {

	private String firstName;
	private String lastName;
	
	private int birthYear;
	private Gender gender;

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public int getBirthYear() {
		return birthYear;
	}

	public void setBirthYear(int birthYear) {
		this.birthYear = birthYear;
	}
	
	enum Gender { MALE, FEMALE, OTHER}
	public void setGender(Gender gender) {
		this.gender = gender;
	}
	
	public String getTitle() {
		if (gender == Gender.MALE) {
			return "Mr";
		} else if (gender == Gender.FEMALE) {
			return "Ms";
		} else {
			return "";
		}
	}
	
	public String getSalutation() {
		return getTitle() + " " + getFirstName();
	}
}
