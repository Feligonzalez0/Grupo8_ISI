package com.is1.proyecto.controller.AdminController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.models.AuditoriaAdmin;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public class AdminDashboardController extends AdminBaseController {
    
    // GET /admin
    public ModelAndView mostrarDashboard(Request req, Response res) {
        return new ModelAndView(new HashMap<>(), "admin/adminDashboard.mustache");
    }
    
    // GET /admin/auditoria
    public ModelAndView mostrarAuditoria(Request req, Response res) {
        List<AuditoriaAdmin> logsDB = AuditoriaAdmin.findAll().orderBy("id DESC");
        List<Map<String, Object>> logs = new ArrayList<>();

        for (AuditoriaAdmin log : logsDB) {
            Map<String, Object> logView = new HashMap<>();
            logView.put("usuario", log.getUsuario());
            logView.put("accion", log.getAccion());
            logView.put("detalle", log.getDetalle());
            logView.put("fecha", log.getFecha());
            logs.add(logView);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("logs", logs);
        model.put("sinLogs", logs.isEmpty());

        return new ModelAndView(model, "admin/adminAuditoria.mustache");
    }
}