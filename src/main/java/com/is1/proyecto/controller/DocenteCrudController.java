package com.is1.proyecto.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.is1.proyecto.services.AuditoriaService;
import com.is1.proyecto.services.DocenteService;
public class DocenteCrudController implements CrudController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final DocenteService docenteService;

    public DocenteCrudController(DocenteService docenteService) {
        this.docenteService = docenteService;
    }

    @Override
    public ModelAndView listar(Request req, Response res) {
        List<Map<String, Object>> docentes = docenteService.listarDocentes();

        Map<String, Object> model = new HashMap<>();
        model.put("docentes", docentes);
        agregarMensajes(req, model);

        return new ModelAndView(model, "admin/docentes/docentesDashboard.mustache");
    }

    public ModelAndView mostrarListadoDocentes(Request req, Response res) {
        List<Map<String, Object>> docentes = DocenteService.listarDocentes();

        Map<String, Object> model = new HashMap<>();
        model.put("docentes", docentes);
        agregarMensajes(req, model);

        return new ModelAndView(model, "admin/docentes/listadoDocentes.mustache");
    }

    @Override
    public ModelAndView nuevo(Request req, Response res) {
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

    @Override
    public Object crear(Request req, Response res) {
        String nombre          = req.queryParams("nombre");
        String apellido        = req.queryParams("apellido");
        String dniString       = req.queryParams("dni");
        String email           = req.queryParams("email");
        String fechaNacimiento = req.queryParams("fecha_nacimiento");
        String telefono        = req.queryParams("telefono");
        String direccion       = req.queryParams("direccion");
        String username        = req.queryParams("username");

        try {
            docenteService.crearDocente(nombre, apellido, dniString, email,
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

    @Override
    public ModelAndView editar(Request req, Response res) {
        Integer codigoProfesor = Integer.parseInt(req.params(":id"));

        Map<String, Object> vista = docenteService.obtenerDocenteParaVista(codigoProfesor);

        if (vista == null) {
            res.redirect("/admin/docentes?errorMessage=Docente no encontrado.");
            return null;
        }

        agregarMensajes(req, vista);
        return new ModelAndView(vista, "admin/docentes/editarDocente.mustache");
    }

    @Override
    public Object actualizar(Request req, Response res) {
        Integer codigoProfesor = Integer.parseInt(req.params(":id"));

        String nombre          = req.queryParams("nombre");
        String apellido        = req.queryParams("apellido");
        String fechaNacimiento = req.queryParams("fecha_nacimiento");
        String telefono        = req.queryParams("telefono");
        String direccion       = req.queryParams("direccion");
        String email           = req.queryParams("email");

        try {
            docenteService.editarDocente(codigoProfesor, nombre, apellido,
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

        Map<String, Object> vista = docenteService.obtenerDocenteParaVista(codigoProfesor);

        if (vista == null) {
            res.redirect("/admin/docentes?errorMessage=Docente no encontrado.");
            return null;
        }

        return new ModelAndView(vista, "admin/docentes/eliminarDocente.mustache");
    }

    @Override
    public Object eliminar(Request req, Response res) {
        Integer codigoProfesor = Integer.parseInt(req.params(":id"));

        try {
            docenteService.eliminarDocente(codigoProfesor);

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
