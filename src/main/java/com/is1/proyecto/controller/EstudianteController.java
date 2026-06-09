package com.is1.proyecto.controller;
 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
 
import com.is1.proyecto.models.Carrera;
import com.is1.proyecto.models.Correlatividad;
import com.is1.proyecto.models.Estado;
import com.is1.proyecto.models.Estudiante;
import com.is1.proyecto.models.ExamenFinal;
import com.is1.proyecto.models.InscripcionCarrera;
import com.is1.proyecto.models.InscripcionExamen;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.MaterialEstudio;
import com.is1.proyecto.models.Persona;
import com.is1.proyecto.models.PlanDeEstudios;
import com.is1.proyecto.models.Situacion;
import com.is1.proyecto.services.AuditoriaService;
 
import spark.ModelAndView;
import spark.Request;
import spark.Response;
 
/**
 * Controller que maneja todas las rutas del area de estudiantes (/estudiante/*).
 */
public class EstudianteController {

    // GET /estudiante/dashboard
    public ModelAndView showDashboard(Request req, Response res) {
        Integer userId  = req.session().attribute("userId");
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
        model.put("nombre", persona != null ? persona.getNombre()   : "");
        model.put("apellido", persona != null ? persona.getApellido() : "");
        model.put("nroLegajo", estudiante.getNroLegajo());

        String error = req.queryParams("error");
        if(error != null) {
            model.put("errorMessage", error);
        }

        return new ModelAndView(model, "estudiante/estudianteDashboard.mustache");
    }

