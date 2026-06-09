package com.is1.proyecto.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.is1.proyecto.services.AuditoriaService;
import com.is1.proyecto.services.adminService.*;

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
 */
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final AdminDocenteService adminDocenteService;
    private final AdminEstudianteService adminEstudianteService;
    private final AdminPlanService adminPlanService;
    private final AdminCarreraService adminCarreraService;
    private final AdminMateriaService adminMateriaService;

    // Constructor
    public AdminController() {
        this.adminDocenteService = new AdminDocenteService();
        this.adminEstudianteService = new AdminEstudianteService();
        this.adminPlanService = new AdminPlanService();
        this.adminCarreraService = new AdminCarreraService();
        this.adminMateriaService = new AdminMateriaService();
    }

    // GET /admin  —  Dashboard de administración
    public ModelAndView mostrarAdminDashboard(Request req, Response res) {
        return new ModelAndView(new HashMap<>(), "admin/adminDashboard.mustache");
    }

    // === DOCENTES ===
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

    // ================== PLANES DE ESTUDIO ==================

    // GET /admin/planes
    public ModelAndView mostrarPlanes(Request req, Response res) {
        List<Map<String, Object>> planes = AdminPlanService.listarPlanes();

        Map<String, Object> model = new HashMap<>();
        model.put("planes", planes);
        agregarMensajes(req, model);

        return new ModelAndView(model, "admin/planes/planesDashboard.mustache");
    }

    // GET /admin/planes/agregar
    public ModelAndView mostrarFormularioAgregarPlan(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        model.put("carreras", adminPlanService.listarCarreras());
        agregarMensajes(req, model);

        return new ModelAndView(model, "admin/planes/agregarPlan.mustache");
    }

    // POST /admin/planes/agregar
    public Object procesarCrearPlan(Request req, Response res) {
        String anioStr = req.queryParams("anio");
        String vigenciaStr = req.queryParams("vigencia");
        String aniosTotalStr = req.queryParams("anios_total");
        String cantMatStr = req.queryParams("cantidad_materias_total");
        String codCarreraStr = req.queryParams("cod_carrera");

        try {
            adminPlanService.crearPlan(anioStr, vigenciaStr, aniosTotalStr, cantMatStr, codCarreraStr);

            AuditoriaService.registrarAuditoria(req, "CREAR_PLAN", 
                    "Año: " + anioStr + " - Carrera: " + codCarreraStr);

            res.redirect("/admin/planes?successMessage=Plan creado correctamente.");

        } catch (IllegalArgumentException e) {
            res.redirect("/admin/planes/agregar?errorMessage=" + encode(e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("Error al crear plan: {}", e.getMessage());
            res.redirect("/admin/planes/agregar?errorMessage=" + encode(e.getMessage()));
        }

        return null;
    }

    // GET /admin/planes/:id/edit
    public ModelAndView mostrarFormularioEditarPlan(Request req, Response res) {
        Integer codPlan = Integer.parseInt(req.params(":id"));

        Map<String, Object> plan = adminPlanService.obtenerPlanParaVista(codPlan);
        if (plan == null) {
            res.redirect("/admin/planes?errorMessage=Plan no encontrado.");
            return null;
        }

        Map<String, Object> carrerasData = adminPlanService.obtenerCarrerasParaSelect(codPlan);
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
    public Object procesarEditarPlan(Request req, Response res) {
        Integer codPlan = Integer.parseInt(req.params(":id"));

        String vigenciaStr = req.queryParams("vigencia");
        String aniosTotalStr = req.queryParams("anios_total");
        String cantMatStr = req.queryParams("cantidad_materias_total");
        String codCarreraStr = req.queryParams("cod_carrera");

        try {
            adminPlanService.editarPlan(codPlan, vigenciaStr, aniosTotalStr, cantMatStr, codCarreraStr);

            AuditoriaService.registrarAuditoria(req, "EDITAR_PLAN", "Código plan: " + codPlan);

            res.redirect("/admin/planes?successMessage=Plan actualizado correctamente.");

        } catch (IllegalArgumentException e) {
            res.redirect("/admin/planes/" + codPlan + "/edit?errorMessage=" + encode(e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("Error al editar plan {}: {}", codPlan, e.getMessage());
            res.redirect("/admin/planes/" + codPlan + "/edit?errorMessage=" + encode(e.getMessage()));
        }

        return null;
    }

    // GET /admin/planes/:id/delete
    public ModelAndView mostrarConfirmacionEliminarPlan(Request req, Response res) {
        Integer codPlan = Integer.parseInt(req.params(":id"));

        Map<String, Object> vista = adminPlanService.obtenerVistaEliminarPlan(codPlan);
        if (vista == null) {
            res.redirect("/admin/planes?errorMessage=Plan no encontrado.");
            return null;
        }

        agregarMensajes(req, vista);

        return new ModelAndView(vista, "admin/planes/eliminarPlan.mustache");
    }

    // POST /admin/planes/:id/delete
    public Object procesarEliminarPlan(Request req, Response res) {
        Integer codPlan = Integer.parseInt(req.params(":id"));

        try {
            adminPlanService.eliminarPlan(codPlan);

            AuditoriaService.registrarAuditoria(req, "ELIMINAR_PLAN", "Código plan: " + codPlan);

            res.redirect("/admin/planes?successMessage=Plan eliminado correctamente.");

        } catch (IllegalArgumentException e) {
            res.redirect("/admin/planes?errorMessage=" + encode(e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("Error al eliminar plan {}: {}", codPlan, e.getMessage());
            res.redirect("/admin/planes?errorMessage=" + encode(e.getMessage()));
        }

        return null;
    }

    // GET /admin/planes/:id/materias
    public ModelAndView mostrarMateriasDelPlan(Request req, Response res) {
        Integer codPlan = Integer.parseInt(req.params(":id"));

        Map<String, Object> vista = adminPlanService.obtenerMateriasPorPlan(codPlan);
        if (vista == null) {
            res.redirect("/admin/planes?errorMessage=Plan no encontrado.");
            return null;
        }

        agregarMensajes(req, vista);

        return new ModelAndView(vista, "admin/planes/materiasPlan.mustache");
    }
    
    // ================== CARRERAS ==================

    // GET /admin/carreras
    public ModelAndView mostrarCarreras(Request req, Response res) {
        List<Map<String, Object>> carreras = AdminCarreraService.listarCarreras();

        Map<String, Object> model = new HashMap<>();
        model.put("carreras", carreras);
        agregarMensajes(req, model);

        return new ModelAndView(model, "admin/carreras/carrerasDashboard.mustache");
    }

    // GET /admin/carreras/agregar
    public ModelAndView mostrarFormularioAgregarCarrera(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        agregarMensajes(req, model);

        return new ModelAndView(model, "admin/carreras/agregarCarrera.mustache");
    }

    // POST /admin/carreras/agregar
    public Object procesarCrearCarrera(Request req, Response res) {
        String nombre = req.queryParams("nombre");
        String descripcion = req.queryParams("descripcion");

        try {
            adminCarreraService.crearCarrera(nombre, descripcion);

            AuditoriaService.registrarAuditoria(req, "CREAR_CARRERA", "Nombre: " + nombre);

            res.redirect("/admin/carreras?successMessage=Carrera creada correctamente.");

        } catch (IllegalArgumentException e) {
            res.redirect("/admin/carreras/agregar?errorMessage=" + encode(e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("Error al crear carrera: {}", e.getMessage());
            res.redirect("/admin/carreras/agregar?errorMessage=" + encode(e.getMessage()));
        }

        return null;
    }

    // GET /admin/carreras/:id/edit
    public ModelAndView mostrarFormularioEditarCarrera(Request req, Response res) {
        Integer codCarrera = Integer.parseInt(req.params(":id"));

        Map<String, Object> carrera = adminCarreraService.obtenerCarreraParaVista(codCarrera);
        if (carrera == null) {
            res.redirect("/admin/carreras?errorMessage=Carrera no encontrada.");
            return null;
        }

        agregarMensajes(req, carrera);

        return new ModelAndView(carrera, "admin/carreras/editarCarrera.mustache");
    }

    // POST /admin/carreras/:id/edit
    public Object procesarEditarCarrera(Request req, Response res) {
        Integer codCarrera = Integer.parseInt(req.params(":id"));

        String nombre = req.queryParams("nombre");
        String descripcion = req.queryParams("descripcion");

        try {
            adminCarreraService.editarCarrera(codCarrera, nombre, descripcion);

            AuditoriaService.registrarAuditoria(req, "EDITAR_CARRERA", "Código: " + codCarrera);

            res.redirect("/admin/carreras?successMessage=Carrera actualizada correctamente.");

        } catch (IllegalArgumentException e) {
            res.redirect("/admin/carreras/" + codCarrera + "/edit?errorMessage=" + encode(e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("Error al editar carrera {}: {}", codCarrera, e.getMessage());
            res.redirect("/admin/carreras/" + codCarrera + "/edit?errorMessage=" + encode(e.getMessage()));
        }

        return null;
    }

    // GET /admin/carreras/:id/delete
    public ModelAndView mostrarConfirmacionEliminarCarrera(Request req, Response res) {
        Integer codCarrera = Integer.parseInt(req.params(":id"));

        Map<String, Object> vista = adminCarreraService.obtenerVistaEliminarCarrera(codCarrera);
        if (vista == null) {
            res.redirect("/admin/carreras?errorMessage=Carrera no encontrada.");
            return null;
        }

        return new ModelAndView(vista, "admin/carreras/eliminarCarrera.mustache");
    }

    // POST /admin/carreras/:id/delete
    public Object procesarEliminarCarrera(Request req, Response res) {
        Integer codCarrera = Integer.parseInt(req.params(":id"));

        try {
            adminCarreraService.eliminarCarrera(codCarrera);

            AuditoriaService.registrarAuditoria(req, "ELIMINAR_CARRERA", "Código: " + codCarrera);

            res.redirect("/admin/carreras?successMessage=Carrera eliminada correctamente.");

        } catch (IllegalArgumentException e) {
            res.redirect("/admin/carreras?errorMessage=" + encode(e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("Error al eliminar carrera {}: {}", codCarrera, e.getMessage());
            res.redirect("/admin/carreras?errorMessage=" + encode(e.getMessage()));
        }

        return null;
    }    

    // ================== MATERIAS ==================

    // GET /admin/materias
    public ModelAndView mostrarMaterias(Request req, Response res) {
        List<Map<String, Object>> materias = AdminMateriaService.listarMaterias();

        Map<String, Object> model = new HashMap<>();
        model.put("materias", materias);
        agregarMensajes(req, model);

        return new ModelAndView(model, "admin/materias/materiasDashboard.mustache");
    }

    // GET /admin/materias/listado
    public ModelAndView mostrarListadoMaterias(Request req, Response res) {
        List<Map<String, Object>> materias = AdminMateriaService.listarMateriasParaListado();

        Map<String, Object> model = new HashMap<>();
        model.put("materias", materias);
        agregarMensajes(req, model);

        return new ModelAndView(model, "admin/materias/listadoMaterias.mustache");
    }

    // GET /admin/materias/agregar
    public ModelAndView mostrarFormularioAgregarMateria(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        
        List<Map<String, Object>> planes = adminMateriaService.listarPlanes();
        model.put("planes", planes);
        model.put("sinPlanes", planes.isEmpty());
        agregarMensajes(req, model);

        return new ModelAndView(model, "admin/materias/agregarMateria.mustache");
    }

    // POST /admin/materias/agregar
    public Object procesarCrearMateria(Request req, Response res) {
        String nombre = req.queryParams("nombre");
        String codigo = req.queryParams("cod_materia");
        String descripcion = req.queryParams("descripcion");
        String codPlanStr = req.queryParams("cod_plan");

        try {
            adminMateriaService.crearMateria(nombre, codigo, descripcion, codPlanStr);

            AuditoriaService.registrarAuditoria(req, "CREAR_MATERIA", "Nombre: " + nombre);

            res.redirect("/admin/materias?successMessage=Materia creada correctamente.");

        } catch (IllegalArgumentException e) {
            res.redirect("/admin/materias/agregar?errorMessage=" + encode(e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("Error al crear materia: {}", e.getMessage());
            res.redirect("/admin/materias/agregar?errorMessage=" + encode(e.getMessage()));
        }

        return null;
    }

    // GET /admin/materias/:id/edit
    public ModelAndView mostrarFormularioEditarMateria(Request req, Response res) {
        Integer codMateria = Integer.parseInt(req.params(":id"));

        Map<String, Object> materia = adminMateriaService.obtenerMateriaParaVista(codMateria);
        if (materia == null) {
            res.redirect("/admin/materias?errorMessage=Materia no encontrada.");
            return null;
        }

        List<Map<String, Object>> planes = adminMateriaService.listarPlanesConSelected(
            (Integer) materia.get("codPlan")
        );

        Map<String, Object> model = new HashMap<>();
        model.putAll(materia);
        model.put("planes", planes);
        agregarMensajes(req, model);

        return new ModelAndView(model, "admin/materias/editarMateria.mustache");
    }

    // POST /admin/materias/:id/edit
    public Object procesarEditarMateria(Request req, Response res) {
        Integer codMateria = Integer.parseInt(req.params(":id"));

        String nombre = req.queryParams("nombre");
        String descripcion = req.queryParams("descripcion");
        String codPlanStr = req.queryParams("cod_plan");

        try {
            adminMateriaService.editarMateria(codMateria, nombre, descripcion, codPlanStr);

            AuditoriaService.registrarAuditoria(req, "EDITAR_MATERIA", "Código materia: " + codMateria);

            res.redirect("/admin/materias?successMessage=Materia actualizada correctamente.");

        } catch (IllegalArgumentException e) {
            res.redirect("/admin/materias/" + codMateria + "/edit?errorMessage=" + encode(e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("Error al editar materia {}: {}", codMateria, e.getMessage());
            res.redirect("/admin/materias/" + codMateria + "/edit?errorMessage=" + encode(e.getMessage()));
        }

        return null;
    }

    // GET /admin/materias/:id/delete
    public ModelAndView mostrarConfirmacionEliminarMateria(Request req, Response res) {
        Integer codMateria = Integer.parseInt(req.params(":id"));

        Map<String, Object> materia = adminMateriaService.obtenerMateriaParaVista(codMateria);
        if (materia == null) {
            res.redirect("/admin/materias?errorMessage=Materia no encontrada.");
            return null;
        }

        return new ModelAndView(materia, "admin/materias/eliminarMateria.mustache");
    }

    // POST /admin/materias/:id/delete
    public Object procesarEliminarMateria(Request req, Response res) {
        Integer codMateria = Integer.parseInt(req.params(":id"));

        try {
            adminMateriaService.eliminarMateria(codMateria);

            AuditoriaService.registrarAuditoria(req, "ELIMINAR_MATERIA", "Código materia: " + codMateria);

            res.redirect("/admin/materias?successMessage=Materia eliminada correctamente.");

        } catch (IllegalArgumentException e) {
            res.redirect("/admin/materias?errorMessage=" + encode(e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("Error al eliminar materia {}: {}", codMateria, e.getMessage());
            res.redirect("/admin/materias?errorMessage=" + encode(e.getMessage()));
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