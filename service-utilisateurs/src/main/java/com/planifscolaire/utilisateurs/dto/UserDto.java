package com.planifscolaire.utilisateurs.dto;

import com.planifscolaire.utilisateurs.enums.Role;

import jakarta.validation.constraints.*;

public class UserDto {
    private Long id;

    @NotBlank
    @Size(max = 255)
    private String nom;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotNull private Role role;

    private boolean actif = true;

    public UserDto() {}

    public UserDto(Long id, String nom, String email, Role role, boolean actif) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.role = role;
        this.actif = actif;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }
}
