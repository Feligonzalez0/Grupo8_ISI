package com.is1.proyecto.routes;

import static spark.Spark.get;
import static spark.Spark.post;

import com.is1.proyecto.controller.AdminController;
import spark.template.mustache.MustacheTemplateEngine;

public class AdminRoutes {

    public static void register(AdminController controller, MustacheTemplateEngine engine) {
        // === DASHBOARD ===
        get("/admin", (req, res) -> controller.mostrarAdminDashboard(req, res), engine);
        
        // === DOCENTES ===
        get("/admin/docentes", (req, res) -> controller.mostrarDocentes(req, res), engine);
        get("/admin/docentes/listado", (req, res) -> controller.mostrarListadoDocentes(req, res), engine);
        get("/admin/docentes/agregar", (req, res) -> controller.mostrarFormularioAgregarDocente(req, res), engine);
        post("/admin/docentes/new", (req, res) -> controller.procesarCrearDocente(req, res));
        get("/admin/docentes/:id/edit", (req, res) -> controller.mostrarFormularioEditarDocente(req, res), engine);
        post("/admin/docentes/:id/edit", (req, res) -> controller.procesarEditarDocente(req, res));
        get("/admin/docentes/:id/delete", (req, res) -> controller.mostrarConfirmacionEliminarDocente(req, res), engine);
        post("/admin/docentes/:id/delete", (req, res) -> controller.procesarEliminarDocente(req, res));
        get("/admin/docentes/:id/materias", (req, res) -> controller.mostrarMateriasDocente(req, res), engine);
        post("/admin/docentes/:id/materias/agregar", (req, res) -> controller.procesarAsignarMateria(req, res));
        post("/admin/docentes/:id/materias/:asignacionId/delete", (req, res) -> controller.procesarQuitarMateria(req, res));

        // === ESTUDIANTES ===
        get("/admin/estudiantes", (req, res) -> controller.mostrarEstudiantes(req, res), engine);
        get("/admin/estudiantes/listado", (req, res) -> controller.mostrarListadoEstudiantes(req, res), engine);
        get("/admin/estudiantes/agregar", (req, res) -> controller.mostrarFormularioAgregarEstudiante(req, res), engine);
        post("/admin/estudiantes/new", (req, res) -> controller.procesarCrearEstudiante(req, res));
        get("/admin/estudiantes/:id/edit", (req, res) -> controller.mostrarFormularioEditarEstudiante(req, res), engine);
        post("/admin/estudiantes/:id/edit", (req, res) -> controller.procesarEditarEstudiante(req, res));
        get("/admin/estudiantes/:id/delete", (req, res) -> controller.mostrarConfirmacionEliminarEstudiante(req, res), engine);
        post("/admin/estudiantes/:id/delete", (req, res) -> controller.procesarEliminarEstudiante(req, res));
        get("/admin/estudiantes/:legajo/materias", (req, res) -> controller.mostrarMateriasParaInscribir(req, res), engine);
        post("/admin/estudiantes/:legajo/inscribir", (req, res) -> controller.procesarInscribirMateria(req, res));
        
        // === PLANES DE ESTUDIO ===
        get("/admin/planes", (req, res) -> controller.mostrarPlanes(req, res), engine);
        get("/admin/planes/agregar", (req, res) -> controller.mostrarFormularioAgregarPlan(req, res), engine);
        post("/admin/planes/agregar", (req, res) -> controller.procesarCrearPlan(req, res));
        get("/admin/planes/:id/edit", (req, res) -> controller.mostrarFormularioEditarPlan(req, res), engine);
        post("/admin/planes/:id/edit", (req, res) -> controller.procesarEditarPlan(req, res));
        get("/admin/planes/:id/delete", (req, res) -> controller.mostrarConfirmacionEliminarPlan(req, res), engine);
        post("/admin/planes/:id/delete", (req, res) -> controller.procesarEliminarPlan(req, res));
        get("/admin/planes/:id/materias", (req, res) -> controller.mostrarMateriasDelPlan(req, res), engine);

        // === CARRERAS ===
        get("/admin/carreras", (req, res) -> controller.mostrarCarreras(req, res), engine);
        get("/admin/carreras/agregar", (req, res) -> controller.mostrarFormularioAgregarCarrera(req, res), engine);
        post("/admin/carreras/agregar", (req, res) -> controller.procesarCrearCarrera(req, res));
        get("/admin/carreras/:id/edit", (req, res) -> controller.mostrarFormularioEditarCarrera(req, res), engine);
        post("/admin/carreras/:id/edit", (req, res) -> controller.procesarEditarCarrera(req, res));
        get("/admin/carreras/:id/delete", (req, res) -> controller.mostrarConfirmacionEliminarCarrera(req, res), engine);
        post("/admin/carreras/:id/delete", (req, res) -> controller.procesarEliminarCarrera(req, res));
        
        // === MATERIAS ===
        get("/admin/materias", (req, res) -> controller.mostrarMaterias(req, res), engine);
        get("/admin/materias/listado", (req, res) -> controller.mostrarListadoMaterias(req, res), engine);
        get("/admin/materias/agregar", (req, res) -> controller.mostrarFormularioAgregarMateria(req, res), engine);
        post("/admin/materias/agregar", (req, res) -> controller.procesarCrearMateria(req, res));
        get("/admin/materias/:id/edit", (req, res) -> controller.mostrarFormularioEditarMateria(req, res), engine);
        post("/admin/materias/:id/edit", (req, res) -> controller.procesarEditarMateria(req, res));
        get("/admin/materias/:id/delete", (req, res) -> controller.mostrarConfirmacionEliminarMateria(req, res), engine);
        post("/admin/materias/:id/delete", (req, res) -> controller.procesarEliminarMateria(req, res));
    }
}