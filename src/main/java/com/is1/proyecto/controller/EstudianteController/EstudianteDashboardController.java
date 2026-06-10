package com.is1.proyecto.controller.EstudianteController;
import java.util.HashMap;
import java.util.Map;

import com.is1.proyecto.models.Estudiante;
import com.is1.proyecto.models.Persona;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public class EstudianteDashboardController {

    // GET /estudiante/dashboard
    public ModelAndView showDashboard(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        String userRol = req.session().attribute("userRol");

        if(userId == null || !"ALUMNO".equals(userRol)) {
            res.redirect("/dashboard?error=Acceso no autorizado.");
            return null;
        }

        Estudiante estudiante = Estudiante.findFirst("user_id = ?", userId);
        if(estudiante == null) {
            res.redirect("/dashboard?error=No se encontro el perfil de estudiante.");
            return null;
        }

        Persona persona = Persona.findFirst("dni = ?", estudiante.getDni());

        Map<String, Object> model = new HashMap<>();
        model.put("nombre", persona != null ? persona.getNombre() : "");
        model.put("apellido", persona != null ? persona.getApellido() : "");
        model.put("nroLegajo", estudiante.getNroLegajo());

        String error = req.queryParams("error");
        if(error != null) {
            model.put("errorMessage", error);
        }

        return new ModelAndView(model, "estudiante/estudianteDashboard.mustache");
    }
}