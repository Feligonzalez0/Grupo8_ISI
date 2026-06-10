package com.is1.proyecto.controller.EstudianteController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.models.Estado;
import com.is1.proyecto.models.Estudiante;
import com.is1.proyecto.models.ExamenFinal;
import com.is1.proyecto.models.InscripcionExamen;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.services.AuditoriaService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public class EstudianteExamenController {

    // GET /estudiante/examenes
    public ModelAndView showExamenes(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Estudiante estudiante = Estudiante.findFirst("user_id = ?", userId);
        if(estudiante == null) {
            res.redirect("/dashboard?error=No se encontró el perfil de estudiante.");
            return null;
        }

        List<Estado> regulares = Estado.where("dni_estudiante = ? AND estado = ?", estudiante.getDni(), "REGULAR");
        List<Map<String, Object>> examenesView = new ArrayList<>();

        for(Estado e : regulares) {
            Integer codMateria = e.getCodMateria();
            boolean yaInscripto = InscripcionExamen.yaInscripto(estudiante.getDni(), codMateria);
            Materia materia = Materia.findFirst("cod_materia = ?", codMateria);

            for(ExamenFinal ex : (List<ExamenFinal>)(List<?>)ExamenFinal.where("cod_materia = ?", codMateria)) {
                Map<String, Object> ev = new HashMap<>();
                ev.put("idExamen", ex.getId());
                ev.put("nombreMateria", materia != null ? materia.getNombre() : "Sin nombre");
                ev.put("fecha", ex.getFecha());
                ev.put("yaInscripto", yaInscripto);
                examenesView.add(ev);
            }
        }

        Map<String, Object> model = new HashMap<>();
        model.put("examenes", examenesView);
        model.put("sinExamenes", examenesView.isEmpty());
        model.put("successMessage", req.queryParams("successMessage"));
        model.put("errorMessage", req.queryParams("errorMessage"));

        return new ModelAndView(model, "estudiante/examenesDisponibles.mustache");
    }

    // POST /estudiante/examenes/:id/inscribir
    public Object handleInscribirExamen(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Estudiante estudiante = Estudiante.findFirst("user_id = ?", userId);
        if(estudiante == null) { res.redirect("/dashboard"); return null; }

        Integer idExamen = Integer.parseInt(req.params(":id"));
        ExamenFinal examen = ExamenFinal.findById(idExamen);
        if(examen == null) {
            res.redirect("/estudiante/examenes?errorMessage=Examen no encontrado.");
            return null;
        }

        Estado estado = Estado.findFirst("dni_estudiante = ? AND cod_materia = ? AND estado = ?",
            estudiante.getDni(), examen.getCodMateria(), "REGULAR");
        if(estado == null) {
            res.redirect("/estudiante/examenes?errorMessage=No tenes esa materia en estado Regular.");
            return null;
        }

        if(InscripcionExamen.yaInscripto(estudiante.getDni(), examen.getCodMateria())) {
            res.redirect("/estudiante/examenes?errorMessage=Ya estas inscripto a un examen de esa materia.");
            return null;
        }

        try {
            InscripcionExamen inscripcion = new InscripcionExamen();
            inscripcion.setDniEstudiante(estudiante.getDni());
            inscripcion.setIdExamen(idExamen);
            inscripcion.saveIt();
            AuditoriaService.registrarAuditoria(req, "INSCRIBIR_EXAMEN", "dni:" + estudiante.getDni() + " idExamen:" + idExamen);
            res.redirect("/estudiante/examenes?successMessage=Inscripcion realizada correctamente.");
        } catch (Exception e) {
            res.redirect("/estudiante/examenes?errorMessage=Error al inscribirse: " + e.getMessage());
        }

        return null;
    }
}   