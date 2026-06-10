package com.is1.proyecto.routes;

import com.is1.proyecto.controller.EstudianteController.EstudianteAvanceController;
import com.is1.proyecto.controller.EstudianteController.EstudianteCarreraController;
import com.is1.proyecto.controller.EstudianteController.EstudianteDashboardController;
import com.is1.proyecto.controller.EstudianteController.EstudianteExamenController;
import com.is1.proyecto.controller.EstudianteController.EstudianteInscripcionController;
import com.is1.proyecto.controller.EstudianteController.EstudianteMaterialController;
            
import static spark.Spark.get;
import static spark.Spark.post;
import spark.template.mustache.MustacheTemplateEngine;

public class EstudianteRoutes 
{

    public static void register(MustacheTemplateEngine engine) 
    {
        EstudianteDashboardController dashboard = new EstudianteDashboardController();
        EstudianteInscripcionController inscripcion = new EstudianteInscripcionController();
        EstudianteCarreraController carrera = new EstudianteCarreraController();
        EstudianteAvanceController avance = new EstudianteAvanceController();
        EstudianteExamenController examenes = new EstudianteExamenController();
        EstudianteMaterialController material = new EstudianteMaterialController();

        get("/estudiante/dashboard", dashboard::showDashboard, engine);
        get("/estudiante/inscripcion", inscripcion::showInscripcion, engine);
        post("/estudiante/inscripcion", inscripcion::handleInscripcion);
        get("/estudiante/carrera", carrera::showCarrera, engine);
        post("/estudiante/carrera/inscribir", carrera::handleInscribirCarrera);
        get("/estudiante/avance", avance::showAvance, engine);
        get("/estudiante/examenes", examenes::showExamenes, engine);
        post("/estudiante/examenes/:id/inscribir",examenes::handleInscribirExamen);
        get("/estudiante/material", material::showMaterial, engine);
    }
}