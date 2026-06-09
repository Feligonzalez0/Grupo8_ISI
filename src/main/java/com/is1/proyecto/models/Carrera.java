package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("Carrera")
public class Carrera extends Model {

    public Integer getCodigo() {
        return getInteger("cod_carrera");
    }

    public void setCodigo(Integer codigo) {
        set("cod_carrera", codigo);
    }

    public String getNombre() {
        return getString("nombre");
    }

    public void setNombre(String nombre) {
        set("nombre", nombre);
    }

    public String getDescripcion() {
        return getString("descripcion");
    }

    public void setDescripcion(String descripcion) {
        set("descripcion", descripcion);
    }
}