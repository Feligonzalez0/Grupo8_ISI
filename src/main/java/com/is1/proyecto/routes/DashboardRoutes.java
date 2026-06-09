package com.is1.proyecto.routes;
import com.is1.proyecto.controller.DashboardController;
import spark.template.mustache.MustacheTemplateEngine;
import static spark.Spark.get;
import static spark.Spark.post;

public class DashboardRoutes {
    public static void register(DashboardController controller, MustacheTemplateEngine engine) {
        get("/dashboard", (req, res) -> controller.mostrarDashboard(req, res), engine);
        get("/profile", (req, res) -> controller.mostrarPerfil(req, res), engine);
        get("/configuracion", (req, res) -> controller.mostrarConfiguracion(req, res), engine);
        post("/configuracion/password", (req, res) -> controller.procesarCambioPassword(req, res));
        post("/configuracion/datos", (req, res) -> controller.procesarActualizacionDatos(req, res));
    }
}