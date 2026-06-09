package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("InscripcionExamen")
@IdName("id")
public class InscripcionExamen extends Model {

    public Integer getId() {
        return getInteger("id");
    }

    public Integer getDniEstudiante() {
        return getInteger("dni_estudiante");
    }

    public void setDniEstudiante(Integer dni) {
        set("dni_estudiante", dni);
    }

    public Integer getIdExamen() {
        return getInteger("id_examen");
    }

    public void setIdExamen(Integer idExamen) {
        set("id_examen", idExamen);
    }

    /**
     * Verifica si el estudiante ya está inscripto a algún examen de esa materia.
     */
    public static boolean yaInscripto(Integer dniEstudiante, Integer codMateria) {
        return InscripcionExamen.count("dni_estudiante = ? AND id_examen IN (SELECT id FROM ExamenFinal WHERE cod_materia = ?)", dniEstudiante, codMateria) > 0;
    }
}