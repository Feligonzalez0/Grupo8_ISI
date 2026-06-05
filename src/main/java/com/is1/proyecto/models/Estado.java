package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("Estado")
public class Estado extends Model {

    public Integer getDniEstudiante() {
        return getInteger("dni_estudiante");
    }

    public void setDniEstudiante(Integer dniEstudiante) {
        set("dni_estudiante", dniEstudiante);
    }

    public Integer getCodMateria() {
        return getInteger("cod_materia");
    }

    public void setCodMateria(Integer codMateria) {
        set("cod_materia", codMateria);
    }

    public TEstado getEstado() {
        String estado = getString("estado");
        if(estado == null) {
            return null;
        }

        return TEstado.valueOf(estado);
    }

    public void setEstado(TEstado estado) {
        set("estado", estado.name());
    }
}