package com.is1.proyecto.models;
 
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("AuditoriaAdmin")
public class AuditoriaAdmin extends Model {
 
    public String getUsuario() {
        return getString("usuario");
    }
 
    public void setUsuario(String usuario) {
        set("usuario", usuario);
    }
 
    public String getAccion() {
        return getString("accion");
    }
 
    public void setAccion(String accion) {
        set("accion", accion);
    }
 
    public String getDetalle() {
        return getString("detalle");
    }
 
    public void setDetalle(String detalle) {
        set("detalle", detalle);
    }
 
    public String getFecha() {
        return getString("fecha");
    }
 
    public void setFecha(String fecha) {
        set("fecha", fecha);
    }
 
    public static void registrar(String usuario, String accion, String detalle) {
        AuditoriaAdmin log = new AuditoriaAdmin();

        log.setUsuario(usuario);
        log.setAccion(accion);
        log.setDetalle(detalle);
        log.setFecha(java.time.LocalDateTime.now().toString());

        log.saveIt();
    }
}