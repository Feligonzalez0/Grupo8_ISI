package com.is1.proyecto.models;

import org.javalite.activejdbc.annotations.Table;

@Table("Estudiante")

public class Estudiante extends Persona{

  public Integer getDNI() {
        return getInteger("dni"); // Obtiene el valor de la columna 'dni'
    }

    public void setDNI(Integer dni) {
        set("dni", dni); // Establece el valor para la columna 'dni'
    }

    // CODIGO
    public Integer getNroLeg() {
        return getInteger("nro_legajo"); // Obtiene el valor de la columna "nro_legajo"
    }

    public void setNroLeg(Integer codigo) {
        set("nro_legajo", codigo); // Establece el valor para la columna "nro_legajo"
    }

    // EMAIL
    public String getEmail() {
        return getString("email");
    }

    public void setEmail(String email) {
        set("email", email);
    }






}
