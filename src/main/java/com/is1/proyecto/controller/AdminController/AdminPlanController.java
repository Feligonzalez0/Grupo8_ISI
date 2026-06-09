package com.is1.proyecto.controller.AdminController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.services.adminService.AdminPlanService;
import com.is1.proyecto.services.AuditoriaService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public class AdminPlanController extends AdminBaseController {
    
    private final AdminPlanService planService;
    
    public AdminPlanController() {
        this.planService = new AdminPlanService();
    }
    
    // GET /admin/planes
    public ModelAndView listar(Request req, Response res) {
        List<Map<String, Object>> planes = AdminPlanService.listarPlanes();
        
        Map<String, Object> model = new HashMap<>();
        model.put("planes", planes);
        agregarMensajes(req, model);
        
        return new ModelAndView(model, "admin/planes/planesDashboard.mustache");
    }
    
    // GET /admin/planes/agregar
    public ModelAndView mostrarFormularioAgregar(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        model.put("carreras", planService.listarCarreras());
        agregarMensajes(req, model);
        
        return new ModelAndView(model, "admin/planes/agregarPlan.mustache");
    }
    
    // POST /admin/planes/agregar
    public Object crear(Request req, Response res) {
        String anioStr = req.queryParams("anio");
        String vigenciaStr = req.queryParams("vigencia");
        String aniosTotalStr = req.queryParams("anios_total");
        String cantMatStr = req.queryParams("cantidad_materias_total");
        String codCarreraStr = req.queryParams("cod_carrera");
        
        try {
            planService.crearPlan(anioStr, vigenciaStr, aniosTotalStr, cantMatStr, codCarreraStr);
            
            AuditoriaService.registrarAuditoria(req, "CREAR_PLAN", 
                    "Año: " + anioStr + " - Carrera: " + codCarreraStr);
            
            return redirectWithSuccess(res, "/admin/planes", "Plan creado correctamente.");
            
        } catch (IllegalArgumentException e) {
            return redirectWithError(res, "/admin/planes/agregar", e.getMessage());
            
        } catch (RuntimeException e) {
            logger.error("Error al crear plan: {}", e.getMessage());
            return redirectWithError(res, "/admin/planes/agregar", e.getMessage());
        }
    }
    
    // GET /admin/planes/:id/edit
    public ModelAndView mostrarFormularioEditar(Request req, Response res) {
        Integer codPlan = Integer.parseInt(req.params(":id"));
        
        Map<String, Object> plan = planService.obtenerPlanParaVista(codPlan);
        if (plan == null) {
            res.redirect("/admin/planes?errorMessage=Plan no encontrado.");
            return null;
        }
        
        Map<String, Object> carrerasData = planService.obtenerCarrerasParaSelect(codPlan);
        if (carrerasData == null) {
            res.redirect("/admin/planes?errorMessage=Plan no encontrado.");
            return null;
        }
        
        Map<String, Object> model = new HashMap<>();
        model.putAll(plan);
        model.putAll(carrerasData);
        agregarMensajes(req, model);
        
        return new ModelAndView(model, "admin/planes/editarPlan.mustache");
    }
    
    // POST /admin/planes/:id/edit
    public Object editar(Request req, Response res) {
        Integer codPlan = Integer.parseInt(req.params(":id"));
        
        String vigenciaStr = req.queryParams("vigencia");
        String aniosTotalStr = req.queryParams("anios_total");
        String cantMatStr = req.queryParams("cantidad_materias_total");
        String codCarreraStr = req.queryParams("cod_carrera");
        
        try {
            planService.editarPlan(codPlan, vigenciaStr, aniosTotalStr, cantMatStr, codCarreraStr);
            
            AuditoriaService.registrarAuditoria(req, "EDITAR_PLAN", "Código plan: " + codPlan);
            
            return redirectWithSuccess(res, "/admin/planes", "Plan actualizado correctamente.");
            
        } catch (IllegalArgumentException e) {
            return redirectWithError(res, "/admin/planes/" + codPlan + "/edit", e.getMessage());
            
        } catch (RuntimeException e) {
            logger.error("Error al editar plan {}: {}", codPlan, e.getMessage());
            return redirectWithError(res, "/admin/planes/" + codPlan + "/edit", e.getMessage());
        }
    }
    
    // GET /admin/planes/:id/delete
    public ModelAndView mostrarConfirmacionEliminar(Request req, Response res) {
        Integer codPlan = Integer.parseInt(req.params(":id"));
        
        Map<String, Object> vista = planService.obtenerVistaEliminarPlan(codPlan);
        if (vista == null) {
            res.redirect("/admin/planes?errorMessage=Plan no encontrado.");
            return null;
        }
        
        agregarMensajes(req, vista);
        
        return new ModelAndView(vista, "admin/planes/eliminarPlan.mustache");
    }
    
    // POST /admin/planes/:id/delete
    public Object eliminar(Request req, Response res) {
        Integer codPlan = Integer.parseInt(req.params(":id"));
        
        try {
            planService.eliminarPlan(codPlan);
            
            AuditoriaService.registrarAuditoria(req, "ELIMINAR_PLAN", "Código plan: " + codPlan);
            
            return redirectWithSuccess(res, "/admin/planes", "Plan eliminado correctamente.");
            
        } catch (IllegalArgumentException e) {
            return redirectWithError(res, "/admin/planes", e.getMessage());
            
        } catch (RuntimeException e) {
            logger.error("Error al eliminar plan {}: {}", codPlan, e.getMessage());
            return redirectWithError(res, "/admin/planes", e.getMessage());
        }
    }
    
    // GET /admin/planes/:id/materias
    public ModelAndView mostrarMaterias(Request req, Response res) {
        Integer codPlan = Integer.parseInt(req.params(":id"));
        
        Map<String, Object> vista = planService.obtenerMateriasPorPlan(codPlan);
        if (vista == null) {
            res.redirect("/admin/planes?errorMessage=Plan no encontrado.");
            return null;
        }
        
        agregarMensajes(req, vista);
        
        return new ModelAndView(vista, "admin/planes/materiasPlan.mustache");
    }
}