package com.is1.proyecto.routes;

import static spark.Spark.get;
import static spark.Spark.post;

import com.is1.proyecto.controller.AdminController.*;

import spark.template.mustache.MustacheTemplateEngine;

public class AdminRoutes {

    public static void register(MustacheTemplateEngine engine) {
        
        // Instanciar controllers
        AdminDashboardController dashboardController = new AdminDashboardController();
        AdminDocenteController docenteController = new AdminDocenteController();
        AdminEstudianteController estudianteController = new AdminEstudianteController();
        AdminPlanController planController = new AdminPlanController();
        AdminCarreraController carreraController = new AdminCarreraController();
        AdminMateriaController materiaController = new AdminMateriaController();
        
        // === DASHBOARD ===
        get("/admin", (req, res) -> dashboardController.mostrarDashboard(req, res), engine);
        get("/admin/auditoria", (req, res) -> dashboardController.mostrarAuditoria(req, res), engine);
        
        // === DOCENTES ===
        get("/admin/docentes", (req, res) -> docenteController.listar(req, res), engine);
        get("/admin/docentes/listado", (req, res) -> docenteController.listadoCompleto(req, res), engine);
        get("/admin/docentes/agregar", (req, res) -> docenteController.mostrarFormularioAgregar(req, res), engine);
        post("/admin/docentes/new", (req, res) -> docenteController.crear(req, res));
        get("/admin/docentes/:id/edit", (req, res) -> docenteController.mostrarFormularioEditar(req, res), engine);
        post("/admin/docentes/:id/edit", (req, res) -> docenteController.editar(req, res));
        get("/admin/docentes/:id/delete", (req, res) -> docenteController.mostrarConfirmacionEliminar(req, res), engine);
        post("/admin/docentes/:id/delete", (req, res) -> docenteController.eliminar(req, res));
        get("/admin/docentes/:id/materias", (req, res) -> docenteController.mostrarMaterias(req, res), engine);
        post("/admin/docentes/:id/materias/agregar", (req, res) -> docenteController.asignarMateria(req, res));
        post("/admin/docentes/:id/materias/:asignacionId/delete", (req, res) -> docenteController.quitarMateria(req, res));
        
        // === ESTUDIANTES ===
        get("/admin/estudiantes", (req, res) -> estudianteController.listar(req, res), engine);
        get("/admin/estudiantes/listado", (req, res) -> estudianteController.listadoCompleto(req, res), engine);
        get("/admin/estudiantes/agregar", (req, res) -> estudianteController.mostrarFormularioAgregar(req, res), engine);
        post("/admin/estudiantes/new", (req, res) -> estudianteController.crear(req, res));
        get("/admin/estudiantes/:id/edit", (req, res) -> estudianteController.mostrarFormularioEditar(req, res), engine);
        post("/admin/estudiantes/:id/edit", (req, res) -> estudianteController.editar(req, res));
        get("/admin/estudiantes/:id/delete", (req, res) -> estudianteController.mostrarConfirmacionEliminar(req, res), engine);
        post("/admin/estudiantes/:id/delete", (req, res) -> estudianteController.eliminar(req, res));
        get("/admin/estudiantes/:legajo/materias", (req, res) -> estudianteController.mostrarMateriasParaInscribir(req, res), engine);
        post("/admin/estudiantes/:legajo/inscribir", (req, res) -> estudianteController.inscribirMateria(req, res));
        
        // === PLANES ===
        get("/admin/planes", (req, res) -> planController.listar(req, res), engine);
        get("/admin/planes/agregar", (req, res) -> planController.mostrarFormularioAgregar(req, res), engine);
        post("/admin/planes/agregar", (req, res) -> planController.crear(req, res));
        get("/admin/planes/:id/edit", (req, res) -> planController.mostrarFormularioEditar(req, res), engine);
        post("/admin/planes/:id/edit", (req, res) -> planController.editar(req, res));
        get("/admin/planes/:id/delete", (req, res) -> planController.mostrarConfirmacionEliminar(req, res), engine);
        post("/admin/planes/:id/delete", (req, res) -> planController.eliminar(req, res));
        get("/admin/planes/:id/materias", (req, res) -> planController.mostrarMaterias(req, res), engine);
        
        // === CARRERAS ===
        get("/admin/carreras", (req, res) -> carreraController.listar(req, res), engine);
        get("/admin/carreras/agregar", (req, res) -> carreraController.mostrarFormularioAgregar(req, res), engine);
        post("/admin/carreras/agregar", (req, res) -> carreraController.crear(req, res));
        get("/admin/carreras/:id/edit", (req, res) -> carreraController.mostrarFormularioEditar(req, res), engine);
        post("/admin/carreras/:id/edit", (req, res) -> carreraController.editar(req, res));
        get("/admin/carreras/:id/delete", (req, res) -> carreraController.mostrarConfirmacionEliminar(req, res), engine);
        post("/admin/carreras/:id/delete", (req, res) -> carreraController.eliminar(req, res));
        
        // === MATERIAS ===
        get("/admin/materias", (req, res) -> materiaController.listar(req, res), engine);
        get("/admin/materias/listado", (req, res) -> materiaController.listadoCompleto(req, res), engine);
        get("/admin/materias/agregar", (req, res) -> materiaController.mostrarFormularioAgregar(req, res), engine);
        post("/admin/materias/agregar", (req, res) -> materiaController.crear(req, res));
        get("/admin/materias/:id/edit", (req, res) -> materiaController.mostrarFormularioEditar(req, res), engine);
        post("/admin/materias/:id/edit", (req, res) -> materiaController.editar(req, res));
        get("/admin/materias/:id/delete", (req, res) -> materiaController.mostrarConfirmacionEliminar(req, res), engine);
        post("/admin/materias/:id/delete", (req, res) -> materiaController.eliminar(req, res));
    }
}