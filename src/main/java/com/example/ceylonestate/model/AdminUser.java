package com.example.ceylonestate.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * An administrator account with elevated permissions.
 * OOP: Inheritance - extends User, same as RegularUser, but...
 * OOP: Polymorphism - getRole()/getDashboardPath() return DIFFERENT values here,
 *      proving the two subclasses genuinely behave differently at runtime even
 *      though they're both just "a User" from the outside.
 */
@Entity
@DiscriminatorValue("ADMIN")
public class AdminUser extends User {

    protected AdminUser() {
        // required by JPA
    }

    public AdminUser(String username, String email, String password) {
        super(username, email, password);
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }

    @Override
    public String getDashboardPath() {
        return "/admin";
    }
}
