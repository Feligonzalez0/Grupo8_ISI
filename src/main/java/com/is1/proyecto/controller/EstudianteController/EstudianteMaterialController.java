package com.is1.proyecto.controller.EstudianteController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.models.Estado;
import com.is1.proyecto.models.Estudiante;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.MaterialEstudio;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public class EstudianteMaterialController {

    // GET /estudiante/material
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
            Materia materia = Materia.findFirst("cod_materia = ?", e.getCodMateria());
            for(MaterialEstudio mat : (List<MaterialEstudio>)(List<?>)MaterialEstudio.where("cod_materia = ?", e.getCodMateria())) {
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
        model.put("materiales", materiales);
        model.put("sinMateriales", materiales.isEmpty());

        return new ModelAndView(model, "estudiante/materialEstudio.mustache");
    }
}