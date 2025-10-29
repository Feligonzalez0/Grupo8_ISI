package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("Docente")
public class Docente extends Model {
    // DNI
    public Integer getDNI() {
        return getInteger("dni"); // Obtiene el valor de la columna 'dni'
    }

    public void setDNI(Integer dni) {
        set("dni", dni); // Establece el valor para la columna 'dni'
    }

    // CODIGO
    public Integer getCodigo() {
        return getInteger("codigo_profesor"); // Obtiene el valor de la columna 'codigo_profesor'
    }

    public void setCodigo(Integer codigo) {
        set("codigo_profesor", codigo); // Establece el valor para la columna 'codigo_profesor'
    }
}
