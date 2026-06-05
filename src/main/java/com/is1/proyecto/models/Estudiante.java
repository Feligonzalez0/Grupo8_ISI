package com.is1.proyecto.models;

import org.javalite.activejdbc.annotations.Table;

@Table("Estudiante")
public class Estudiante extends Persona{
    
    public Integer getDni() {
        return getInteger("dni");
    }

    public void setDni(Integer dni) {
        set("dni", dni);
    }

    public Integer getNroLegajo() {
        return getInteger("nro_legajo");
    }

    public void setNroLegajo(Integer codigo) {
        set("nro_legajo", codigo);
    }

    public String getEmail() {
        return getString("email");
    }

    public void setEmail(String email) {
        set("email", email);
    }
}