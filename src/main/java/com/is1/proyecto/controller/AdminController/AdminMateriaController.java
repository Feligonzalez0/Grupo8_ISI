package com.is1.proyecto.controller.AdminController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.services.adminService.AdminMateriaService;
import com.is1.proyecto.services.AuditoriaService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public class AdminMateriaController extends AdminBaseController {
    
    private final AdminMateriaService materiaService;
    
    public AdminMateriaController() {
        this.materiaService = new AdminMateriaService();
    }
    
    // GET /admin/materias
    public ModelAndView listar(Request req, Response res) {
        List<Map<String, Object>> materias = AdminMateriaService.listarMaterias();
        
        Map<String, Object> model = new HashMap<>();
        model.put("materias", materias);
        agregarMensajes(req, model);
        
        return new ModelAndView(model, "admin/materias/materiasDashboard.mustache");
    }
    
    // GET /admin/materias/listado
    public ModelAndView listadoCompleto(Request req, Response res) {
        List<Map<String, Object>> materias = AdminMateriaService.listarMateriasParaListado();
        
        Map<String, Object> model = new HashMap<>();
        model.put("materias", materias);
        agregarMensajes(req, model);
        
        return new ModelAndView(model, "admin/materias/listadoMaterias.mustache");
    }
    
    // GET /admin/materias/agregar
    public ModelAndView mostrarFormularioAgregar(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        
        List<Map<String, Object>> planes = materiaService.listarPlanes();
        model.put("planes", planes);
        model.put("sinPlanes", planes.isEmpty());
        agregarMensajes(req, model);
        
        return new ModelAndView(model, "admin/materias/agregarMateria.mustache");
    }
    
    // POST /admin/materias/agregar
    public Object crear(Request req, Response res) {
        String nombre = req.queryParams("nombre");
        String codigo = req.queryParams("cod_materia");
        String descripcion = req.queryParams("descripcion");
        String codPlanStr = req.queryParams("cod_plan");
        
        try {
            materiaService.crearMateria(nombre, codigo, descripcion, codPlanStr);
            
            AuditoriaService.registrarAuditoria(req, "CREAR_MATERIA", "Nombre: " + nombre);
            
            return redirectWithSuccess(res, "/admin/materias", "Materia creada correctamente.");
            
        } catch (IllegalArgumentException e) {
            return redirectWithError(res, "/admin/materias/agregar", e.getMessage());
            
        } catch (RuntimeException e) {
            logger.error("Error al crear materia: {}", e.getMessage());
            return redirectWithError(res, "/admin/materias/agregar", e.getMessage());
        }
    }
    
    // GET /admin/materias/:id/edit
    public ModelAndView mostrarFormularioEditar(Request req, Response res) {
        Integer codMateria = Integer.parseInt(req.params(":id"));
        
        Map<String, Object> materia = materiaService.obtenerMateriaParaVista(codMateria);
        if (materia == null) {
            res.redirect("/admin/materias?errorMessage=Materia no encontrada.");
            return null;
        }
        
        List<Map<String, Object>> planes = materiaService.listarPlanesConSelected(
            (Integer) materia.get("codPlan")
        );
        
        Map<String, Object> model = new HashMap<>();
        model.putAll(materia);
        model.put("planes", planes);
        agregarMensajes(req, model);
        
        return new ModelAndView(model, "admin/materias/editarMateria.mustache");
    }
    
    // POST /admin/materias/:id/edit
    public Object editar(Request req, Response res) {
        Integer codMateria = Integer.parseInt(req.params(":id"));
        
        String nombre = req.queryParams("nombre");
        String descripcion = req.queryParams("descripcion");
        String codPlanStr = req.queryParams("cod_plan");
        
        try {
            materiaService.editarMateria(codMateria, nombre, descripcion, codPlanStr);
            
            AuditoriaService.registrarAuditoria(req, "EDITAR_MATERIA", "Código materia: " + codMateria);
            
            return redirectWithSuccess(res, "/admin/materias", "Materia actualizada correctamente.");
            
        } catch (IllegalArgumentException e) {
            return redirectWithError(res, "/admin/materias/" + codMateria + "/edit", e.getMessage());
            
        } catch (RuntimeException e) {
            logger.error("Error al editar materia {}: {}", codMateria, e.getMessage());
            return redirectWithError(res, "/admin/materias/" + codMateria + "/edit", e.getMessage());
        }
    }
    
    // GET /admin/materias/:id/delete
    public ModelAndView mostrarConfirmacionEliminar(Request req, Response res) {
        Integer codMateria = Integer.parseInt(req.params(":id"));
        
        Map<String, Object> materia = materiaService.obtenerMateriaParaVista(codMateria);
        if (materia == null) {
            res.redirect("/admin/materias?errorMessage=Materia no encontrada.");
            return null;
        }
        
        return new ModelAndView(materia, "admin/materias/eliminarMateria.mustache");
    }
    
    // POST /admin/materias/:id/delete
    public Object eliminar(Request req, Response res) {
        Integer codMateria = Integer.parseInt(req.params(":id"));
        
        try {
            materiaService.eliminarMateria(codMateria);
            
            AuditoriaService.registrarAuditoria(req, "ELIMINAR_MATERIA", "Código materia: " + codMateria);
            
            return redirectWithSuccess(res, "/admin/materias", "Materia eliminada correctamente.");
            
        } catch (IllegalArgumentException e) {
            return redirectWithError(res, "/admin/materias", e.getMessage());
            
        } catch (RuntimeException e) {
            logger.error("Error al eliminar materia {}: {}", codMateria, e.getMessage());
            return redirectWithError(res, "/admin/materias", e.getMessage());
        }
    }
}