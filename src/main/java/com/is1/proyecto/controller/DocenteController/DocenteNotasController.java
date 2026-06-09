package com.is1.proyecto.controller.DocenteController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javalite.activejdbc.Base;

import com.is1.proyecto.models.Docente;
import com.is1.proyecto.models.Estado;
import com.is1.proyecto.models.Estudiante;
import com.is1.proyecto.models.ExamenFinal;
import com.is1.proyecto.models.InscripcionExamen;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.Persona;
import com.is1.proyecto.services.AuditoriaService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public class DocenteNotasController {

    // GET /docente/notas
    public ModelAndView showNotas(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Docente docente = Docente.findFirst("user_id = ?", userId);
        if(docente == null) { res.redirect("/dashboard?error=No se encontró el perfil de docente."); return null; }

        List<ExamenFinal> examenesDB = ExamenFinal.where("codigo_profesor = ?", docente.getCodigoProfesor());
        List<Map<String, Object>> examenesView = new ArrayList<>();
        for(ExamenFinal ex : examenesDB) {
            Materia mat = Materia.findFirst("cod_materia = ?", ex.getCodMateria());
            Map<String, Object> ev = new HashMap<>();
            ev.put("idExamen",      ex.getId());
            ev.put("nombreMateria", mat != null ? mat.getNombre() : "Sin nombre");
            ev.put("fecha",         ex.getFecha());
            examenesView.add(ev);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("examenes",    examenesView);
        model.put("sinExamenes", examenesView.isEmpty());

        String idExamenStr = req.queryParams("id_examen");
        if(idExamenStr != null && !idExamenStr.isEmpty()) {
            int idExamen = Integer.parseInt(idExamenStr);
            for(Map<String, Object> ev : examenesView)
                ev.put("seleccionado", ev.get("idExamen").equals(idExamen));

            ExamenFinal examen = ExamenFinal.findById(idExamen);
            if(examen == null || !examen.getCodigoProfesor().equals(docente.getCodigoProfesor())) {
                res.redirect("/docente/notas?errorMessage=Examen no encontrado o sin permiso."); return null;
            }

            Materia materia = Materia.findFirst("cod_materia = ?", examen.getCodMateria());
            List<InscripcionExamen> inscripciones = InscripcionExamen.where("id_examen = ?", idExamen);
            List<Map<String, Object>> alumnosView = new ArrayList<>();

            for(InscripcionExamen insc : inscripciones) {
                Estudiante estudiante = Estudiante.findFirst("dni = ?", insc.getDniEstudiante());
                Persona persona       = Persona.findFirst("dni = ?", insc.getDniEstudiante());
                Estado estadoActual   = Estado.findFirst("dni_estudiante = ? AND cod_materia = ?", insc.getDniEstudiante(), examen.getCodMateria());

                String estadoStr  = estadoActual != null ? estadoActual.getString("estado") : "REGULAR";
                boolean yaCalificado = "APROBADO".equals(estadoStr) || "LIBRE".equals(estadoStr);
                String estadoClass;
                if("APROBADO".equals(estadoStr))     estadoClass = "bg-green-100 text-green-700";
                else if("LIBRE".equals(estadoStr))   estadoClass = "bg-red-100 text-red-700";
                else if("REGULAR".equals(estadoStr)) estadoClass = "bg-blue-100 text-blue-700";
                else                                 estadoClass = "bg-yellow-100 text-yellow-700";

                String nombre   = persona != null ? persona.getNombre()   : "";
                String apellido = persona != null ? persona.getApellido() : "";
                String iniciales = ((!nombre.isEmpty()   ? String.valueOf(nombre.charAt(0))   : "")
                                  + (!apellido.isEmpty() ? String.valueOf(apellido.charAt(0)) : "")).toUpperCase();

                Map<String, Object> av = new HashMap<>();
                av.put("dni",          insc.getDniEstudiante());
                av.put("nombre",       nombre);
                av.put("apellido",     apellido);
                av.put("iniciales",    iniciales);
                av.put("email",        estudiante != null ? estudiante.getEmail()     : "");
                av.put("nroLegajo",    estudiante != null ? estudiante.getNroLegajo() : "-");
                av.put("codMateria",   examen.getCodMateria());
                av.put("idExamen",     idExamen);
                av.put("estadoActual", estadoStr);
                av.put("estadoClass",  estadoClass);
                av.put("yaCalificado", yaCalificado);
                alumnosView.add(av);
            }

            model.put("examenSeleccionado",       true);
            model.put("nombreMateriaSeleccionada", materia != null ? materia.getNombre() : "Sin nombre");
            model.put("fechaExamen",   examen.getFecha());
            model.put("totalAlumnos",  alumnosView.size());
            model.put("alumnos",       alumnosView);
            model.put("sinAlumnos",    alumnosView.isEmpty());
            model.put("idExamenActual", idExamen);
        }

        model.put("successMessage", req.queryParams("successMessage"));
        model.put("errorMessage",   req.queryParams("errorMessage"));
        return new ModelAndView(model, "docente/notasFinales.mustache");
    }

    // POST /docente/notas/cargar
    public Object handleCargarNota(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Docente docente = Docente.findFirst("user_id = ?", userId);
        if(docente == null) { res.redirect("/dashboard?error=No se encontró el perfil de docente."); return null; }

        String idExamenStr     = req.queryParams("id_examen");
        String dniEstudianteStr= req.queryParams("dni_estudiante");
        String codMateriaStr   = req.queryParams("cod_materia");
        String resultado       = req.queryParams("resultado");

        if(idExamenStr == null || idExamenStr.isEmpty() || dniEstudianteStr == null || dniEstudianteStr.isEmpty()
        || codMateriaStr == null || codMateriaStr.isEmpty() || resultado == null || resultado.isEmpty()) {
            res.redirect("/docente/notas?errorMessage=Datos incompletos. Seleccioná un resultado."); return null;
        }

        int idExamen      = Integer.parseInt(idExamenStr);
        int dniEstudiante = Integer.parseInt(dniEstudianteStr);
        int codMateria    = Integer.parseInt(codMateriaStr);

        ExamenFinal examen = ExamenFinal.findById(idExamen);
        if(examen == null || !examen.getCodigoProfesor().equals(docente.getCodigoProfesor())) {
            res.redirect("/docente/notas?errorMessage=No tenés permiso para ese examen."); return null;
        }

        InscripcionExamen inscripcion = InscripcionExamen.findFirst("id_examen = ? AND dni_estudiante = ?", idExamen, dniEstudiante);
        if(inscripcion == null) {
            res.redirect("/docente/notas?id_examen=" + idExamen + "&errorMessage=El alumno no está inscripto a ese examen."); return null;
        }

        Estado estadoActual = Estado.findFirst("dni_estudiante = ? AND cod_materia = ?", dniEstudiante, codMateria);
        if(estadoActual == null) {
            res.redirect("/docente/notas?id_examen=" + idExamen + "&errorMessage=No se encontró el estado del alumno."); return null;
        }

        String estadoStr = estadoActual.getString("estado");
        if("APROBADO".equals(estadoStr) || "LIBRE".equals(estadoStr)) {
            res.redirect("/docente/notas?id_examen=" + idExamen + "&errorMessage=El alumno ya tiene un resultado cargado."); return null;
        }

        try {
            Base.openTransaction();
            Base.exec("UPDATE Estado SET estado = ? WHERE dni_estudiante = ? AND cod_materia = ?", resultado, dniEstudiante, codMateria);
            Base.commitTransaction();
            String msg = "APROBADO".equals(resultado) ? "Alumno aprobado correctamente." : "Resultado libre registrado.";
            AuditoriaService.registrarAuditoria(req, "CARGAR_NOTA",
                "idExamen:" + idExamen + " dni:" + dniEstudiante + " resultado:" + resultado);
            res.redirect("/docente/notas?id_examen=" + idExamen + "&successMessage=" + java.net.URLEncoder.encode(msg, "UTF-8"));
        } catch (Exception e) {
            Base.rollbackTransaction();
            res.redirect("/docente/notas?id_examen=" + idExamen + "&errorMessage=Error al guardar el resultado: " + e.getMessage());
        }
        return null;
    }
}