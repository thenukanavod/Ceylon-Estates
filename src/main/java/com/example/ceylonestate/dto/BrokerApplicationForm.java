package com.example.ceylonestate.dto;

import com.example.ceylonestate.model.District;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** What the "Apply to be a broker" form posts. */
public class BrokerApplicationForm {

    @NotBlank @Size(max = 80)
    private String name;

    @NotBlank @Email @Size(max = 120)
    private String email;

    @NotBlank @Size(max = 30)
    private String phone;

    @Size(max = 100)
    private String company;

    @Size(max = 40)
    private String licenseNumber;

    @NotNull
    private District district;

    @Size(max = 500)
    private String bio;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public District getDistrict() { return district; }
    public void setDistrict(District district) { this.district = district; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
