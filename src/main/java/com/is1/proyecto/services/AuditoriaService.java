
package com.is1.proyecto.services;
 
import com.is1.proyecto.config.AppConfig;
import com.is1.proyecto.models.AuditoriaAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
 
public class AuditoriaService {
    
    
private static final Logger logger = LoggerFactory.getLogger(AuditoriaService.class);

public static void registrarAuditoria(Request req, String accion, String detalle) {
        try {
            String usuario = req.session().attribute("currentUserUsername");
            AuditoriaAdmin.registrar(usuario, accion, detalle);
            logger.info("Auditoría registrada: usuario={}, accion={}, detalle={}", usuario, accion, detalle);
        } catch (Exception e) {
            System.err.println("ERROR AUDITORIA: " + e.getMessage());
            e.printStackTrace();
            logger.error("Error al registrar auditoría: {}", e.getMessage(), e);
        }
}
}