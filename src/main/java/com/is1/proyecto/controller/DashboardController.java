package com.is1.proyecto.controller;

import java.util.HashMap;
import java.util.Map;

import org.javalite.activejdbc.Base;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.is1.proyecto.models.Docente;
import com.is1.proyecto.models.Estudiante;
import com.is1.proyecto.models.Persona;
import com.is1.proyecto.models.User;
import com.is1.proyecto.services.AuditoriaService;
import com.is1.proyecto.services.AuthService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

/**
 * Controlador de Dashboard, Perfil y Configuración.
 *
 * Responsabilidades:
 *   - Renderizar el dashboard principal del usuario autenticado.
 *   - Mostrar y actualizar el perfil del usuario.
 *   - Mostrar la página de configuración y procesar sus formularios
 *     (cambio de contraseña y actualización de datos personales).
 *
 * Rutas que maneja:
 *   GET  /dashboard
 *   GET  /profile
 *   GET  /configuracion
 *   POST /configuracion/password
 *   POST /configuracion/datos
 */
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    private final AuthService authService;

    // Constructor
    public DashboardController(AuthService authService) {
        this.authService = authService;
    }

    // GET /dashboard
    public ModelAndView mostrarDashboard(Request req, Response res) {
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn       = req.session().attribute("loggedIn");

        // Guardia de sesión (el filtro AuthFilter ya debería haber bloqueado esto,
        // pero se mantiene como segunda línea de defensa)
        if (currentUsername == null || loggedIn == null || !loggedIn) {
            logger.debug("Acceso a /dashboard sin sesión. Redirigiendo.");
            res.redirect("/?error=Debes iniciar sesion para acceder a esta pagina.");
            return null;
        }

        String userRol = req.session().attribute("userRol");

        Map<String, Object> model = new HashMap<>();
        model.put("username",    currentUsername);
        model.put("rol",         formatearRol(userRol));
        model.put("rolClass",    resolverClaseRol(userRol));
        model.put("isAdmin",      "ADMINISTRADOR".equals(userRol));
        model.put("isDocente",    "DOCENTE".equals(userRol));
        model.put("isAlumno",     "ALUMNO".equals(userRol));
        model.put("isUnassigned", "UNASSIGNED".equals(userRol));

        agregarMensaje(req, model, "error", "errorMessage");

        return new ModelAndView(model, "dashboard.mustache");
    }

    // GET /profile
    public ModelAndView mostrarPerfil(Request req, Response res) {
        User usuario = resolverUsuarioSesion(req, res);
        if (usuario == null) return null; // resolverUsuarioSesion ya redirigió

        String rol = usuario.getString("rol");
        Map<String, Object> model = new HashMap<>();

        model.put("usuario",  usuario);
        model.put("esAdmin",  "ADMINISTRADOR".equals(rol));
        model.put("esDocente","DOCENTE".equals(rol));
        model.put("esAlumno", "ALUMNO".equals(rol));

        agregarDatosPersona(model, usuario, rol);

        return new ModelAndView(model, "perfil.mustache");
    }

    // GET /configuracion
    public ModelAndView mostrarConfiguracion(Request req, Response res) {
        User usuario = resolverUsuarioSesion(req, res);
        if (usuario == null) return null;

        String rol = usuario.getString("rol");
        Map<String, Object> model = new HashMap<>();

        model.put("usuario",  usuario);
        model.put("esAdmin",  "ADMINISTRADOR".equals(rol));
        model.put("esDocente","DOCENTE".equals(rol));
        model.put("esAlumno", "ALUMNO".equals(rol));

        agregarDatosPersona(model, usuario, rol);

        // Mensajes del resultado de los POST (redirigen con query params)
        agregarMensaje(req, model, "successMessage", "successMessage");
        agregarMensaje(req, model, "errorMessage",   "errorMessage");

        return new ModelAndView(model, "configuracion.mustache");
    }

    // POST /configuracion/password
    public Object procesarCambioPassword(Request req, Response res) {
        User usuario = resolverUsuarioSesion(req, res);
        if (usuario == null) return null;

        String currentUsername = req.session().attribute("currentUserUsername");
        String passwordActual  = req.queryParams("password_actual");
        String passwordNueva   = req.queryParams("password_nueva");
        String passwordConfirm = req.queryParams("password_confirm");

        try {
            authService.cambiarPassword(usuario, passwordActual, passwordNueva, passwordConfirm);
            AuditoriaService.registrarAuditoria(req, "CAMBIO_PASSWORD", "Usuario: " + currentUsername);
            res.redirect("/configuracion?successMessage=Contrasena+actualizada+correctamente.");

        } catch (IllegalArgumentException e) {
            // Campos vacíos o contraseñas nuevas no coinciden
            res.redirect("/configuracion?errorMessage=" + encode(e.getMessage()));

        } catch (SecurityException e) {
            // Contraseña actual incorrecta
            res.redirect("/configuracion?errorMessage=" + encode(e.getMessage()));

        } catch (RuntimeException e) {
            // Error al guardar en BD
            res.redirect("/configuracion?errorMessage=" + encode(e.getMessage()));
        }

        return null;
    }

    // POST /configuracion/datos
    public Object procesarActualizacionDatos(Request req, Response res) {
        User usuario = resolverUsuarioSesion(req, res);
        if (usuario == null) return null;

        String currentUsername = req.session().attribute("currentUserUsername");
        String telefono        = req.queryParams("telefono");
        String direccion       = req.queryParams("direccion");

        if (telefono == null || telefono.isEmpty() || direccion == null || direccion.isEmpty()) {
            res.redirect("/configuracion?errorMessage=Telefono+y+direccion+son+obligatorios.");
            return null;
        }

        String  rol = usuario.getString("rol");
        Integer dni = resolverDniPorRol(usuario, rol);

        if (dni == null) {
            res.redirect("/configuracion?errorMessage=No+se+encontro+informacion+personal+asociada+a+tu+cuenta.");
            return null;
        }

        try {
            Base.exec(
                "UPDATE Persona SET telefono = ?, direccion = ? WHERE dni = ?",
                telefono, direccion, dni
            );
            AuditoriaService.registrarAuditoria(req, "ACTUALIZAR_DATOS", "Usuario: " + currentUsername);
            res.redirect("/configuracion?successMessage=Datos+personales+actualizados+correctamente.");
        } catch (Exception e) {
            res.redirect("/configuracion?errorMessage=Error+al+actualizar+los+datos:+" + encode(e.getMessage()));
        }

        return null;
    }

    // HELPERS PRIVADOS

    /**
     * Busca el User a partir del nombre guardado en sesión.
     * Si la sesión es inválida o el usuario no existe en BD, redirige y devuelve null.
     * Todos los métodos del controller lo llaman primero para no repetir este bloque.
     */
    private User resolverUsuarioSesion(Request req, Response res) {
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn       = req.session().attribute("loggedIn");

        if (currentUsername == null || loggedIn == null || !loggedIn) {
            res.redirect("/?error=Debes iniciar sesion para acceder a esta pagina.");
            return null;
        }

        User usuario = User.findFirst("name = ?", currentUsername);
        if (usuario == null) {
            req.session().invalidate();
            res.redirect("/?error=Sesion invalida.");
            return null;
        }

        return usuario;
    }

    /**
     * Agrega al model los datos de Persona (y Docente/Estudiante) según el rol.
     * Extrae el bloque duplicado que aparecía igual en /profile y /configuracion.
     */
    private void agregarDatosPersona(Map<String, Object> model, User usuario, String rol) {
        if ("DOCENTE".equals(rol)) {
            Docente docente = Docente.findFirst("user_id = ?", usuario.getInteger("id"));
            if (docente != null) {
                Persona persona = Persona.findFirst("dni = ?", docente.getInteger("dni"));
                model.put("docente", docente);
                model.put("persona", persona);
            }
        } else if ("ALUMNO".equals(rol)) {
            Estudiante estudiante = Estudiante.findFirst("user_id = ?", usuario.getInteger("id"));
            if (estudiante != null) {
                Persona persona = Persona.findFirst("dni = ?", estudiante.getInteger("dni"));
                model.put("estudiante", estudiante);
                model.put("persona", persona);
            }
        }
    }

    /**
     * Resuelve el DNI de la Persona asociada al usuario según su rol.
     * Devuelve null si el rol no tiene Persona asociada o no se encuentra.
     */
    private Integer resolverDniPorRol(User usuario, String rol) {
        if ("DOCENTE".equals(rol)) {
            Docente docente = Docente.findFirst("user_id = ?", usuario.getInteger("id"));
            return docente != null ? docente.getInteger("dni") : null;
        } else if ("ALUMNO".equals(rol)) {
            Estudiante estudiante = Estudiante.findFirst("user_id = ?", usuario.getInteger("id"));
            return estudiante != null ? estudiante.getInteger("dni") : null;
        }
        return null;
    }

    /**
     * Formatea el rol para mostrarlo con la primera letra en mayúscula.
     * Ejemplo: "ADMINISTRADOR" -> "Administrador"
     */
    private String formatearRol(String rol) {
        if (rol == null || rol.isEmpty()) return "";
        return rol.substring(0, 1).toUpperCase() + rol.substring(1).toLowerCase();
    }

    /**
     * Devuelve las clases CSS de Tailwind para el badge del rol en el dashboard.
     */
    private String resolverClaseRol(String rol) {
        if (rol == null) return "bg-gray-100 text-gray-700";
        switch (rol) {
            case "ADMINISTRADOR": return "bg-purple-100 text-purple-700";
            case "DOCENTE":       return "bg-blue-100 text-blue-700";
            case "ALUMNO":        return "bg-green-100 text-green-700";
            default:              return "bg-gray-100 text-gray-700";
        }
    }

    /**
     * Lee un query param del request y lo agrega al model con el nombre de clave dado,
     * solo si no es nulo ni vacío.
     *
     * @param queryParamName Nombre del query param a leer (ej. "error", "successMessage").
     * @param modelKey       Clave con la que se guarda en el model (ej. "errorMessage").
     */
    private void agregarMensaje(Request req, Map<String, Object> model,
                                 String queryParamName, String modelKey) {
        String valor = req.queryParams(queryParamName);
        if (valor != null && !valor.trim().isEmpty()) {
            model.put(modelKey, valor);
        }
    }

    /**
     * Codifica un mensaje para usarlo de forma segura en un query param de redirección.
     * Reemplaza espacios por + para compatibilidad con los query params del original.
     */
    private String encode(String mensaje) {
        if (mensaje == null) return "Error+desconocido.";
        return mensaje.replace(" ", "+");
    }
}