    // GET /estudiante/inscripcion  — ver materias disponibles para inscribirse
    public ModelAndView showInscripcion(Request req, Response res) {
        Integer userId  = req.session().attribute("userId");
        String userRol = req.session().attribute("userRol");

        if(userId == null || !"ALUMNO".equals(userRol)) {
            res.redirect("/dashboard?error=Acceso no autorizado.");
            return null;
        }

        Estudiante estudiante = Estudiante.findFirst("user_id = ?", userId);
        if(estudiante == null) {
            res.redirect("/dashboard?error=No se encontró el perfil de estudiante.");
            return null;
        }

        Persona persona = Persona.findFirst("dni = ?", estudiante.getDni());

        List<Estado> estadosActuales = Estado.where("dni_estudiante = ?", estudiante.getDni());
        List<Integer> codMateriasInscriptas = new ArrayList<>();
        for(Estado e : estadosActuales) {
            codMateriasInscriptas.add(e.getCodMateria());
        }

        List<Integer> codMateriasAprobadas = new ArrayList<>();
        for(Estado e : estadosActuales) {
            if("APROBADO".equals(e.getString("estado"))) {
                codMateriasAprobadas.add(e.getCodMateria());
            }
        }

        InscripcionCarrera inscripcionCarrera = InscripcionCarrera.findFirst("dni_estudiante = ?", estudiante.getDni());
        if(inscripcionCarrera == null) {
            res.redirect("/dashboard?error=No estás inscripto en ninguna carrera.");
            return null;
        }

        PlanDeEstudios plan = PlanDeEstudios.findFirst("cod_carrera = ?", inscripcionCarrera.getCodCarrera());
        if(plan == null) {
            res.redirect("/dashboard?error=La carrera no tiene plan de estudios.");
            return null;
        }

        List<Materia> materiasDelPlan = Materia.where("cod_plan = ?", plan.getCod());
        List<Map<String, Object>> materiasDisponibles = new ArrayList<>();
        List<Map<String, Object>> materiasInscriptas  = new ArrayList<>();

        for(Materia m : materiasDelPlan) {
            Integer codMat = m.getCodMateria();

            if(codMateriasInscriptas.contains(codMat)) {
                Map<String, Object> mv = new HashMap<>();
                mv.put("codMateria", codMat);
                mv.put("nombre", m.getNombre());
                Estado est = Estado.findFirst("dni_estudiante = ? AND cod_materia = ?", estudiante.getDni(), codMat);
                mv.put("estado", est != null ? est.getString("estado") : "");
                materiasInscriptas.add(mv);
                continue;
            }

            boolean cumpleCorrelativas = true;
            List<String> faltantes = new ArrayList<>();

            List<Correlatividad> correlativas = Correlatividad.where("cod_materia = ?", codMat);
            for(Correlatividad c : correlativas) {
                if(!codMateriasAprobadas.contains(c.getCodCorrelativa())) {
                    cumpleCorrelativas = false;
                    Materia mc = Materia.findFirst("cod_materia = ?", c.getCodCorrelativa());
                    faltantes.add(mc != null ? mc.getNombre() : "Cód. " + c.getCodCorrelativa());
                }
            }

            Map<String, Object> mv = new HashMap<>();
            mv.put("codMateria", codMat);
            mv.put("nombre", m.getNombre());
            mv.put("descripcion", m.getDescripcion());
            mv.put("puedeInscribirse", cumpleCorrelativas);
            mv.put("tieneCorrelativas", !correlativas.isEmpty());
            mv.put("correlativasFaltantes", String.join(", ", faltantes));
            materiasDisponibles.add(mv);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("dni", estudiante.getDni());
        model.put("nroLegajo", estudiante.getNroLegajo());
        model.put("nombre", persona != null ? persona.getNombre()   : "");
        model.put("apellido", persona != null ? persona.getApellido() : "");
        model.put("materias", materiasDisponibles);
        model.put("sinMaterias", materiasDisponibles.isEmpty());
        model.put("inscriptas", materiasInscriptas);
        model.put("tieneInscriptas", !materiasInscriptas.isEmpty());
        model.put("successMessage", req.queryParams("successMessage"));
        model.put("errorMessage", req.queryParams("errorMessage"));

        return new ModelAndView(model, "estudiante/inscripcion.mustache");
    }

    // POST /estudiante/inscripcion  — inscribirse a una materia
    public ModelAndView handleInscripcion(Request req, Response res) {
        Integer userId  = req.session().attribute("userId");
        String userRol = req.session().attribute("userRol");

        if(userId == null || !"ALUMNO".equals(userRol)) {
            res.redirect("/dashboard?error=Acceso no autorizado.");
            return null;
        }

        Estudiante estudiante = Estudiante.findFirst("user_id = ?", userId);
        if(estudiante == null) {
            res.redirect("/dashboard?error=No se encontró el perfil de estudiante.");
            return null;
        }

        String codMateriaStr = req.queryParams("cod_materia");
        if(codMateriaStr == null || codMateriaStr.isEmpty()) {
            res.redirect("/estudiante/inscripcion?errorMessage=Debe seleccionar una materia.");
            return null;
        }

        int codMateria = Integer.parseInt(codMateriaStr);
        Materia materia = Materia.findFirst("cod_materia = ?", codMateria);
        if(materia == null) {
            res.redirect("/estudiante/inscripcion?errorMessage=La materia no existe.");
            return null;
        }

        Estado yaInscripto = Estado.findFirst("dni_estudiante = ? AND cod_materia = ?", estudiante.getDni(), codMateria);
        if(yaInscripto != null) {
            res.redirect("/estudiante/inscripcion?errorMessage=Ya estás inscripto en esa materia.");
            return null;
        }

        List<Correlatividad> correlativas = Correlatividad.where("cod_materia = ?", codMateria);
        if(!correlativas.isEmpty()) {
            List<Estado> aprobadas = Estado.where("dni_estudiante = ? AND estado = 'APROBADO'", estudiante.getDni());
            List<Integer> codAprobadas = new ArrayList<>();
            for(Estado e : aprobadas) {
                codAprobadas.add(e.getCodMateria());
            }

            List<String> faltantes = new ArrayList<>();
            for(Correlatividad c : correlativas) {
                if (!codAprobadas.contains(c.getCodCorrelativa())) {
                    Materia mc = Materia.findFirst("cod_materia = ?", c.getCodCorrelativa());
                    faltantes.add(mc != null ? mc.getNombre() : "Cód. " + c.getCodCorrelativa());
                }
            }

            if(!faltantes.isEmpty()) {
                String msg = "No cumplís las correlatividades. Te falta aprobar: " + String.join(", ", faltantes);

                try {
                    res.redirect("/estudiante/inscripcion?errorMessage=" + java.net.URLEncoder.encode(msg, "UTF-8"));
                } catch (Exception ex) {
                    res.redirect("/estudiante/inscripcion?errorMessage=Correlativas incompletas.");
                }

                return null;
            }
        }

        try {
            estudiante.inscribirseMateria(codMateria);
            AuditoriaService.registrarAuditoria(req, "INSCRIBIR_MATERIA", "dni:" + estudiante.getDni() + " codMateria:" + codMateria);

            res.redirect("/estudiante/inscripcion?successMessage=Te inscribiste correctamente a la materia.");
        } catch (Exception e) {
            try {
                res.redirect("/estudiante/inscripcion?errorMessage=" + java.net.URLEncoder.encode(e.getMessage(), "UTF-8"));
            } catch (Exception ex) {
                res.redirect("/estudiante/inscripcion?errorMessage=Error al inscribirse.");
            }
        }

        return null;
    }
 
    // GET /estudiante/carrera  — ver carrera actual o lista para inscribirse
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
            model.put("descripcionActual", carreraActual != null ? carreraActual.getDescripcion() : "");
            model.put("situacionCarrera", inscripcion.getSituacion() != null ? inscripcion.getSituacion().name() : "");
            model.put("esIngresante", Situacion.INGRESANTE.equals(inscripcion.getSituacion()));
            model.put("esAvanzado", Situacion.AVANZADO.equals(inscripcion.getSituacion()));
        } else {
            List<Map<String, Object>> carreras = new ArrayList<>();

            List<Carrera> carrerasDB = Carrera.findAll();
            for(Carrera c : carrerasDB) {
                Map<String, Object> cv = new HashMap<>();
                cv.put("codCarrera", c.getCodigo());
                cv.put("nombre", c.getNombre());
                cv.put("descripcion", c.getDescripcion() != null ? c.getDescripcion() : "");
                carreras.add(cv);
            }

            model.put("yaInscripto", false);
            model.put("carreras",    carreras);
            model.put("sinCarreras", carreras.isEmpty());
        }

