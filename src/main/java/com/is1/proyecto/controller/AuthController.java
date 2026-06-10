package com.is1.proyecto.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.is1.proyecto.models.User;
import com.is1.proyecto.services.AuthService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

/**
 * Controlador de autenticación.
 *
 * Responsabilidades:
 *   - Leer parámetros del Request (formularios, sesión, query params).
 *   - Delegar la lógica de negocio a AuthService.
 *   - Gestionar la sesión HTTP (escribir/invalidar atributos).
 *   - Construir el modelo de vista (Map) y devolver el ModelAndView correcto,
 *     o redirigir según el resultado.
 */
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // GET /  —  Mostrar formulario de login
    public ModelAndView mostrarLogin(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();

        agregarMensajes(req, model, "error", "message");

        return new ModelAndView(model, "login.mustache");
    }

    // POST /login  —  Procesar formulario de login
    public ModelAndView procesarLogin(Request req, Response res) {
        String username = req.queryParams("username");
        String plainTextPassword = req.queryParams("password");

        Map<String, Object> model = new HashMap<>();

        try {
            // Delegar validación y verificación de credenciales al service
            User usuario = authService.login(username, plainTextPassword);

            // --- Login exitoso: poblar sesión ---
            req.session(true).attribute("currentUserUsername", usuario.getString("name"));
            req.session().attribute("userId", usuario.getId());
            req.session().attribute("userRol", usuario.getRol());
            req.session().attribute("loggedIn", true);

            logger.debug("Sesión iniciada. ID de sesión: {}", req.session().id());

            res.redirect("/dashboard");
            return null;

        } catch (IllegalArgumentException e) {
            // Campos vacíos u otros errores de validación
            res.status(400);
            model.put("errorMessage", e.getMessage());
            return new ModelAndView(model, "login.mustache");

        } catch (SecurityException e) {
            // Credenciales incorrectas (usuario no existe o contraseña mal)
            res.status(401);
            model.put("errorMessage", e.getMessage());
            return new ModelAndView(model, "login.mustache");
        }
    }

    // GET /logout  —  Cerrar sesión
    public Object procesarLogout(Request req, Response res) {
        req.session().invalidate();
        logger.debug("Sesión cerrada. Redirigiendo a /");

        res.redirect("/");
        return null;
    }

    // GET /user/create  —  Mostrar formulario de registro
    public ModelAndView mostrarFormularioRegistro(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();

        // Estos query params son los que el POST /user/new usa al redirigir
        agregarMensajes(req, model, "error", "message");

        return new ModelAndView(model, "user_form.mustache");
    }

    // POST /user/new  —  Procesar formulario de registro
    public String procesarRegistro(Request req, Response res) {
        String name = req.queryParams("name");
        String password = req.queryParams("password");

        try {
            User nuevoUsuario = authService.registrar(name, password);

            res.status(201);
            res.redirect("/user/create?message=Cuenta creada exitosamente para "
                    + nuevoUsuario.getString("name") + "!");
            return "";

        } catch (IllegalArgumentException e) {
            // Validaciones: campos vacíos, usuario duplicado
            res.status(400);
            res.redirect("/user/create?error=" + e.getMessage());
            return "";

        } catch (RuntimeException e) {
            // Error interno de base de datos
            res.status(500);
            res.redirect("/user/create?error=" + e.getMessage());
            return "";
        }
    }

    // HELPER PRIVADO
    private void agregarMensajes(Request req, Map<String, Object> model, String errorParam, String successParam) 
    {
        String error = req.queryParams(errorParam);
        String success = req.queryParams(successParam);

        if (error != null && !error.trim().isEmpty()) model.put("errorMessage", error);
        if (success != null && !success.trim().isEmpty()) model.put("successMessage", success);
    }
}