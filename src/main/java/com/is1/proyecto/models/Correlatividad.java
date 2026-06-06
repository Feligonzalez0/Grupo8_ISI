package com.is1.proyecto.models;
 
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;
 
@Table("Correlatividad")
public class Correlatividad extends Model {
 
    public Integer getCodMateria() {
        return getInteger("cod_materia");
    }
 
    public void setCodMateria(Integer codMateria) {
        set("cod_materia", codMateria);
    }
 
    public Integer getCodCorrelativa() {
        return getInteger("cod_correlativa");
    }
 
    public void setCodCorrelativa(Integer codCorrelativa) {
        set("cod_correlativa", codCorrelativa);
    }
}