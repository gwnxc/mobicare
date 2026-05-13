package com.example.mobicare;

public class Child {
    public String firstName;
    public String middleName;
    public String lastName;
    public String birthDate;
    public String gender;
    public String placeOfBirth;
    public String parentUid; // Store the UID here instead of just a name

    public Child() {}

    public Child(String firstName, String middleName, String lastName,
                 String birthDate, String gender, String placeOfBirth, String parentUid) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.gender = gender;
        this.placeOfBirth = placeOfBirth;
        this.parentUid = parentUid;
    }
}