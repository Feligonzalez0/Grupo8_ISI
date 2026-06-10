package com.is1.proyecto.controller.EstudianteController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.models.Carrera;
import com.is1.proyecto.models.Estudiante;
import com.is1.proyecto.models.InscripcionCarrera;
import com.is1.proyecto.models.Persona;
import com.is1.proyecto.models.Situacion;
import com.is1.proyecto.services.AuditoriaService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public class EstudianteCarreraController {

    // GET /estudiante/carrera
    public ModelAndView showCarrera(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Estudiante estudiante = Estudiante.findFirst("user_id = ?", userId);
        if(estudiante == null) {
            res.redirect("/dashboard?error=No se encontró el perfil de estudiante.");
            return null;
        }

        Persona persona = Persona.findFirst("dni = ?", estudiante.getDni());
        Map<String, Object> model = new HashMap<>();
        model.put("nombre", persona != null ? persona.getNombre() : "");
        model.put("apellido", persona != null ? persona.getApellido() : "");
        model.put("nroLegajo", estudiante.getNroLegajo());

        InscripcionCarrera inscripcion = InscripcionCarrera.findFirst("dni_estudiante = ?", estudiante.getDni());
        if(inscripcion != null) {
            Carrera carreraActual = Carrera.findFirst("cod_carrera = ?", inscripcion.getCodCarrera());
            model.put("yaInscripto", true);
            model.put("carreraActual", carreraActual != null ? carreraActual.getNombre() : "Sin nombre");
            model.put("descripcionActual", carreraActual != null ? carreraActual.getDescripcion(): "");
            model.put("situacionCarrera", inscripcion.getSituacion() != null ? inscripcion.getSituacion().name() : "");
            model.put("esIngresante", Situacion.INGRESANTE.equals(inscripcion.getSituacion()));
            model.put("esAvanzado", Situacion.AVANZADO.equals(inscripcion.getSituacion()));
        } else {
            List<Map<String, Object>> carreras = new ArrayList<>();
            for(Carrera c : (List<Carrera>)(List<?>)Carrera.findAll()) {
                Map<String, Object> cv = new HashMap<>();
                cv.put("codCarrera", c.getCodigo());
                cv.put("nombre", c.getNombre());
                cv.put("descripcion", c.getDescripcion() != null ? c.getDescripcion() : "");
                carreras.add(cv);
            }
            model.put("yaInscripto", false);
            model.put("carreras", carreras);
            model.put("sinCarreras", carreras.isEmpty());
        }

        String success = req.queryParams("successMessage");
        String error = req.queryParams("errorMessage");
        if(success != null){
            model.put("successMessage", success);
        } 
            
        if(error != null){
            model.put("errorMessage", error);
        } 

        return new ModelAndView(model, "estudiante/inscripcionCarrera.mustache");
    }

    // POST /estudiante/carrera/inscribir
    public Object handleInscribirCarrera(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Estudiante estudiante = Estudiante.findFirst("user_id = ?", userId);
        if(estudiante == null) {
            res.redirect("/dashboard?error=No se encontró el perfil de estudiante.");
            return null;
        }

        if(InscripcionCarrera.findFirst("dni_estudiante = ?", estudiante.getDni()) != null) {
            res.redirect("/estudiante/carrera?errorMessage=Ya estás inscripto en una carrera.");
            return null;
        }

        String codCarreraStr = req.queryParams("cod_carrera");
        if(codCarreraStr == null || codCarreraStr.isEmpty()) {
            res.redirect("/estudiante/carrera?errorMessage=Debe seleccionar una carrera.");
            return null;
        }

        Integer codCarrera;
        try {
            codCarrera = Integer.parseInt(codCarreraStr);
        } catch (NumberFormatException e) {
            res.redirect("/estudiante/carrera?errorMessage=Carrera inválida.");
            return null;
        }

        Carrera carrera = Carrera.findFirst("cod_carrera = ?", codCarrera);
        if(carrera == null) {
            res.redirect("/estudiante/carrera?errorMessage=La carrera seleccionada no existe.");
            return null;
        }

        try {
            estudiante.inscribirseCarrera(codCarrera);
            AuditoriaService.registrarAuditoria(req, "INSCRIBIR_CARRERA", "dni:" + estudiante.getDni() + " codCarrera:" + codCarrera);
            res.redirect("/estudiante/carrera?successMessage=Te inscribiste correctamente a " + carrera.getNombre() + ".");
        } catch (Exception e) {
            try {
                res.redirect("/estudiante/carrera?errorMessage=" + java.net.URLEncoder.encode(e.getMessage(), "UTF-8"));
            } catch (Exception ex) {
                res.redirect("/estudiante/carrera?errorMessage=Error al inscribirse a la carrera.");
            }
        }

        return null;
    }
}