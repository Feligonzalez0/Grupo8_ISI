package com.is1.proyecto; // Define el paquete de la aplicación, debe coincidir con la estructura de carpetas.

import org.javalite.activejdbc.Base; // Clase central de ActiveJDBC para gestionar la conexión a la base de datos.
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.is1.proyecto.config.DBConfigSingleton; // Motor de plantillas Mustache para Spark.
import com.is1.proyecto.controller.AuthController;
import com.is1.proyecto.controller.DashboardController;
import com.is1.proyecto.controller.EstudianteController;
import com.is1.proyecto.filters.AuthFilter;
import com.is1.proyecto.routes.AdminRoutes;
import com.is1.proyecto.routes.AuthRoutes;
import com.is1.proyecto.routes.DashboardRoutes;
import com.is1.proyecto.routes.DocenteRoutes;
import com.is1.proyecto.routes.EstudianteRoutes;
import com.is1.proyecto.services.AuthService;

import static spark.Spark.exception;
import static spark.Spark.internalServerError;
import static spark.Spark.notFound;
import static spark.Spark.port;
import spark.template.mustache.MustacheTemplateEngine;

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
        DocenteRoutes.register(engine);
        AdminRoutes.register(engine);

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
    }
}