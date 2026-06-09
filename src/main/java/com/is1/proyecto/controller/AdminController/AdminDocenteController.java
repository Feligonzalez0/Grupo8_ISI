package com.is1.proyecto.controller.AdminController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.services.adminService.AdminDocenteService;
import com.is1.proyecto.services.AuditoriaService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public class AdminDocenteController extends AdminBaseController {
    
    private final AdminDocenteService docenteService;
    
    public AdminDocenteController() {
        this.docenteService = new AdminDocenteService();
    }
    
    // GET /admin/docentes
    public ModelAndView listar(Request req, Response res) {
        List<Map<String, Object>> docentes = AdminDocenteService.listarDocentes();
        Map<String, Object> model = new HashMap<>();
        model.put("docentes", docentes);
        agregarMensajes(req, model);
        return new ModelAndView(model, "admin/docentes/docentesDashboard.mustache");
    }
    
    // GET /admin/docentes/listado
    public ModelAndView listadoCompleto(Request req, Response res) {
        List<Map<String, Object>> docentes = AdminDocenteService.listarDocentes();
        Map<String, Object> model = new HashMap<>();
        model.put("docentes", docentes);
        agregarMensajes(req, model);
        return new ModelAndView(model, "admin/docentes/listadoDocentes.mustache");
    }
    
    // GET /admin/docentes/agregar
    public ModelAndView mostrarFormularioAgregar(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        model.put("dni", "");
        model.put("nombre", "");
        model.put("apellido", "");
        model.put("email", "");
        model.put("fecha_nacimiento", "");
        model.put("telefono", "");
        model.put("direccion", "");
        model.put("username", "");
        agregarMensajes(req, model);
        return new ModelAndView(model, "admin/docentes/agregarDocente.mustache");
    }
    
    // POST /admin/docentes/new
    public Object crear(Request req, Response res) {
        try {
            docenteService.crearDocente(
                req.queryParams("nombre"),
                req.queryParams("apellido"),
                req.queryParams("dni"),
                req.queryParams("email"),
                req.queryParams("fecha_nacimiento"),
                req.queryParams("telefono"),
                req.queryParams("direccion"),
                req.queryParams("username")
            );
            AuditoriaService.registrarAuditoria(req, "CREAR_DOCENTE", 
                "DNI: " + req.queryParams("dni"));
            return redirectWithSuccess(res, "/admin/docentes/agregar", "Docente agregado correctamente.");
        } catch (IllegalArgumentException e) {
            return redirectWithError(res, "/admin/docentes/agregar", e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Error al crear docente: {}", e.getMessage());
            return redirectWithError(res, "/admin/docentes/agregar", e.getMessage());
        }
    }
    
    // GET /admin/docentes/:id/edit
    public ModelAndView mostrarFormularioEditar(Request req, Response res) {
        Integer codigoProfesor = Integer.parseInt(req.params(":id"));
        Map<String, Object> vista = docenteService.obtenerDocenteParaVista(codigoProfesor);
        if (vista == null) {
            res.redirect("/admin/docentes?errorMessage=Docente no encontrado.");
            return null;
        }
        agregarMensajes(req, vista);
        return new ModelAndView(vista, "admin/docentes/editarDocente.mustache");
    }
    
    // POST /admin/docentes/:id/edit
    public Object editar(Request req, Response res) {
        Integer codigoProfesor = Integer.parseInt(req.params(":id"));
        try {
            docenteService.editarDocente(
                codigoProfesor,
                req.queryParams("nombre"),
                req.queryParams("apellido"),
                req.queryParams("fecha_nacimiento"),
                req.queryParams("telefono"),
                req.queryParams("direccion"),
                req.queryParams("email")
            );
            AuditoriaService.registrarAuditoria(req, "EDITAR_DOCENTE", "Código: " + codigoProfesor);
            return redirectWithSuccess(res, "/admin/docentes", "Docente actualizado correctamente.");
        } catch (IllegalArgumentException e) {
            return redirectWithError(res, "/admin/docentes", e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Error al editar docente: {}", e.getMessage());
            return redirectWithError(res, "/admin/docentes/" + codigoProfesor + "/edit", "Error al actualizar.");
        }
    }
    
    // GET /admin/docentes/:id/delete
    public ModelAndView mostrarConfirmacionEliminar(Request req, Response res) {
        Integer codigoProfesor = Integer.parseInt(req.params(":id"));
        Map<String, Object> vista = docenteService.obtenerDocenteParaVista(codigoProfesor);
        if (vista == null) {
            res.redirect("/admin/docentes?errorMessage=Docente no encontrado.");
            return null;
        }
        return new ModelAndView(vista, "admin/docentes/eliminarDocente.mustache");
    }
    
    // POST /admin/docentes/:id/delete
    public Object eliminar(Request req, Response res) {
        Integer codigoProfesor = Integer.parseInt(req.params(":id"));
        try {
            docenteService.eliminarDocente(codigoProfesor);
            AuditoriaService.registrarAuditoria(req, "ELIMINAR_DOCENTE", "Código: " + codigoProfesor);
            return redirectWithSuccess(res, "/admin/docentes", "Docente eliminado correctamente.");
        } catch (IllegalArgumentException e) {
            return redirectWithError(res, "/admin/docentes", e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Error al eliminar docente: {}", e.getMessage());
            return redirectWithError(res, "/admin/docentes", "Error al eliminar docente.");
        }
    }
    
    // GET /admin/docentes/:id/materias
    public ModelAndView mostrarMaterias(Request req, Response res) {
        Integer codigoProfesor = Integer.parseInt(req.params(":id"));
        Map<String, Object> model = docenteService.obtenerVistaMateriasDocente(codigoProfesor);
        if (model == null) {
            res.redirect("/admin/docentes?errorMessage=Docente no encontrado.");
            return null;
        }
        agregarMensajes(req, model);
        return new ModelAndView(model, "admin/docentes/materiasDocente.mustache");
    }
    
    // POST /admin/docentes/:id/materias/agregar
    public Object asignarMateria(Request req, Response res) {
        Integer codigoProfesor = Integer.parseInt(req.params(":id"));
        try {
            docenteService.asignarMateria(
                codigoProfesor,
                req.queryParams("cod_materia"),
                req.queryParams("fecha"),
                req.queryParams("cargo")
            );
            AuditoriaService.registrarAuditoria(req, "ASIGNAR_MATERIA_DOCENTE", "Profesor: " + codigoProfesor);
            return redirectWithSuccess(res, "/admin/docentes/" + codigoProfesor + "/materias", "Materia asignada correctamente.");
        } catch (IllegalArgumentException e) {
            return redirectWithError(res, "/admin/docentes/" + codigoProfesor + "/materias", e.getMessage());
        }
    }
    
    // POST /admin/docentes/:id/materias/:asignacionId/delete
    public Object quitarMateria(Request req, Response res) {
        Integer codigoProfesor = Integer.parseInt(req.params(":id"));
        Integer asignacionId = Integer.parseInt(req.params(":asignacionId"));
        try {
            docenteService.quitarMateria(codigoProfesor, asignacionId);
            AuditoriaService.registrarAuditoria(req, "QUITAR_MATERIA_DOCENTE", "Profesor: " + codigoProfesor);
            return redirectWithSuccess(res, "/admin/docentes/" + codigoProfesor + "/materias", "Materia quitada correctamente.");
        } catch (Exception e) {
            return redirectWithError(res, "/admin/docentes/" + codigoProfesor + "/materias", "Error al quitar materia.");
        }
    }
}