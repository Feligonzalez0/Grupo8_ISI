package com.is1.proyecto.routes;
import com.is1.proyecto.controller.EstudianteController;
import spark.template.mustache.MustacheTemplateEngine;
import static spark.Spark.get;
import static spark.Spark.post;

public class EstudianteRoutes {
    public static void register(EstudianteController controller, MustacheTemplateEngine engine) {

        get("/estudiante/dashboard", (req, res) -> controller.showDashboard(req, res), engine);
        get("/estudiante/inscripcion", (req, res) -> controller.showInscripcion(req, res), engine);
        post("/estudiante/inscripcion", (req, res) -> controller.handleInscripcion(req, res));
        get("/estudiante/examenes", (req, res) -> controller.showExamenes(req, res), engine);
        post("/estudiante/examenes/:id/inscribir", (req, res) -> controller.handleInscribirExamen(req, res));
        get("/estudiante/carrera", (req, res) -> controller.showCarrera(req, res), engine);
        post("/estudiante/carrera/inscribir", (req, res) -> controller.handleInscribirCarrera(req, res));
        get("/estudiante/avance", (req, res) -> controller.showAvance(req, res), engine);
        get("/estudiante/material", (req, res) -> controller.showMaterial(req, res), engine);

    }
}
