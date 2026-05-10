package com.is1.proyecto.models;
import org.javalite.activejdbc.Model;

import org.javalite.activejdbc.annotations.Table;

@Table("Carrera")
/*
CREATE TABLE Carrera(
    cod_carrera INTEGER PRIMARY KEY,
    nombre TEXT,
    descripcion TEXT
); */
public class Carrera extends Model {

    public Integer getCodigo() {
        return getInteger("cod_carrera"); // Obtiene el valor de la columna 'cod_carrera'
    }

    public void setCodigo(Integer codigo) {
        set("cod_carrera", codigo); // Establece el valor para la columna 'cod_carrera'
    }

    public String getNombre() {
        return getString("nombre"); // Obtiene el valor de la columna "nombre"
    }

    public void setNombre(Integer nombre) {
        set("nombre", nombre); // Establece el valor para la columna "nombre"
    }

    // EMAIL
    public String getDescripcion() {
        return getString("descripcion");
    }

    public void setDescripcion(String descripcion) {
        set("descripcion", descripcion);
    }
}
