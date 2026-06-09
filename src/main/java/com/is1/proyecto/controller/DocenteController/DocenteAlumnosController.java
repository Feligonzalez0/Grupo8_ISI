package com.is1.proyecto.controller.DocenteController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javalite.activejdbc.Base;

import com.is1.proyecto.models.Docente;
import com.is1.proyecto.models.Estado;
import com.is1.proyecto.models.Estudiante;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.PeriodoAcademico;
import com.is1.proyecto.models.Persona;
import com.is1.proyecto.services.AuditoriaService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public class DocenteAlumnosController {

    // GET /docente/alumnos
    public ModelAndView showAlumnos(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Docente docente = Docente.findFirst("user_id = ?", userId);
        if(docente == null) {
            res.redirect("/dashboard?error=No se encontró el perfil de docente.");
            return null;
        }

        List<PeriodoAcademico> periodos = PeriodoAcademico.where("codigo_profesor = ?", docente.getCodigoProfesor());

        List<Map<String, Object>> materiasView = new ArrayList<>();
        for(PeriodoAcademico p : periodos) {
            Materia m = Materia.findFirst("cod_materia = ?", p.getCodMateria());
            if(m != null) {
                Map<String, Object> mv = new HashMap<>();
                mv.put("codMateria", m.getCodMateria());
                mv.put("nombre", m.getNombre());
                mv.put("fecha", p.getFecha());
                materiasView.add(mv);
            }
        }

        String codMateriaStr = req.queryParams("cod_materia");
        String fechaFiltro   = req.queryParams("fecha");
        List<Map<String, Object>> alumnosView = new ArrayList<>();

        if(codMateriaStr != null && !codMateriaStr.isEmpty()) {
            int codMateria = Integer.parseInt(codMateriaStr);
            PeriodoAcademico perm = PeriodoAcademico.findFirst(
                "codigo_profesor = ? AND cod_materia = ?", docente.getCodigoProfesor(), codMateria);
            if(perm == null) {
                res.redirect("/docente/alumnos?errorMessage=No tenés permiso para esa materia.");
                return null;
            }

            List<Estado> estados = Estado.where("cod_materia = ?", codMateria);
            if(fechaFiltro != null && !fechaFiltro.isEmpty()) {
                List<Estado> filtrados = new ArrayList<>();
                for(Estado e : estados) {
                    PeriodoAcademico pa = PeriodoAcademico.findFirst(
                        "cod_materia = ? AND codigo_profesor = ? AND fecha LIKE ?",
                        codMateria, docente.getCodigoProfesor(), fechaFiltro + "%");
                    if(pa != null) filtrados.add(e);
                }
                estados = filtrados;
            }

            for(Estado e : estados) {
                Estudiante estudiante = Estudiante.findFirst("dni = ?", e.getDniEstudiante());
                Persona persona       = Persona.findFirst("dni = ?", e.getDniEstudiante());
                String estadoStr = e.getString("estado");
                String estadoClass;
                if("APROBADO".equals(estadoStr))     estadoClass = "bg-green-100 text-green-700";
                else if("REGULAR".equals(estadoStr)) estadoClass = "bg-blue-100 text-blue-700";
                else if("LIBRE".equals(estadoStr))   estadoClass = "bg-red-100 text-red-700";
                else                                 estadoClass = "bg-yellow-100 text-yellow-700";

                Map<String, Object> av = new HashMap<>();
                av.put("dni",        e.getDniEstudiante());
                av.put("nroLegajo",  estudiante != null ? estudiante.getNroLegajo() : "-");
                av.put("nombre",     persona    != null ? persona.getNombre()       : "");
                av.put("apellido",   persona    != null ? persona.getApellido()     : "");
                av.put("email",      estudiante != null ? estudiante.getEmail()     : "");
                av.put("estado",     estadoStr);
                av.put("esInscripto","INSCRIPTO".equals(estadoStr));
                av.put("codMateria", codMateria);
                av.put("estadoClass",estadoClass);
                alumnosView.add(av);
            }
        }

        List<String> fechasUnicas = new ArrayList<>();
        for(PeriodoAcademico p : periodos) {
            String fecha = p.getFecha();
            if(fecha != null && fecha.length() >= 4) {
                String anio = fecha.substring(0, 4);
                if(!fechasUnicas.contains(anio)) fechasUnicas.add(anio);
            }
        }
        List<Map<String, Object>> fechasView = new ArrayList<>();
        for(String f : fechasUnicas) {
            Map<String, Object> fv = new HashMap<>();
            fv.put("fecha", f);
            fechasView.add(fv);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("materias",     materiasView);
        model.put("sinMaterias",  materiasView.isEmpty());
        model.put("alumnos",      alumnosView);
        model.put("sinAlumnos",   alumnosView.isEmpty());
        model.put("mostrarTabla", codMateriaStr != null && !codMateriaStr.isEmpty());
        model.put("fechas",       fechasView);
        model.put("successMessage", req.queryParams("successMessage"));
        model.put("errorMessage",   req.queryParams("errorMessage"));

        return new ModelAndView(model, "docente/alumnosInscriptos.mustache");
    }

    // POST /docente/alumnos/estadoCursada
    public Object handleEstadoCursada(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Docente docente = Docente.findFirst("user_id = ?", userId);
        if(docente == null) { res.redirect("/dashboard?error=No se encontró el perfil de docente."); return null; }

        String dniEstudianteStr = req.queryParams("dni_estudiante");
        String codMateriaStr    = req.queryParams("cod_materia");
        String nuevoEstado      = req.queryParams("nuevo_estado");

        if(dniEstudianteStr == null || dniEstudianteStr.isEmpty()
        || codMateriaStr == null    || codMateriaStr.isEmpty()
        || nuevoEstado == null      || nuevoEstado.isEmpty()) {
            res.redirect("/docente/alumnos?errorMessage=Datos incompletos."); return null;
        }
        if(!"REGULAR".equals(nuevoEstado) && !"LIBRE".equals(nuevoEstado)) {
            res.redirect("/docente/alumnos?errorMessage=Estado inválido."); return null;
        }

        int dniEstudiante = Integer.parseInt(dniEstudianteStr);
        int codMateria    = Integer.parseInt(codMateriaStr);

        PeriodoAcademico perm = PeriodoAcademico.findFirst(
            "codigo_profesor = ? AND cod_materia = ?", docente.getCodigoProfesor(), codMateria);
        if(perm == null) { res.redirect("/docente/alumnos?errorMessage=No tenés permiso para esa materia."); return null; }

        Estado estadoActual = Estado.findFirst("dni_estudiante = ? AND cod_materia = ?", dniEstudiante, codMateria);
        if(estadoActual == null) { res.redirect("/docente/alumnos?errorMessage=No se encontró el estado del alumno."); return null; }
        if(!"INSCRIPTO".equals(estadoActual.getString("estado"))) {
            res.redirect("/docente/alumnos?errorMessage=Solo se puede cambiar el estado de alumnos INSCRIPTOS."); return null;
        }

        try {
            Base.openTransaction();
            Base.exec("UPDATE Estado SET estado = ? WHERE dni_estudiante = ? AND cod_materia = ?", nuevoEstado, dniEstudiante, codMateria);
            Base.commitTransaction();
            String msg = "REGULAR".equals(nuevoEstado) ? "Alumno marcado como Regular correctamente." : "Alumno marcado como Libre correctamente.";
            AuditoriaService.registrarAuditoria(req, "CAMBIAR_ESTADO_CURSADA",
                "dni:" + dniEstudiante + " codMateria:" + codMateria + " nuevoEstado:" + nuevoEstado);
            res.redirect("/docente/alumnos?cod_materia=" + codMateria + "&successMessage=" + java.net.URLEncoder.encode(msg, "UTF-8"));
        } catch (Exception e) {
            Base.rollbackTransaction();
            res.redirect("/docente/alumnos?errorMessage=Error al actualizar el estado: " + e.getMessage());
        }
        return null;
    }
}