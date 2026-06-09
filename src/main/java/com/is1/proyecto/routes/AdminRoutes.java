package com.is1.proyecto.routes;

import static spark.Spark.get;
import static spark.Spark.post;

import com.is1.proyecto.controller.AdminController;
import spark.template.mustache.MustacheTemplateEngine;

public class AdminRoutes {

    public static void register(AdminController controller, MustacheTemplateEngine engine) {
        get("/admin",(req, res) -> controller.mostrarAdminDashboard(req, res), engine);
        
        // /admin/docentes
        get("/admin/docentes", (req, res) -> controller.mostrarDocentes(req, res), engine);
        get("/admin/docentes/listado", (req, res) -> controller.mostrarListadoDocentes(req, res), engine);
        get("/admin/docentes/agregar", (req, res) -> controller.mostrarFormularioAgregarDocente(req, res), engine);
        post("/docente/new", (req, res) -> controller.procesarCrearDocente(req, res));
        get("/admin/docentes/:id/edit", (req, res) -> controller.mostrarFormularioEditarDocente(req, res), engine);
        post("/admin/docentes/:id/edit", (req, res) -> controller.procesarEditarDocente(req, res));
        get("/admin/docentes/:id/delete", (req, res) -> controller.mostrarConfirmacionEliminarDocente(req, res), engine);
        post("/admin/docentes/:id/delete", (req, res) -> controller.procesarEliminarDocente(req, res));
        get("/admin/docentes/:id/materias", (req, res) -> controller.mostrarMateriasDocente(req, res), engine);
        post("/admin/docentes/:id/materias/agregar", (req, res) -> controller.procesarAsignarMateria(req, res));
        post("/admin/docentes/:id/materias/:asignacionId/delete", (req, res) -> controller.procesarQuitarMateria(req, res));
    }
}
