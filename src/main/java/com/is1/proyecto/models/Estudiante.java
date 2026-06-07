package com.is1.proyecto.models;

import org.javalite.activejdbc.annotations.Table;

@Table("Estudiante")
public class Estudiante extends Persona{
    
    public Integer getDni() {
        return getInteger("dni");
    }

    public void setDni(Integer dni) {
        set("dni", dni);
    }

    public Integer getNroLegajo() {
        return getInteger("nro_legajo");
    }

    public void setNroLegajo(Integer codigo) {
        set("nro_legajo", codigo);
    }

    public String getEmail() {
        return getString("email");
    }

    public void setEmail(String email) {
        set("email", email);
    }

    public void inscribirseMateria(Integer codMateria) {
        Estado existente = Estado.findFirst( "dni_estudiante = ? AND cod_materia = ?", this.getDni(), codMateria);

        if(existente != null) {
            throw new RuntimeException("Ya está inscripto en la materia");
        }

        Estado estado = new Estado();

        estado.setDniEstudiante(getDni());
        estado.setCodMateria(codMateria);
        estado.setEstado(TEstado.INSCRIPTO);

        estado.saveIt();
    }

    public void inscribirseCarrera(Integer codCarrera) {
        InscripcionCarrera existente = InscripcionCarrera.findFirst( "dni_estudiante = ? AND cod_carrera = ?", this.getDni(), codCarrera);

        if(existente != null) {
            throw new RuntimeException("Ya estás inscripto en una carrera");
        }

        InscripcionCarrera inscripcion = new InscripcionCarrera();

        inscripcion.setDniEstudiante(this.getDni());
        inscripcion.setCodCarrera(codCarrera);
        inscripcion.setSituacion(Situacion.INGRESANTE);

        inscripcion.saveIt();
    }
}