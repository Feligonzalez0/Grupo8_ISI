package com.is1.proyecto.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.is1.proyecto.services.AuditoriaService;
import com.is1.proyecto.services.AdminDocenteService;
import com.is1.proyecto.services.AdminEstudianteService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

/**
 * Controlador de administración.
 *
 * Responsabilidades:
 *   - Recibir requests HTTP de las rutas /admin/*.
 *   - Leer parámetros del formulario y de la URL.
 *   - Delegar toda la lógica de negocio al servicio correspondiente.
 *   - Construir el modelo de vista y devolver el ModelAndView, o redirigir.
 *
 * Rutas que maneja actualmente:
 *   GET  /admin
 *   GET  /admin/docentes
 *   GET  /admin/docentes/listado
 *   GET  /admin/docentes/agregar
 *   POST /admin/docentes/new
 *   GET  /admin/docentes/:id/edit
 *   POST /admin/docentes/:id/edit
 *   GET  /admin/docentes/:id/delete
 *   POST /admin/docentes/:id/delete
 */
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final AdminDocenteService adminDocenteService;
    private final AdminEstudianteService adminEstudianteService;


    // Constructor
    public AdminController(AdminDocenteService adminDocenteService, 
                        AdminEstudianteService adminEstudianteService) {
        this.adminDocenteService = adminDocenteService;
        this.adminEstudianteService = adminEstudianteService;
    }

    // GET /admin  —  Dashboard de administración
    public ModelAndView mostrarAdminDashboard(Request req, Response res) {
        return new ModelAndView(new HashMap<>(), "admin/adminDashboard.mustache");
    }

    // GET /admin/docentes  —  Listado de docentes (dashboard)
    public ModelAndView mostrarDocentes(Request req, Response res) {
        List<Map<String, Object>> docentes = AdminDocenteService.listarDocentes();

        Map<String, Object> model = new HashMap<>();
        model.put("docentes", docentes);
        agregarMensajes(req, model);

        return new ModelAndView(model, "admin/docentes/docentesDashboard.mustache");
    }

    // GET /admin/docentes/listado  —  Listado completo de docentes
    public ModelAndView mostrarListadoDocentes(Request req, Response res) {
        List<Map<String, Object>> docentes = AdminDocenteService.listarDocentes();

        Map<String, Object> model = new HashMap<>();
        model.put("docentes", docentes);
        agregarMensajes(req, model);

        return new ModelAndView(model, "admin/docentes/listadoDocentes.mustache");
    }

    // GET /admin/docentes/agregar  —  Formulario para agregar docente
    public ModelAndView mostrarFormularioAgregarDocente(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();

        // Campos vacíos para que el formulario Mustache no lance errores
        model.put("dni",              "");
        model.put("nombre",           "");
        model.put("apellido",         "");
        model.put("email",            "");
        model.put("fecha_nacimiento", "");
        model.put("telefono",         "");
        model.put("direccion",        "");
        model.put("username",         "");

        agregarMensajes(req, model);

        return new ModelAndView(model, "admin/docentes/agregarDocente.mustache");
    }

    // POST /admin/docentes/new  —  Procesar formulario de creación
    public Object procesarCrearDocente(Request req, Response res) {
        String nombre          = req.queryParams("nombre");
        String apellido        = req.queryParams("apellido");
        String dniString       = req.queryParams("dni");
        String email           = req.queryParams("email");
        String fechaNacimiento = req.queryParams("fecha_nacimiento");
        String telefono        = req.queryParams("telefono");
        String direccion       = req.queryParams("direccion");
        String username        = req.queryParams("username");

        try {
            adminDocenteService.crearDocente(nombre, apellido, dniString, email,
                                        fechaNacimiento, telefono, direccion, username);

            AuditoriaService.registrarAuditoria(req, "CREAR_DOCENTE",
                    "DNI: " + dniString + " - Usuario: " + username);

            res.redirect("/admin/docentes/agregar?successMessage=Docente agregado correctamente.");

        } catch (IllegalArgumentException e) {
            // Error de validación de negocio: redirigir con el mensaje del service
            res.redirect("/admin/docentes/agregar?errorMessage=" + encode(e.getMessage()));

        } catch (RuntimeException e) {
            // Error de persistencia
            logger.error("Error al crear docente: {}", e.getMessage());
            res.redirect("/admin/docentes/agregar?errorMessage=" + encode(e.getMessage()));
        }

        return null;
    }

    // GET /admin/docentes/:id/edit  —  Formulario de edición
    public ModelAndView mostrarFormularioEditarDocente(Request req, Response res) {
        Integer codigoProfesor = Integer.parseInt(req.params(":id"));

        Map<String, Object> vista = adminDocenteService.obtenerDocenteParaVista(codigoProfesor);

        if (vista == null) {
            res.redirect("/admin/docentes?errorMessage=Docente no encontrado.");
            return null;
        }

        agregarMensajes(req, vista);
        return new ModelAndView(vista, "admin/docentes/editarDocente.mustache");
    }

    // POST /admin/docentes/:id/edit  —  Procesar edición
    public Object procesarEditarDocente(Request req, Response res) {
        Integer codigoProfesor = Integer.parseInt(req.params(":id"));

        String nombre          = req.queryParams("nombre");
        String apellido        = req.queryParams("apellido");
        String fechaNacimiento = req.queryParams("fecha_nacimiento");
        String telefono        = req.queryParams("telefono");
        String direccion       = req.queryParams("direccion");
        String email           = req.queryParams("email");

        try {
            adminDocenteService.editarDocente(codigoProfesor, nombre, apellido,
                                          fechaNacimiento, telefono, direccion, email);

            AuditoriaService.registrarAuditoria(req, "EDITAR_DOCENTE",
                    "Código profesor: " + codigoProfesor);

            res.redirect("/admin/docentes?successMessage=Docente actualizado correctamente.");

        } catch (IllegalArgumentException e) {
            res.redirect("/admin/docentes?errorMessage=" + encode(e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("Error al editar docente {}: {}", codigoProfesor, e.getMessage());
            res.redirect("/admin/docentes/" + codigoProfesor + "/edit?errorMessage=Error al actualizar docente.");
        }

        return null;
    }

    // GET /admin/docentes/:id/delete  —  Confirmación de eliminación
    public ModelAndView mostrarConfirmacionEliminarDocente(Request req, Response res) {
        Integer codigoProfesor = Integer.parseInt(req.params(":id"));

        Map<String, Object> vista = adminDocenteService.obtenerDocenteParaVista(codigoProfesor);

        if (vista == null) {
            res.redirect("/admin/docentes?errorMessage=Docente no encontrado.");
            return null;
        }

        return new ModelAndView(vista, "admin/docentes/eliminarDocente.mustache");
    }

    // POST /admin/docentes/:id/delete  —  Procesar eliminación
    public Object procesarEliminarDocente(Request req, Response res) {
        Integer codigoProfesor = Integer.parseInt(req.params(":id"));

        try {
            adminDocenteService.eliminarDocente(codigoProfesor);

            AuditoriaService.registrarAuditoria(req, "ELIMINAR_DOCENTE",
                    "Código profesor: " + codigoProfesor);

            res.redirect("/admin/docentes?successMessage=Docente eliminado correctamente.");

        } catch (IllegalArgumentException e) {
            res.redirect("/admin/docentes?errorMessage=" + encode(e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("Error al eliminar docente {}: {}", codigoProfesor, e.getMessage());
            res.redirect("/admin/docentes?errorMessage=Error al eliminar docente.");
        }

        return null;
    }

    // GET /admin/docentes/:id/materias  —  Listar materias
    public ModelAndView mostrarMateriasDocente(Request req, Response res) {
        Integer codigoProfesor = Integer.parseInt(req.params(":id"));

        Map<String, Object> model =
                adminDocenteService.obtenerVistaMateriasDocente(codigoProfesor);

        if (model == null) {
            res.redirect("/admin/docentes?errorMessage=Docente no encontrado.");
            return null;
        }

        model.put("successMessage", req.queryParams("successMessage"));
        model.put("errorMessage", req.queryParams("errorMessage"));

        return new ModelAndView(
            model,
            "admin/docentes/materiasDocente.mustache"
        );
    }

    // POST /admin/docentes/:id/agregar  —  Procesar agregar materia
    public Object procesarAsignarMateria(Request req, Response res) {
        Integer codigoProfesor = Integer.parseInt(req.params(":id"));

        try {
            adminDocenteService.asignarMateria(
                    codigoProfesor,
                    req.queryParams("cod_materia"),
                    req.queryParams("fecha"),
                    req.queryParams("cargo")
            );

            AuditoriaService.registrarAuditoria(req, "ASIGNAR_MATERIA_DOCENTE", "codigoProfesor:" + codigoProfesor);
            res.redirect("/admin/docentes/" + codigoProfesor + "/materias?successMessage=Materia asignada correctamente.");
        } catch (IllegalArgumentException e) {
            res.redirect(
                "/admin/docentes/" + codigoProfesor +
                "/materias?errorMessage=" + e.getMessage()
            );
        }

        return null;
    }

    // POST /admin/docentes/:id/materias/:asignacionId/delete  —  Procesar quitar materia
    public Object procesarQuitarMateria(Request req, Response res) {
        Integer codigoProfesor = Integer.parseInt(req.params(":id"));
        Integer asignacionId   = Integer.parseInt(req.params(":asignacionId"));

        try {
            adminDocenteService.quitarMateria(codigoProfesor, asignacionId);

            AuditoriaService.registrarAuditoria(req,"QUITAR_MATERIA_DOCENTE", "codigoProfesor:" + codigoProfesor + " asignacionId:" + asignacionId);

            res.redirect("/admin/docentes/" + codigoProfesor + "/materias?successMessage=Materia quitada correctamente.");

        } catch (Exception e) {
            res.redirect("/admin/docentes/" + codigoProfesor + "/materias?errorMessage=Error al quitar la materia.");
        }

        return null;
    }
    // ================== ESTUDIANTES ==================

    // GET /admin/estudiantes
    public ModelAndView mostrarEstudiantes(Request req, Response res) {
        List<Map<String, Object>> estudiantes = AdminEstudianteService.listarEstudiantes();

        Map<String, Object> model = new HashMap<>();
        model.put("estudiantes", estudiantes);
        agregarMensajes(req, model);

        return new ModelAndView(model, "admin/estudiantes/estudiantesDashboard.mustache");
    }

    // GET /admin/estudiantes/listado
    public ModelAndView mostrarListadoEstudiantes(Request req, Response res) {
        List<Map<String, Object>> estudiantes = AdminEstudianteService.listarEstudiantes();

        Map<String, Object> model = new HashMap<>();
        model.put("estudiantes", estudiantes);
        agregarMensajes(req, model);

        return new ModelAndView(model, "admin/estudiantes/listadoEstudiantes.mustache");
    }

    // GET /admin/estudiantes/agregar
    public ModelAndView mostrarFormularioAgregarEstudiante(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();

        // Campos vacíos
        model.put("dni", "");
        model.put("nombre", "");
        model.put("apellido", "");
        model.put("email", "");
        model.put("fecha_nacimiento", "");
        model.put("telefono", "");
        model.put("direccion", "");
        model.put("username", "");
        model.put("nro_legajo", "");

        agregarMensajes(req, model);

        return new ModelAndView(model, "admin/estudiantes/agregarEstudiante.mustache");
    }

    // POST /admin/estudiantes/new
    public Object procesarCrearEstudiante(Request req, Response res) {
        String nombre = req.queryParams("nombre");
        String apellido = req.queryParams("apellido");
        String dniString = req.queryParams("dni");
        String email = req.queryParams("email");
        String fechaNacimiento = req.queryParams("fecha_nacimiento");
        String telefono = req.queryParams("telefono");
        String direccion = req.queryParams("direccion");
        String username = req.queryParams("username");
        String nroLegajoString = req.queryParams("nro_legajo");

        try {
            adminEstudianteService.crearEstudiante(nombre, apellido, dniString, email,
                    fechaNacimiento, telefono, direccion, username, nroLegajoString);

            AuditoriaService.registrarAuditoria(req, "CREAR_ESTUDIANTE",
                    "DNI: " + dniString + " - Legajo: " + nroLegajoString);

            res.redirect("/admin/estudiantes/agregar?successMessage=Estudiante agregado correctamente.");

        } catch (IllegalArgumentException e) {
            res.redirect("/admin/estudiantes/agregar?errorMessage=" + encode(e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("Error al crear estudiante: {}", e.getMessage());
            res.redirect("/admin/estudiantes/agregar?errorMessage=" + encode(e.getMessage()));
        }

        return null;
    }

    // GET /admin/estudiantes/:id/edit
    public ModelAndView mostrarFormularioEditarEstudiante(Request req, Response res) {
        Integer nroLegajo = Integer.parseInt(req.params(":id"));

        Map<String, Object> vista = adminEstudianteService.obtenerEstudianteParaVista(nroLegajo);

        if (vista == null) {
            res.redirect("/admin/estudiantes?errorMessage=Estudiante no encontrado.");
            return null;
        }

        agregarMensajes(req, vista);
        return new ModelAndView(vista, "admin/estudiantes/editarEstudiante.mustache");
    }

    // POST /admin/estudiantes/:id/edit
    public Object procesarEditarEstudiante(Request req, Response res) {
        Integer nroLegajo = Integer.parseInt(req.params(":id"));

        String nombre = req.queryParams("nombre");
        String apellido = req.queryParams("apellido");
        String fechaNacimiento = req.queryParams("fecha_nacimiento");
        String telefono = req.queryParams("telefono");
        String direccion = req.queryParams("direccion");
        String email = req.queryParams("email");

        try {
            adminEstudianteService.editarEstudiante(nroLegajo, nombre, apellido,
                    fechaNacimiento, telefono, direccion, email);

            AuditoriaService.registrarAuditoria(req, "EDITAR_ESTUDIANTE",
                    "Legajo: " + nroLegajo);

            res.redirect("/admin/estudiantes?successMessage=Estudiante actualizado correctamente.");

        } catch (IllegalArgumentException e) {
            res.redirect("/admin/estudiantes?errorMessage=" + encode(e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("Error al editar estudiante {}: {}", nroLegajo, e.getMessage());
            res.redirect("/admin/estudiantes/" + nroLegajo + "/edit?errorMessage=Error al actualizar estudiante.");
        }

        return null;
    }

    // GET /admin/estudiantes/:id/delete
    public ModelAndView mostrarConfirmacionEliminarEstudiante(Request req, Response res) {
        Integer nroLegajo = Integer.parseInt(req.params(":id"));

        Map<String, Object> vista = adminEstudianteService.obtenerEstudianteParaVista(nroLegajo);

        if (vista == null) {
            res.redirect("/admin/estudiantes?errorMessage=Estudiante no encontrado.");
            return null;
        }

        return new ModelAndView(vista, "admin/estudiantes/eliminarEstudiante.mustache");
    }

    // POST /admin/estudiantes/:id/delete
    public Object procesarEliminarEstudiante(Request req, Response res) {
        Integer nroLegajo = Integer.parseInt(req.params(":id"));

        try {
            adminEstudianteService.eliminarEstudiante(nroLegajo);

            AuditoriaService.registrarAuditoria(req, "ELIMINAR_ESTUDIANTE",
                    "Legajo: " + nroLegajo);

            res.redirect("/admin/estudiantes?successMessage=Estudiante eliminado correctamente.");

        } catch (IllegalArgumentException e) {
            res.redirect("/admin/estudiantes?errorMessage=" + encode(e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("Error al eliminar estudiante {}: {}", nroLegajo, e.getMessage());
            res.redirect("/admin/estudiantes?errorMessage=Error al eliminar estudiante.");
        }

        return null;
    }

    // GET /admin/estudiantes/:legajo/materias
    public ModelAndView mostrarMateriasParaInscribir(Request req, Response res) {
        Integer nroLegajo = Integer.parseInt(req.params(":legajo"));

        Map<String, Object> model = adminEstudianteService.obtenerVistaMateriasEstudiante(nroLegajo);

        if (model == null) {
            res.redirect("/admin/estudiantes?errorMessage=Estudiante no encontrado.");
            return null;
        }

        agregarMensajes(req, model);
        return new ModelAndView(model, "admin/estudiantes/inscribirMateria.mustache");
    }

    // POST /admin/estudiantes/:legajo/inscribir
    public Object procesarInscribirMateria(Request req, Response res) {
        Integer nroLegajo = Integer.parseInt(req.params(":legajo"));
        Integer codMateria = Integer.parseInt(req.queryParams("cod_materia"));

        try {
            adminEstudianteService.inscribirMateria(nroLegajo, codMateria);

            AuditoriaService.registrarAuditoria(req, "INSCRIBIR_MATERIA_ESTUDIANTE",
                    "Legajo: " + nroLegajo + " - Materia: " + codMateria);

            res.redirect("/admin/estudiantes?successMessage=Materia asignada correctamente.");

        } catch (IllegalArgumentException e) {
            res.redirect("/admin/estudiantes?errorMessage=" + encode(e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("Error al inscribir materia: {}", e.getMessage());
            res.redirect("/admin/estudiantes?errorMessage=" + encode(e.getMessage()));
        }

        return null;
    }
    // HELPERS PRIVADOS
    private void agregarMensajes(Request req, Map<String, Object> model) {
        String success = req.queryParams("successMessage");
        String error   = req.queryParams("errorMessage");
        if (success != null && !success.trim().isEmpty()) model.put("successMessage", success);
        if (error   != null && !error.trim().isEmpty())   model.put("errorMessage",   error);
    }

    /**
     * Codifica un mensaje para usarlo en un query param de redirección.
     */
    private String encode(String mensaje) {
        if (mensaje == null) return "Error+desconocido.";
        return mensaje.replace(" ", "+");
    }
}