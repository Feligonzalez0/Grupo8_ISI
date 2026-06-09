package com.is1.proyecto; // Define el paquete de la aplicación, debe coincidir con la estructura de carpetas.

import java.util.ArrayList;
import java.util.HashMap; // Utilidad para serializar/deserializar objetos Java a/desde JSON.
import java.util.List;
import java.util.Map; // Importa los métodos estáticos principales de Spark (get, post, before, after, etc.).

import org.javalite.activejdbc.Base; // Clase central de ActiveJDBC para gestionar la conexión a la base de datos.
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.is1.proyecto.config.*; // Motor de plantillas Mustache para Spark.
import com.is1.proyecto.controller.*;
import com.is1.proyecto.filters.AuthFilter;
import com.is1.proyecto.models.*;
import com.is1.proyecto.routes.*;
import com.is1.proyecto.services.*;

import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;
import static spark.Spark.notFound;
import static spark.Spark.internalServerError;
import static spark.Spark.exception;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.get; // Modelo de ActiveJDBC que representa la tabla 'users'.

// mvn clean compile activejdbc-instrumentation:instrument exec:java "-Dexec.mainClass=com.is1.proyecto.App"

/**
 * Clase principal de la aplicación Spark.
 * Configura las rutas, filtros y el inicio del servidor web.
 */
public class App {

    // Se inicializa una sola vez para ser reutilizada en toda la aplicación.
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private static void ejecutarScheme() {
    try {
        String sql = new String(App.class.getClassLoader().getResourceAsStream("scheme.sql").readAllBytes());
        Base.exec(sql);

        System.out.println("Schema ejecutado correctamente.");
    } catch (Exception e) {
        System.err.println("Error ejecutando schema.sql");
        e.printStackTrace();
    }
}

    /**
     * Método principal que se ejecuta al iniciar la aplicación.
     * Aquí se configuran todas las rutas y filtros de Spark.
     */
    public static void main(String[] args) {
        port(8080); // Configura el puerto en el que la aplicación Spark escuchará las peticiones (por defecto es 8080).

        // Obtener la instancia única del singleton de configuración de la base de datos.
        DBConfigSingleton dbConfig = DBConfigSingleton.getInstance();
        AuthFilter.registerAll(dbConfig);
        logger.info("Base de datos usada: {}", dbConfig.getDbUrl());

        try {
            Base.open(dbConfig.getDriver(), dbConfig.getDbUrl(), dbConfig.getUser(), dbConfig.getPass());

            ejecutarScheme();

            Base.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // --- Rutas de autenticación ---
        MustacheTemplateEngine engine = new MustacheTemplateEngine();
        AuthController authController = new AuthController(new AuthService());
        AuthRoutes.register(authController, engine);

        // --- Rutas ---
        DashboardRoutes.register(new DashboardController(new AuthService()), engine);
        EstudianteRoutes.register(new EstudianteController(), engine);
        DocenteRoutes.register(new DocenteController(), engine);
        
        AdminController adminController = new AdminController();
        AdminRoutes.register(adminController, engine);

        // --- Manejo de errores ---
        // Ir a una ruta que no existe.
        notFound((req, res) -> {
            res.type("text/html");
            logger.warn("404 - Ruta no encontrada: {}", req.url());

            return "<h1>404 - Pagina no encontrada</h1><p>La ruta <b>" + req.url() + "</b> no existe.</p><a href='/dashboard'>Volver al inicio</a>";
        });

        // Error interno en el servidor.
        internalServerError((req, res) -> {
            res.type("text/html");
            logger.error("500 - Error interno en: {}", req.url());

            return "<h1>500 - Error interno del servidor</h1><p>Ocurrió un error inesperado. Intente más tarde.</p><a href='/dashboard'>Volver al inicio</a>";
        });

        // Excepciones no capturadas.
        exception(Exception.class, (e, req, res) -> {
            logger.error("Excepción no manejada en {}: {}", req.url(), e.getMessage(), e);

            res.status(500);
            res.type("text/html");
            res.body("<h1>500 - Error interno del servidor</h1><p>Ocurrió un error inesperado. Intente más tarde.</p><a href='/dashboard'>Volver al inicio</a>");
        });

        // Acceso no autorizado.
        exception(spark.HaltException.class, (e, req, res) -> {
            logger.warn("Acceso detenido en {}: status {}", req.url(), e.statusCode());
        });
        
        get("/admin/auditoria", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            List<AuditoriaAdmin> logsDB = AuditoriaAdmin.findAll().orderBy("id DESC");

            List<Map<String, Object>> logs = new ArrayList<>();
            for(AuditoriaAdmin log : logsDB) {
                Map<String, Object> logView = new HashMap<>();
                logView.put("usuario", log.getUsuario());
                logView.put("accion",  log.getAccion());
                logView.put("detalle", log.getDetalle());
                logView.put("fecha",   log.getFecha());
                logs.add(logView);
            }

            model.put("logs",    logs);
            model.put("sinLogs", logs.isEmpty());

            return new ModelAndView(model, "admin/adminAuditoria.mustache");
        }, new MustacheTemplateEngine());
        } // Fin del método main

        // HELPERS
        public static boolean esEmailValido(String email) {
            String regex = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
            return email != null && email.matches(regex);
        }
} // Fin de la clase App