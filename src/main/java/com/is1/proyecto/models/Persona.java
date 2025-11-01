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
    public Integer getFechaNacimiento() {
        return getInteger("fecha_nacimiento");
    }

    public void setFechaNacimiento(Integer fecha_nacimiento) {
        set("fecha_nacimiento", fecha_nacimiento);
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

    // Edad
    public Integer getEdad() {
        return getInteger("edad");
    }

    public void setEdad(Integer edad) {
        set("edad", edad);
    }
}
