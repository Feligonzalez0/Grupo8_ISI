package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("Persona")
public class Persona extends Model {
    // DNI
    public Integer getDNI() {
        return getInteger("dni");
    }

    public void setDNI(Integer dni) {
        set("dni", dni);
    }

    // Nombre
    public String getNombre() {
        return getString("nombre");
    }

    public void setNombre(String nombre) {
        set("nombre", nombre);
    }

    // Apellido
    public String getApellido() {
        return getString("apellido");
    }

    public void setApellido(String apellido) {
        set("apellido", apellido);
    }


    // Nacimiento
    public Integer getNacimiento() {
        return getInteger("nacimiento");
    }

    public void setNacimiento(Integer nacimiento) {
        set("nacimiento", nacimiento);
    }

    // Teléfono
    public Integer getTelefono() {
        return getInteger("telefono");
    }

    public void setTelefono(Integer telefono) {
        set("telefono", telefono);
    }

    // Dirección
    public String getDireccion() {
        return getString("direccion");
    }

    public void setDireccion(String direccion) {
        set("direccion", direccion);
    }
}
