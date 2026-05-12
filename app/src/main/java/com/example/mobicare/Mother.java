package com.example.mobicare;

public class Mother {
    private String fullName;
    private String birthDate;
    private String age;
    private String address;
    private String phone;
    private String civilStatus;
    private String email;
    private String emergencyContact;
    private String medicalHistory;
    private String linkedUid; // This needs to be here!

    // Empty constructor for Firebase
    public Mother() {}

    // Constructor with all fields
    public Mother(String fullName, String birthDate, String age, String address,
                  String phone, String civilStatus, String email,
                  String emergencyContact, String medicalHistory, String linkedUid) {
        this.fullName = fullName;
        this.birthDate = birthDate;
        this.age = age;
        this.address = address;
        this.phone = phone;
        this.civilStatus = civilStatus;
        this.email = email;
        this.emergencyContact = emergencyContact;
        this.medicalHistory = medicalHistory;
        this.linkedUid = linkedUid;
    }

    // Getters and Setters (Important for Firebase to read/write)
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getAge() { return age; }
    public void setAge(String age) { this.age = age; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCivilStatus() { return civilStatus; }
    public void setCivilStatus(String civilStatus) { this.civilStatus = civilStatus; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }

    public String getMedicalHistory() { return medicalHistory; }
    public void setMedicalHistory(String medicalHistory) { this.medicalHistory = medicalHistory; }

    public String getLinkedUid() { return linkedUid; }
    public void setLinkedUid(String linkedUid) { this.linkedUid = linkedUid; }
}