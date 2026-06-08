package com.is1.proyecto.filters;
 
import com.is1.proyecto.config.AppConfig;
import com.is1.proyecto.config.DBConfigSingleton;
import org.javalite.activejdbc.Base;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
 
import static spark.Spark.*;
 
/**
 * Centraliza todos los filtros before/after de Spark.
 *
 * Responsabilidades:
 *   1. Apertura y cierre de conexión a la BD en cada request.
 *   2. Protección de rutas que requieren sesión activa.
 *   3. Protección de rutas que requieren rol ADMINISTRADOR.
 *   4. Protección de rutas que requieren rol DOCENTE (o ADMINISTRADOR).
 *
 * Uso desde App.java:
 *   AuthFilter.registerAll(DBConfigSingleton.getInstance());
 */
public class AuthFilter {
 
    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);
 
    // ------------------------------------------------------------------
    // Punto de entrada único — llamar desde App.main()
    // ------------------------------------------------------------------
    public static void registerAll(DBConfigSingleton dbConfig) {
 
        registrarFiltrosDB(dbConfig);
        registrarFiltrosAuth();
    }
 
    // ------------------------------------------------------------------
    // 1. Gestión del ciclo de vida de la conexión a la BD
    // ------------------------------------------------------------------
    private static void registrarFiltrosDB(DBConfigSingleton dbConfig) {
 
        before((req, res) -> {
            try {
                if (!Base.hasConnection()) {
                    Base.open(
                        dbConfig.getDriver(),
                        dbConfig.getDbUrl(),
                        dbConfig.getUser(),
                        dbConfig.getPass()
                    );
                }
            } catch (Exception e) {
                logger.error("Error al abrir conexión con ActiveJDBC: {}", e.getMessage(), e);
                halt(500, "{\"error\": \"Error interno del servidor: Fallo al conectar a la base de datos.\"}");
            }
        });
 
        after((req, res) -> {
            try {
                Base.close();
            } catch (Exception e) {
                logger.error("Error al cerrar conexión con ActiveJDBC: {}", e.getMessage(), e);
            }
        });
    }
 
    // ------------------------------------------------------------------
    // 2. Filtros de autenticación y autorización por rol
    // ------------------------------------------------------------------
    private static void registrarFiltrosAuth() {
 
        // Rutas de administrador
        before("/admin",   (req, res) -> requireAdmin(req, res));
        before("/admin/*", (req, res) -> requireAdmin(req, res));
 
        // Rutas de sesión genérica (cualquier usuario logueado)
        before("/dashboard",      (req, res) -> requireLogin(req, res));
        before("/profile",        (req, res) -> requireLogin(req, res));
        before("/configuracion",  (req, res) -> requireLogin(req, res));
        before("/configuracion/*",(req, res) -> requireLogin(req, res));
 
        // Rutas de docente
        before("/docente/*", (req, res) -> requireDocente(req, res));
 
        // Rutas de estudiante
        before("/estudiante/*", (req, res) -> requireEstudiante(req, res));
    }
 
    // ------------------------------------------------------------------
    // Helpers reutilizables (públicos para tests o uso puntual futuro)
    // ------------------------------------------------------------------
 
    /** Verifica que haya sesión activa; redirige a "/" si no. */
    public static void requireLogin(Request req, Response res) {
        Boolean loggedIn = req.session().attribute(AppConfig.SESSION_LOGGED_IN);
        if (loggedIn == null || !loggedIn) {
            res.redirect("/");
            halt();
        }
    }
 
    /** Verifica sesión activa Y rol ADMINISTRADOR. */
    public static void requireAdmin(Request req, Response res) {
        Boolean loggedIn = req.session().attribute(AppConfig.SESSION_LOGGED_IN);
        if (loggedIn == null || !loggedIn) {
            res.redirect("/");
            halt();
        }
        if (!AppConfig.ROL_ADMIN.equals(req.session().attribute(AppConfig.SESSION_USER_ROL))) {
            res.redirect("/dashboard?error=Debes ser administrador para acceder a esta pagina.");
            halt();
        }
    }
 
    /** Verifica sesión activa Y rol DOCENTE o ADMINISTRADOR. */
    public static void requireDocente(Request req, Response res) {
        requireLogin(req, res);
        String rol = req.session().attribute(AppConfig.SESSION_USER_ROL);
        if (!AppConfig.ROL_DOCENTE.equals(rol) && !AppConfig.ROL_ADMIN.equals(rol)) {
            res.redirect("/dashboard");
            halt();
        }
    }
 
    /** Verifica sesión activa Y rol ALUMNO o ADMINISTRADOR. */
    public static void requireEstudiante(Request req, Response res) {
        requireLogin(req, res);
        String rol = req.session().attribute(AppConfig.SESSION_USER_ROL);
        if (!AppConfig.ROL_ALUMNO.equals(rol) && !AppConfig.ROL_ADMIN.equals(rol)) {
            res.redirect("/dashboard");
            halt();
        }
    }
}