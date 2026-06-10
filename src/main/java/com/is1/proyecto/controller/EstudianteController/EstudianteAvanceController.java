package com.is1.proyecto.controller.EstudianteController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.models.Estado;
import com.is1.proyecto.models.Estudiante;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.Persona;
import com.is1.proyecto.models.PlanDeEstudios;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public class EstudianteAvanceController 
{

    // GET /estudiante/avance
    public ModelAndView showAvance(Request req, Response res) 
    {
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

            String estadoStr = e.getString("estado");
            String estadoClass;

            if("APROBADO".equals(estadoStr)) {
                estadoClass = "bg-green-100 text-green-700";
            }else if("REGULAR".equals(estadoStr)){
                estadoClass = "bg-blue-100 text-blue-700";
            }else if("LIBRE".equals(estadoStr)){
                estadoClass = "bg-red-100 text-red-700";
            }else{
                estadoClass = "bg-yellow-100 text-yellow-700";
            }                                      

            Map<String, Object> mv = new HashMap<>();
            mv.put("nombre", materia.getNombre());
            mv.put("codMateria", materia.getCodMateria());
            mv.put("estado", estadoStr);
            mv.put("estadoClass", estadoClass);

            if("APROBADO".equals(estadoStr)) {
                aprobadas.add(mv);
            }else{
                pendientes.add(mv);
            }                         
        }

        int totalMaterias = 0;
        if(!estados.isEmpty()) {
            Materia primera = Materia.findFirst("cod_materia = ?", estados.get(0).getCodMateria());
            if(primera != null) {
                PlanDeEstudios plan = PlanDeEstudios.findFirst("cod_plan = ?", primera.getCodPlan());
                if(plan != null) {
                    totalMaterias = plan.getCantidadMaterias();
                }
                    
            }
        }

        int cantAprobadas = aprobadas.size();
        double porcentaje;

        if (totalMaterias > 0) {
            porcentaje = Math.round((cantAprobadas * 100.0 / totalMaterias) * 10.0) / 10.0;
        } else {
            porcentaje = 0.0;
        }

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
}
