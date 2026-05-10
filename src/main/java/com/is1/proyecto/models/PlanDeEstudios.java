package com.is1.proyecto.models;

import java.sql.Date;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("PlanDeEstudios")

public class PlanDeEstudios extends Model{

  public Integer getCod() {
        return getInteger("cod_plan");
    }

    public void setCod(Integer cod_plan) {
        set("cod_plan", cod_plan);
    }

    public Integer getAño() {
        return getInteger("año");
    }

    public void setAño(Date año) {
        set("año", año);
    }

    public Integer getVigencia() {
        return getInteger("vigencia");
    }

    public void setVigencia(Integer vigencia) {
        set("vigencia", vigencia);
    }

    public Integer getAñosTotal() {
        return getInteger("años_total");
    }

    public void setAñosTotal(Integer años_total) {
        set("años_total", años_total);
    }

    public Integer getCantidadMaterias() {
        return getInteger("cantidad_materias_total");
    }

    public void setCantidadMaterias(Integer cantidad_materias_total) {
        set("cantidad_materias_total", cantidad_materias_total);
    }
}