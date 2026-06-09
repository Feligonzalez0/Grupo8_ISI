package com.is1.proyecto.controller.AdminController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.services.adminService.AdminEstudianteService;
import com.is1.proyecto.services.AuditoriaService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public class AdminEstudianteController extends AdminBaseController {
    
    private final AdminEstudianteService estudianteService;
    
    public AdminEstudianteController() {
        this.estudianteService = new AdminEstudianteService();
    }
    
    // GET /admin/estudiantes
    public ModelAndView listar(Request req, Response res) {
        List<Map<String, Object>> estudiantes = AdminEstudianteService.listarEstudiantes();
        Map<String, Object> model = new HashMap<>();
        model.put("estudiantes", estudiantes);
        agregarMensajes(req, model);
        return new ModelAndView(model, "admin/estudiantes/estudiantesDashboard.mustache");
    }
    
    // GET /admin/estudiantes/listado
    public ModelAndView listadoCompleto(Request req, Response res) {
        List<Map<String, Object>> estudiantes = AdminEstudianteService.listarEstudiantes();
        Map<String, Object> model = new HashMap<>();
        model.put("estudiantes", estudiantes);
        agregarMensajes(req, model);
        return new ModelAndView(model, "admin/estudiantes/listadoEstudiantes.mustache");
    }
    
    // GET /admin/estudiantes/agregar
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
        model.put("nro_legajo", "");
        agregarMensajes(req, model);
        return new ModelAndView(model, "admin/estudiantes/agregarEstudiante.mustache");
    }
    
    // POST /admin/estudiantes/new
    public Object crear(Request req, Response res) {
        try {
            estudianteService.crearEstudiante(
                req.queryParams("nombre"),
                req.queryParams("apellido"),
                req.queryParams("dni"),
                req.queryParams("email"),
                req.queryParams("fecha_nacimiento"),
                req.queryParams("telefono"),
                req.queryParams("direccion"),
                req.queryParams("username"),
                req.queryParams("nro_legajo")
            );
            AuditoriaService.registrarAuditoria(req, "CREAR_ESTUDIANTE", 
                "DNI: " + req.queryParams("dni"));
            return redirectWithSuccess(res, "/admin/estudiantes/agregar", "Estudiante agregado correctamente.");
        } catch (IllegalArgumentException e) {
            return redirectWithError(res, "/admin/estudiantes/agregar", e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Error al crear estudiante: {}", e.getMessage());
            return redirectWithError(res, "/admin/estudiantes/agregar", e.getMessage());
        }
    }
    
    // GET /admin/estudiantes/:id/edit
    public ModelAndView mostrarFormularioEditar(Request req, Response res) {
        Integer nroLegajo = Integer.parseInt(req.params(":id"));
        Map<String, Object> vista = estudianteService.obtenerEstudianteParaVista(nroLegajo);
        if (vista == null) {
            res.redirect("/admin/estudiantes?errorMessage=Estudiante no encontrado.");
            return null;
        }
        agregarMensajes(req, vista);
        return new ModelAndView(vista, "admin/estudiantes/editarEstudiante.mustache");
    }
    
    // POST /admin/estudiantes/:id/edit
    public Object editar(Request req, Response res) {
        Integer nroLegajo = Integer.parseInt(req.params(":id"));
        try {
            estudianteService.editarEstudiante(
                nroLegajo,
                req.queryParams("nombre"),
                req.queryParams("apellido"),
                req.queryParams("fecha_nacimiento"),
                req.queryParams("telefono"),
                req.queryParams("direccion"),
                req.queryParams("email")
            );
            AuditoriaService.registrarAuditoria(req, "EDITAR_ESTUDIANTE", "Legajo: " + nroLegajo);
            return redirectWithSuccess(res, "/admin/estudiantes", "Estudiante actualizado correctamente.");
        } catch (IllegalArgumentException e) {
            return redirectWithError(res, "/admin/estudiantes", e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Error al editar estudiante: {}", e.getMessage());
            return redirectWithError(res, "/admin/estudiantes/" + nroLegajo + "/edit", "Error al actualizar.");
        }
    }
    
    // GET /admin/estudiantes/:id/delete
    public ModelAndView mostrarConfirmacionEliminar(Request req, Response res) {
        Integer nroLegajo = Integer.parseInt(req.params(":id"));
        Map<String, Object> vista = estudianteService.obtenerEstudianteParaVista(nroLegajo);
        if (vista == null) {
            res.redirect("/admin/estudiantes?errorMessage=Estudiante no encontrado.");
            return null;
        }
        return new ModelAndView(vista, "admin/estudiantes/eliminarEstudiante.mustache");
    }
    
    // POST /admin/estudiantes/:id/delete
    public Object eliminar(Request req, Response res) {
        Integer nroLegajo = Integer.parseInt(req.params(":id"));
        try {
            estudianteService.eliminarEstudiante(nroLegajo);
            AuditoriaService.registrarAuditoria(req, "ELIMINAR_ESTUDIANTE", "Legajo: " + nroLegajo);
            return redirectWithSuccess(res, "/admin/estudiantes", "Estudiante eliminado correctamente.");
        } catch (IllegalArgumentException e) {
            return redirectWithError(res, "/admin/estudiantes", e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Error al eliminar estudiante: {}", e.getMessage());
            return redirectWithError(res, "/admin/estudiantes", "Error al eliminar estudiante.");
        }
    }
    
    // GET /admin/estudiantes/:legajo/materias
    public ModelAndView mostrarMateriasParaInscribir(Request req, Response res) {
        Integer nroLegajo = Integer.parseInt(req.params(":legajo"));
        Map<String, Object> model = estudianteService.obtenerVistaMateriasEstudiante(nroLegajo);
        if (model == null) {
            res.redirect("/admin/estudiantes?errorMessage=Estudiante no encontrado.");
            return null;
        }
        agregarMensajes(req, model);
        return new ModelAndView(model, "admin/estudiantes/inscribirMateria.mustache");
    }
    
    // POST /admin/estudiantes/:legajo/inscribir
    public Object inscribirMateria(Request req, Response res) {
        Integer nroLegajo = Integer.parseInt(req.params(":legajo"));
        Integer codMateria = Integer.parseInt(req.queryParams("cod_materia"));
        try {
            estudianteService.inscribirMateria(nroLegajo, codMateria);
            AuditoriaService.registrarAuditoria(req, "INSCRIBIR_MATERIA_ESTUDIANTE", 
                "Legajo: " + nroLegajo);
            return redirectWithSuccess(res, "/admin/estudiantes", "Materia asignada correctamente.");
        } catch (IllegalArgumentException e) {
            return redirectWithError(res, "/admin/estudiantes", e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Error al inscribir materia: {}", e.getMessage());
            return redirectWithError(res, "/admin/estudiantes", e.getMessage());
        }
    }
}