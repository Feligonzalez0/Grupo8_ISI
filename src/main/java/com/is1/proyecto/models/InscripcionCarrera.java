package com.is1.proyecto.models;
 
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;
 
@Table("InscripcionCarrera")
public class InscripcionCarrera extends Model {
 
    public Integer getDniEstudiante() {
        return getInteger("dni_estudiante");
    }
 
    public void setDniEstudiante(Integer dni) {
        set("dni_estudiante", dni);
    }
 
    public Integer getCodCarrera() {
        return getInteger("cod_carrera");
    }
 
    public void setCodCarrera(Integer codCarrera) {
        set("cod_carrera", codCarrera);
    }
 
    public Situacion getSituacion() {
        String situacion = getString("situacion");
        if(situacion == null) {
            return null;
        }

        return Situacion.valueOf(situacion);
    }
 
    public void setSituacion(Situacion situacion) {
        set("situacion", situacion.name());
    }
}