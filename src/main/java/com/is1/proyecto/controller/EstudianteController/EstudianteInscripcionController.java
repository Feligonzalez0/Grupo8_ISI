package com.is1.proyecto.controller.EstudianteController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.models.Correlatividad;
import com.is1.proyecto.models.Estado;
import com.is1.proyecto.models.Estudiante;
import com.is1.proyecto.models.InscripcionCarrera;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.Persona;
import com.is1.proyecto.models.PlanDeEstudios;
import com.is1.proyecto.services.AuditoriaService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public class EstudianteInscripcionController {

    // GET /estudiante/inscripcion
    public ModelAndView showInscripcion(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
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
        List<Integer> codMateriasAprobadas = new ArrayList<>();

        for(Estado e : estadosActuales) {
            codMateriasInscriptas.add(e.getCodMateria());
            if("APROBADO".equals(e.getString("estado")))
                codMateriasAprobadas.add(e.getCodMateria());
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

            if(codMateriasInscriptas.contains(codMat)){
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

            for(Correlatividad c : correlativas){
                if(!codMateriasAprobadas.contains(c.getCodCorrelativa())){
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

    // POST /estudiante/inscripcion
    public Object handleInscripcion(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
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

        if(Estado.findFirst("dni_estudiante = ? AND cod_materia = ?", estudiante.getDni(), codMateria) != null) {
            res.redirect("/estudiante/inscripcion?errorMessage=Ya estás inscripto en esa materia.");
            return null;
        }

        List<Correlatividad> correlativas = Correlatividad.where("cod_materia = ?", codMateria);
        if(!correlativas.isEmpty()) {
            List<Estado> aprobadas = Estado.where("dni_estudiante = ? AND estado = 'APROBADO'", estudiante.getDni());
            List<Integer> codAprobadas = new ArrayList<>();
            for(Estado e : aprobadas) codAprobadas.add(e.getCodMateria());

            List<String> faltantes = new ArrayList<>();
            for(Correlatividad c : correlativas) {
                if(!codAprobadas.contains(c.getCodCorrelativa())) {
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
}