        String success = req.queryParams("successMessage");
        String error = req.queryParams("errorMessage");
        if (success != null) model.put("successMessage", success);
        if (error != null) model.put("errorMessage", error);

        return new ModelAndView(model, "estudiante/inscripcionCarrera.mustache");
    }

    // POST /estudiante/carrera/inscribir
    public ModelAndView handleInscribirCarrera(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Estudiante estudiante = Estudiante.findFirst("user_id = ?", userId);

        if(estudiante == null) {
            res.redirect("/dashboard?error=No se encontró el perfil de estudiante.");
            return null;
        }

        InscripcionCarrera inscripcionExistente = InscripcionCarrera.findFirst("dni_estudiante = ?", estudiante.getDni());
        if(inscripcionExistente != null) {
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
        if (carrera == null) {
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

    // GET /estudiante/avance  — avance academico
    public ModelAndView showAvance(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Estudiante estudiante = Estudiante.findFirst("user_id = ?", userId);

        if(estudiante == null) {
            res.redirect("/dashboard?error=No se encontró el perfil de estudiante.");
            return null;
        }

        Persona persona = Persona.findFirst("dni = ?", estudiante.getDni());
        List<Estado> estados = Estado.where("dni_estudiante = ?", estudiante.getDni());

        List<Map<String, Object>> aprobadas  = new ArrayList<>();
        List<Map<String, Object>> pendientes = new ArrayList<>();

        for(Estado e : estados) {
            Materia materia = Materia.findFirst("cod_materia = ?", e.getCodMateria());
            if(materia == null) continue;

            Map<String, Object> mv = new HashMap<>();
            mv.put("nombre",     materia.getNombre());
            mv.put("codMateria", materia.getCodMateria());
            mv.put("estado",     e.getString("estado"));

            String estadoClass;
            if("APROBADO".equals(e.getString("estado"))) {
                estadoClass = "bg-green-100 text-green-700";
            } else if("REGULAR".equals(e.getString("estado"))) {
                estadoClass = "bg-blue-100 text-blue-700";
            } else if("LIBRE".equals(e.getString("estado"))) {
                estadoClass = "bg-red-100 text-red-700";
            }   else    {
                estadoClass = "bg-yellow-100 text-yellow-700";
                mv.put("estadoClass", estadoClass);
            }                                                

            if("APROBADO".equals(e.getString("estado"))) {
                aprobadas.add(mv);
            }   else    {
                pendientes.add(mv);
            }
        }

        int totalMaterias = 0;
        if(!estados.isEmpty()) {
            Materia primerMateria = Materia.findFirst("cod_materia = ?", estados.get(0).getCodMateria());
            if(primerMateria != null) {
                PlanDeEstudios plan = PlanDeEstudios.findFirst("cod_plan = ?", primerMateria.getCodPlan());
                if(plan != null) {
                    totalMaterias = plan.getCantidadMaterias();
                }
            }
        }

        int cantAprobadas = aprobadas.size();
        double porcentaje = totalMaterias > 0 ? Math.round((cantAprobadas * 100.0 / totalMaterias) * 10.0) / 10.0 : 0.0;

        Map<String, Object> model = new HashMap<>();
        model.put("nombre", persona != null ? persona.getNombre()   : "");
        model.put("apellido", persona != null ? persona.getApellido() : "");
        model.put("nroLegajo", estudiante.getNroLegajo());
        model.put("aprobadas", aprobadas);
        model.put("pendientes", pendientes);
        model.put("cantAprobadas", cantAprobadas);
        model.put("totalMaterias", totalMaterias);
        model.put("porcentaje", porcentaje);
        model.put("sinEstados", estados.isEmpty());

        return new ModelAndView(model, "estudiante/avanceAcademico.mustache");
    }

    // GET /estudiante/examenes  — examenes disponibles para inscribirse
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
            List<ExamenFinal> examenes = ExamenFinal.where("cod_materia = ?", codMateria);
            Materia materia = Materia.findFirst("cod_materia = ?", codMateria);

            for(ExamenFinal ex : examenes) {
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

        String success = req.queryParams("successMessage");
        String error = req.queryParams("errorMessage");
        if(success != null) {
            model.put("successMessage", success);
        }
        if(error != null) {
            model.put("errorMessage", error);
        }

        return new ModelAndView(model, "estudiante/examenesDisponibles.mustache");
    }

    // POST /estudiante/examenes/:id/inscribir
    public ModelAndView handleInscribirExamen(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Estudiante estudiante = Estudiante.findFirst("user_id = ?", userId);

        if(estudiante == null) {
            res.redirect("/dashboard");
            return null;
        }

        Integer idExamen = Integer.parseInt(req.params(":id"));
        ExamenFinal examen = ExamenFinal.findById(idExamen);

        if(examen == null) {
            res.redirect("/estudiante/examenes?errorMessage=Examen no encontrado.");
            return null;
        }

        Estado estado = Estado.findFirst("dni_estudiante = ? AND cod_materia = ? AND estado = ?", estudiante.getDni(), examen.getCodMateria(), "REGULAR");
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

    // GET /estudiante/material  — materiales de las materias en las que está inscripto
    public ModelAndView showMaterial(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Estudiante estudiante = Estudiante.findFirst("user_id = ?", userId);

        if(estudiante == null) {
            res.redirect("/dashboard?error=No se encontró el perfil de estudiante.");
            return null;
        }

        List<Estado> estados = Estado.where("dni_estudiante = ?", estudiante.getDni());
        List<Map<String, Object>> materiales = new ArrayList<>();

        for(Estado e : estados) {
            List<MaterialEstudio> matsDB = MaterialEstudio.where("cod_materia = ?", e.getCodMateria());
            Materia materia = Materia.findFirst("cod_materia = ?", e.getCodMateria());

            for(MaterialEstudio mat : matsDB) {
                Map<String, Object> mv = new HashMap<>();
                mv.put("id", mat.getId());
                mv.put("nombre", mat.getNombre());
                mv.put("descripcion", mat.getDescripcion());
                mv.put("nombreArchivo", mat.getNombreArchivo());
                mv.put("fechaSubida", mat.getFechaSubida());
                mv.put("nombreMateria", materia != null ? materia.getNombre() : "Sin materia");
                materiales.add(mv);
            }
        }

        Map<String, Object> model = new HashMap<>();
        model.put("materiales",    materiales);
        model.put("sinMateriales", materiales.isEmpty());

        return new ModelAndView(model, "estudiante/materialEstudio.mustache");
    }
}
