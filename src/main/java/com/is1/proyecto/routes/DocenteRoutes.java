package com.is1.proyecto.routes;
import com.is1.proyecto.controller.DocenteController;
import spark.template.mustache.MustacheTemplateEngine;
import static spark.Spark.get;
import static spark.Spark.post;

public class DocenteRoutes {
    public static void register(DocenteController controller, MustacheTemplateEngine engine) {

        get("/docente/examenes/crear", (req, res) -> controller.showCrearExamen(req, res), engine);
        post("/docente/examenes/crear", (req, res) -> controller.handleCrearExamen(req, res));
        get("/docente/notas", (req, res) -> controller.showNotas(req, res), engine);
        post("/docente/notas/cargar", (req, res) -> controller.handleCargarNota(req, res));
        get("/docente/alumnos", (req, res) -> controller.showAlumnos(req, res), engine);
        post("/docente/alumnos/estadoCursada", (req, res) -> controller.handleEstadoCursada(req, res));
        post("/docente/material/subir", (req, res) -> controller.handleSubirMaterial(req, res));
        get("/material/descargar/:id", (req, res) -> controller.handleDescargarMaterial(req, res));
        get("/docente/material", (req, res) -> controller.showMaterial(req, res), engine);

    }
}
