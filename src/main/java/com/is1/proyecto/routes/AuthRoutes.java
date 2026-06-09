package com.is1.proyecto.routes;
import com.is1.proyecto.controller.AuthController;
import spark.template.mustache.MustacheTemplateEngine;
import static spark.Spark.get;
import static spark.Spark.post;

public class AuthRoutes {
    public static void register(AuthController controller, MustacheTemplateEngine engine) {

        get("/", (req, res) -> controller.mostrarLogin(req, res), engine);
        post("/login", (req, res) -> controller.procesarLogin(req, res), engine);
        get("/logout", (req, res) -> controller.procesarLogout(req, res));
        get("/user/create", (req, res) -> controller.mostrarFormularioRegistro(req, res), engine);
        get("/user/new", (req, res) -> controller.mostrarFormularioRegistro(req, res), engine);
        post("/user/new", (req, res) -> controller.procesarRegistro(req, res));

    }
}
