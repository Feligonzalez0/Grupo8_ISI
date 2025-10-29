package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("Materia")
public class Materia extends Model{
    // CODIGO
    public Integer getCodigo() {
        return getInteger("codigo");
    }

    public void setCodigo(Integer codigo) {
        set("codigo", codigo);
    }

    // Nombre
    public String getNombre() {
        return getString("nombre");
    }

    public void setNombre(String nombre) {
        set("nombre", nombre);
    }

    // Descripcion
    public String getDescripcion() {
        return getString("descripcion");
    }

    public void setDescripcion(String descripcion) {
        set("descripcion", descripcion);
    }

    // CodigoPlan
    public Integer getCodPlan() {
        return getInteger("cod_plan");
    }

    public void setCodPlan(Integer codPlan) {
        set("cod_plan", codPlan);
    }
}