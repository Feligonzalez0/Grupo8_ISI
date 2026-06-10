package com.is1.proyecto.controller.DocenteController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.models.Docente;
import com.is1.proyecto.models.ExamenFinal;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.PeriodoAcademico;
import com.is1.proyecto.services.AuditoriaService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public class DocenteExamenController {

    // GET /docente/examenes/crear
    public ModelAndView showCrearExamen(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Docente docente = Docente.findFirst("user_id = ?", userId);
        if(docente == null) {
            res.redirect("/dashboard?error=No se encontro el perfil de docente.");
            return null;
        }

        List<PeriodoAcademico> periodos = PeriodoAcademico.where(
            "codigo_profesor = ? AND cargo = ?", docente.getCodigoProfesor(), "RESPONSABLE_DE_CATEDRA");

        List<Map<String, Object>> materiasView = new ArrayList<>();
        for(PeriodoAcademico p : periodos) {
            Materia m = Materia.findFirst("cod_materia = ?", p.getCodMateria());
            if(m != null) {
                Map<String, Object> mv = new HashMap<>();
                mv.put("cod_materia", m.getCodMateria());
                mv.put("nombre", m.getNombre());
                materiasView.add(mv);
            }
        }

        Map<String, Object> model = new HashMap<>();
        model.put("materias", materiasView);
        if (materiasView.isEmpty()) {
            model.put("errorMessage", "No tenés materias asignadas como Responsable de Cátedra.");
        }

        String error = req.queryParams("errorMessage");
        if(error != null) {
            model.put("errorMessage", error);
        }
            

        return new ModelAndView(model, "docente/crearExamen.mustache");
    }

    // POST /docente/examenes/crear
    public Object handleCrearExamen(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Docente docente = Docente.findFirst("user_id = ?", userId);
        if(docente == null) { 
            res.redirect("/dashboard"); return null; 
        }

        String codMateriaStr = req.queryParams("cod_materia");
        String fecha = req.queryParams("fecha");

        if(codMateriaStr == null || codMateriaStr.isEmpty() || fecha == null || fecha.isEmpty()) {
            res.redirect("/docente/examenes/crear?errorMessage=Todos los campos son obligatorios.");
            return null;
        }

        Integer codMateria = Integer.parseInt(codMateriaStr);
        PeriodoAcademico periodo = PeriodoAcademico.findFirst(
            "codigo_profesor = ? AND cod_materia = ? AND cargo = ?",
            docente.getCodigoProfesor(), codMateria, "RESPONSABLE_DE_CATEDRA");

        if(periodo == null) {
            res.redirect("/docente/examenes/crear?errorMessage=No tenés permiso para esa materia.");
            return null;
        }

        try {
            ExamenFinal examen = new ExamenFinal();
            examen.setCodigoProfesor(docente.getCodigoProfesor());
            examen.setCodMateria(codMateria);
            examen.setFecha(fecha);
            examen.saveIt();
            AuditoriaService.registrarAuditoria(req, "CREAR_EXAMEN",
                "codigoProfesor:" + docente.getCodigoProfesor() + " codMateria:" + codMateria + " fecha:" + fecha);
            res.redirect("/dashboard?message=Examen creado correctamente.");
        } catch (Exception e) {
            res.redirect("/docente/examenes/crear?errorMessage=Error al crear el examen: " + e.getMessage());
        }
        return null;
    }
}