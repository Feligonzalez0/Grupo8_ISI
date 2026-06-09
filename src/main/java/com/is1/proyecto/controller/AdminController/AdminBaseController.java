package com.is1.proyecto.controller.AdminController;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Request;
import spark.Response;

public abstract class AdminBaseController {
    
    protected static final Logger logger = LoggerFactory.getLogger(AdminBaseController.class);
    
    /**
     * Agrega mensajes de éxito/error al modelo desde los query parameters
     */
    protected void agregarMensajes(Request req, Map<String, Object> model) {
        String success = req.queryParams("successMessage");
        String error = req.queryParams("errorMessage");
        if (success != null && !success.trim().isEmpty()) {
            model.put("successMessage", success);
        }
        if (error != null && !error.trim().isEmpty()) {
            model.put("errorMessage", error);
        }
    }
    
    /**
     * Codifica un mensaje para usarlo en un query param de redirección
     */
    protected String encode(String mensaje) {
        if (mensaje == null) return "Error+desconocido.";
        return mensaje.replace(" ", "+");
    }
    
    /**
     * Redirección con manejo de error
     */
    protected Object redirectWithError(Response res, String path, String error) {
        res.redirect(path + "?errorMessage=" + encode(error));
        return null;
    }
    
    /**
     * Redirección con manejo de éxito
     */
    protected Object redirectWithSuccess(Response res, String path, String success) {
        res.redirect(path + "?successMessage=" + encode(success));
        return null;
    }
}