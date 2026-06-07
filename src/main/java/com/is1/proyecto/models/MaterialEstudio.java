package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("MaterialEstudio")
public class MaterialEstudio extends Model {

    public Integer getId() {
        return getInteger("id");
    }

    public Integer getCodMateria() {
        return getInteger("cod_materia");
    }

    public void setCodMateria(Integer codMateria) {
        set("cod_materia", codMateria);
    }

    public Integer getCodigoProfesor() {
        return getInteger("codigo_profesor");
    }

    public void setCodigoProfesor(Integer codigoProfesor) {
        set("codigo_profesor", codigoProfesor);
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

    public String getNombreArchivo() {
        return getString("nombre_archivo");
    }

    public void setNombreArchivo(String nombreArchivo) {
        set("nombre_archivo", nombreArchivo);
    }

    public String getRutaArchivo() {
        return getString("ruta_archivo");
    }

    public void setRutaArchivo(String rutaArchivo) {
        set("ruta_archivo", rutaArchivo);
    }

    public String getFechaSubida() {
        return getString("fecha_subida");
    }

    public void setFechaSubida(String fechaSubida) {
        set("fecha_subida", fechaSubida);
    }
}