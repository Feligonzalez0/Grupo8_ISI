package com.is1.proyecto.controller.AdminController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.services.adminService.AdminCarreraService;
import com.is1.proyecto.services.AuditoriaService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public class AdminCarreraController extends AdminBaseController {
    
    private final AdminCarreraService carreraService;
    
    public AdminCarreraController() {
        this.carreraService = new AdminCarreraService();
    }
    
    // GET /admin/carreras
    public ModelAndView listar(Request req, Response res) {
        List<Map<String, Object>> carreras = AdminCarreraService.listarCarreras();
        
        Map<String, Object> model = new HashMap<>();
        model.put("carreras", carreras);
        agregarMensajes(req, model);
        
        return new ModelAndView(model, "admin/carreras/carrerasDashboard.mustache");
    }
    
    // GET /admin/carreras/agregar
    public ModelAndView mostrarFormularioAgregar(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        agregarMensajes(req, model);
        
        return new ModelAndView(model, "admin/carreras/agregarCarrera.mustache");
    }
    
    // POST /admin/carreras/agregar
    public Object crear(Request req, Response res) {
        String nombre = req.queryParams("nombre");
        String descripcion = req.queryParams("descripcion");
        
        try {
            carreraService.crearCarrera(nombre, descripcion);
            
            AuditoriaService.registrarAuditoria(req, "CREAR_CARRERA", "Nombre: " + nombre);
            
            return redirectWithSuccess(res, "/admin/carreras", "Carrera creada correctamente.");
            
        } catch (IllegalArgumentException e) {
            return redirectWithError(res, "/admin/carreras/agregar", e.getMessage());
            
        } catch (RuntimeException e) {
            logger.error("Error al crear carrera: {}", e.getMessage());
            return redirectWithError(res, "/admin/carreras/agregar", e.getMessage());
        }
    }
    
    // GET /admin/carreras/:id/edit
    public ModelAndView mostrarFormularioEditar(Request req, Response res) {
        Integer codCarrera = Integer.parseInt(req.params(":id"));
        
        Map<String, Object> carrera = carreraService.obtenerCarreraParaVista(codCarrera);
        if (carrera == null) {
            res.redirect("/admin/carreras?errorMessage=Carrera no encontrada.");
            return null;
        }
        
        agregarMensajes(req, carrera);
        
        return new ModelAndView(carrera, "admin/carreras/editarCarrera.mustache");
    }
    
    // POST /admin/carreras/:id/edit
    public Object editar(Request req, Response res) {
        Integer codCarrera = Integer.parseInt(req.params(":id"));
        
        String nombre = req.queryParams("nombre");
        String descripcion = req.queryParams("descripcion");
        
        try {
            carreraService.editarCarrera(codCarrera, nombre, descripcion);
            
            AuditoriaService.registrarAuditoria(req, "EDITAR_CARRERA", "Código: " + codCarrera);
            
            return redirectWithSuccess(res, "/admin/carreras", "Carrera actualizada correctamente.");
            
        } catch (IllegalArgumentException e) {
            return redirectWithError(res, "/admin/carreras/" + codCarrera + "/edit", e.getMessage());
            
        } catch (RuntimeException e) {
            logger.error("Error al editar carrera {}: {}", codCarrera, e.getMessage());
            return redirectWithError(res, "/admin/carreras/" + codCarrera + "/edit", e.getMessage());
        }
    }
    
    // GET /admin/carreras/:id/delete
    public ModelAndView mostrarConfirmacionEliminar(Request req, Response res) {
        Integer codCarrera = Integer.parseInt(req.params(":id"));
        
        Map<String, Object> vista = carreraService.obtenerVistaEliminarCarrera(codCarrera);
        if (vista == null) {
            res.redirect("/admin/carreras?errorMessage=Carrera no encontrada.");
            return null;
        }
        
        return new ModelAndView(vista, "admin/carreras/eliminarCarrera.mustache");
    }
    
    // POST /admin/carreras/:id/delete
    public Object eliminar(Request req, Response res) {
        Integer codCarrera = Integer.parseInt(req.params(":id"));
        
        try {
            carreraService.eliminarCarrera(codCarrera);
            
            AuditoriaService.registrarAuditoria(req, "ELIMINAR_CARRERA", "Código: " + codCarrera);
            
            return redirectWithSuccess(res, "/admin/carreras", "Carrera eliminada correctamente.");
            
        } catch (IllegalArgumentException e) {
            return redirectWithError(res, "/admin/carreras", e.getMessage());
            
        } catch (RuntimeException e) {
            logger.error("Error al eliminar carrera {}: {}", codCarrera, e.getMessage());
            return redirectWithError(res, "/admin/carreras", e.getMessage());
        }
    }
}