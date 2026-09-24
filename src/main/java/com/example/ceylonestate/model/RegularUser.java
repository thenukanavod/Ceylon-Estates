package com.example.ceylonestate.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * A standard account with normal (non-admin) permissions.
 * OOP: Inheritance - extends User, reusing all shared fields/behaviour.
 * OOP: Polymorphism - provides its own version of getRole()/getDashboardPath().
 */
@Entity
@DiscriminatorValue("REGULAR")
public class RegularUser extends User {

    protected RegularUser() {
        // required by JPA
    }

    public RegularUser(String username, String email, String password) {
        super(username, email, password);
    }

    @Override
    public String getRole() {
        return "USER";
    }

    @Override
    public String getDashboardPath() {
        return "/profile";
    }
}
