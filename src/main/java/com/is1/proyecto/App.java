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
import static spark.Spark.exception;
import static spark.Spark.get; // Modelo de ActiveJDBC que representa la tabla 'users'.
import static spark.Spark.internalServerError;
import static spark.Spark.notFound;
import static spark.Spark.port;
import static spark.Spark.post;
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

        // --- Rutas Dashboard ---
        DashboardController dashboardController = new DashboardController(new AuthService());
        DashboardRoutes.register(dashboardController, engine);

        // --- Rutas Estudiante ---
        EstudianteController estudianteController = new EstudianteController();
        EstudianteRoutes.register(estudianteController, engine);

        // --- Rutas Docente ---
        DocenteController docenteController = new DocenteController();
        DocenteRoutes.register(docenteController, engine);

        // --- Rutas Admin ---
        AdminController adminController = new AdminController(new DocenteService());
        AdminRoutes.register(adminController, engine);

        // --- Rutas POST para manejar envíos de formularios y APIs ---






        // POST: Endpoint para añadir usuarios (API que devuelve JSON, no HTML).
        // Advertencia: Esta ruta tiene un propósito diferente a las de formulario HTML.
        post("/docente/new", (req, res) -> {

            String nombre = req.queryParams("nombre");
            String apellido = req.queryParams("apellido");
            String dniString = req.queryParams("dni");
            String email = req.queryParams("email");

            // NUEVOS DATOS
            String fechaNacimiento = req.queryParams("fecha_nacimiento");
            String telefono = req.queryParams("telefono");
            String direccion = req.queryParams("direccion");

            // USER ASOCIADO
            String username = req.queryParams("username");

            // =========================
            // VALIDACIONES
            // =========================

            if (
                dniString.isEmpty() ||
                nombre.isEmpty() ||
                apellido.isEmpty() ||
                email.isEmpty() ||
                fechaNacimiento.isEmpty() ||
                telefono.isEmpty() ||
                direccion.isEmpty() ||
                username.isEmpty()
            ) {

                res.redirect("/admin/docentes/agregar?errorMessage=Todos los campos son obligatorios.");
                return null;
            }

            // Email válido
            if (!esEmailValido(email)) {
                res.redirect("/admin/docentes/agregar?errorMessage=Ingrese un email valido.");
                return null;
            }

            // Email repetido
            Docente docenteExistente = Docente.findFirst("email = ?", email);

            if (docenteExistente != null) {
                res.redirect("/admin/docentes/agregar?errorMessage=Ya existe un docente con ese email.");
                return null;
            }

            // Verificar que exista el user
            User usuarioExistente = User.findFirst("name = ?", username);

            if (usuarioExistente == null) {
                res.redirect("/admin/docentes/agregar?errorMessage=No existe un usuario con ese nombre.");
                return null;
            }

            // Verificar que el usuario no esté asociado a otro docente
            Docente docenteConUsuario = Docente.findFirst("user_id = ?", usuarioExistente.getId());

            if (docenteConUsuario != null) {
                res.redirect("/admin/docentes/agregar?errorMessage=Ese usuario ya esta asociado a otro docente.");
                return null;
            }
            
            // Verificar que el user NO es ADMIN
            if ("ADMINISTRADOR".equals(usuarioExistente.getString("rol"))) {
                res.redirect("/admin/docentes/agregar?errorMessage=No puedes asignar un administrador como docente.");
                return null;
            }
            
            // VALIDAR NUMEROS VALIDOS
            Integer dni;
            try {
                dni = Integer.parseInt(dniString);
            } catch (NumberFormatException e) {
                res.redirect("/admin/docentes/agregar?errorMessage=DNI debe ser un numero valido.");
                return null;
            }

            // VALIDAR DNI NO REPETIDO
            Persona personaExistente = Persona.findFirst("dni = ?", dni);
            if (personaExistente != null) {
                res.redirect("/admin/docentes/agregar?errorMessage=Ya existe una persona registrada con ese DNI.");
                return null;
            }

            

            try {                
                // =========================
                // CREAR PERSONA
                // =========================

                Persona persona = new Persona();

                persona.setDni(dni);
                persona.setNombre(nombre);
                persona.setApellido(apellido);

                persona.setFechaNacimiento(fechaNacimiento);
                persona.setTelefono(telefono);
                persona.setDireccion(direccion);

                // =========================
                // CREAR DOCENTE
                // =========================

                Docente docente = new Docente();

                docente.setDNI(dni);
                docente.setEmail(email);

                // Asociar user
                docente.set("user_id", usuarioExistente.getId());

                // Guardar docente
                docente.saveIt();

                // Actualizar rol
                usuarioExistente.set("rol", "DOCENTE");
                usuarioExistente.saveIt();
                persona.saveIt();
                AuditoriaService.registrarAuditoria(req, "CREAR_DOCENTE", "DNI: " + dni + " - Usuario: " + username);

                res.redirect("/admin/docentes/agregar?successMessage=Docente agregado correctamente.");
                return null;
            } catch (Exception e) {

                String msg = e.getMessage();

                if (msg != null && msg.contains("UNIQUE constraint failed: Persona.dni")) {

                    res.redirect("/admin/docentes/agregar?errorMessage=Ya existe una persona registrada con ese DNI.");
                    return null;
                }

                res.redirect("/admin/docentes/agregar?errorMessage=Error al agregar docente: " + msg);
                return null;
            }
        });

        //! ESTUDIANTES
        post("/estudiante/new", (req, res) -> {

            String nombre = req.queryParams("nombre");
            String apellido = req.queryParams("apellido");
            String dniString = req.queryParams("dni");
            String email = req.queryParams("email");

            // NUEVOS DATOS
            String fechaNacimiento = req.queryParams("fecha_nacimiento");
            String telefono = req.queryParams("telefono");
            String direccion = req.queryParams("direccion");
            String nroLegajoString = req.queryParams("nro_legajo");    

            // USER ASOCIADO
            String username = req.queryParams("username");



            // =========================
            // VALIDACIONES
            // =========================

            if (
                dniString.isEmpty() ||
                nombre.isEmpty() ||
                apellido.isEmpty() ||
                email.isEmpty() ||
                fechaNacimiento.isEmpty() ||
                telefono.isEmpty() ||
                direccion.isEmpty() ||
                username.isEmpty() ||
                nroLegajoString.isEmpty()
            ) {

                res.redirect("/admin/estudiantes/agregar?errorMessage=Todos los campos son obligatorios.");
                return null;
            }

            // Email válido
            if (!esEmailValido(email)) {
                res.redirect("/admin/estudiantes/agregar?errorMessage=Ingrese un email valido.");
                return null;
            }

            // Email repetido
            Estudiante estudianteExistete = Estudiante.findFirst("email = ?", email);

            if (estudianteExistete != null) {
                res.redirect("/admin/estudiantes/agregar?errorMessage=Ya existe un estudiante con ese email.");
                return null;
            }

            // Verificar que exista el user
            User usuarioExistente = User.findFirst("name = ?", username);

            if (usuarioExistente == null) {
                res.redirect("/admin/estudiantes/agregar?errorMessage=No existe un usuario con ese nombre.");
                return null;
            }

            // Verificar que el usuario no esté asociado a otro docente
            Estudiante estudianteConUsuario = Estudiante.findFirst("user_id = ?", usuarioExistente.getId());

            if (estudianteConUsuario != null) {
                res.redirect("/admin/estudiantes/agregar?errorMessage=Ese usuario ya esta asociado a otro estudiante.");
                return null;
            }
            
            // Verificar que el user NO es ADMIN
            if ("ADMINISTRADOR".equals(usuarioExistente.getString("rol"))) {
                res.redirect("/admin/estudiantes/agregar?errorMessage=No puedes asignar un administrador como estudiante.");
                return null;
            }
            
            // VALIDAR NUMEROS VALIDOS
            Integer dni;
            Integer nro_legajo;
            try {
                dni = Integer.parseInt(dniString);
                nro_legajo = Integer.parseInt(nroLegajoString);
            } catch (NumberFormatException e) {
                res.redirect("/admin/estudiantes/agregar?errorMessage=DNI debe ser un numero valido.");
                return null;
            }

            // VALIDAR DNI NO REPETIDO
            Persona personaExistente = Persona.findFirst("dni = ?", dni);
            if (personaExistente != null) {
                res.redirect("/admin/estudiantes/agregar?errorMessage=Ya existe una persona registrada con ese DNI.");
                return null;
            }

            //  VALIDAR NRO LEGAJO NO REPETIDO
            if (Estudiante.findFirst("nro_legajo = ?", nro_legajo) != null) {
                res.redirect("/admin/estudiantes/agregar?errorMessage=Ya existe un estudiante con ese legajo.");
                return null;
            }   

            try {                
                // =========================
                // CREAR PERSONA
                // =========================

                Persona persona = new Persona();

                persona.setDni(dni);
                persona.setNombre(nombre);
                persona.setApellido(apellido);

                persona.setFechaNacimiento(fechaNacimiento);
                persona.setTelefono(telefono);
                persona.setDireccion(direccion);
                
                // =========================
                // CREAR ESTUDIANTE
                // =========================

                Estudiante estudiante = new Estudiante();

                estudiante.setDni(dni);
                estudiante.setEmail(email);
                estudiante.setNroLegajo(nro_legajo);

                // Asociar user
                estudiante.set("user_id", usuarioExistente.getId());

                // Guardar estudiante
                estudiante.saveIt();

                // Actualizar rol
                usuarioExistente.set("rol", "ALUMNO");
                usuarioExistente.saveIt();
                persona.saveIt();
                AuditoriaService.registrarAuditoria(req, "CREAR_ESTUDIANTE", "DNI: " + dni + " - Legajo: " + nro_legajo);
                res.redirect("/admin/estudiantes/agregar?successMessage=Estudiante agregado correctamente.");
                return null;
            } catch (Exception e) {

                String msg = e.getMessage();

                if (msg != null && msg.contains("UNIQUE constraint failed: Persona.dni")) {

                    res.redirect("/admin/estudiantes/agregar?errorMessage=Ya existe una persona registrada con ese DNI.");
                    return null;
                }

                res.redirect("/admin/estudiantes/agregar?errorMessage=Error al agregar estudiante: " + msg);
                return null;
            }
        });

        // Con esto podemos hacer localhost:puerto/admin/estudiantes/agregar
        get("/admin/estudiantes/agregar", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
        
            // Obtener y añadir mensaje de éxito de los query parameters (ej.
            // ?message=Cuenta creada!)
            String successMessage = req.queryParams("successMessage");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }

            // Obtener y añadir mensaje de error de los query parameters (ej. ?error=Campos
            // vacíos)
            String errorMessage = req.queryParams("errorMessage");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }

            // Inicializamos variables
            model.put("dni", "");
            model.put("nombre", "");
            model.put("apellido", "");
            model.put("email", "");

            model.put("fecha_nacimiento", "");
            model.put("telefono", "");
            model.put("direccion", "");
            model.put("username", "");

            // Renderizamos la plantilla
            return new ModelAndView(model, "admin/estudiantes/agregarEstudiante.mustache");
        }, new MustacheTemplateEngine());

        //! ESTUDIANTES
        get("/admin/estudiantes", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            List<Estudiante> estudiantesDB = Estudiante.findAll();

            List<Map<String, Object>> estudiantes = new ArrayList<>();

            for (Estudiante estudiante : estudiantesDB) {

                Map<String, Object> estudianteView = new HashMap<>();
                
                // DATOS ESTUDIANTE
                estudianteView.put("id", estudiante.getInteger("nro_legajo"));

                estudianteView.put("email", estudiante.getString("email"));

                // PERSONA
                Integer dni = estudiante.getInteger("dni");

                Persona persona = Persona.findFirst("dni = ?", dni);

                if (persona != null) {
                    estudianteView.put("dni", persona.getInteger("dni"));
                    estudianteView.put("nombre",persona.getString("nombre"));
                    estudianteView.put("apellido", persona.getString("apellido"));
                    estudianteView.put("telefono", persona.getString("telefono"));
                    estudianteView.put("direccion", persona.getString("direccion"));
                }
                
                // USER
                Integer userId = estudiante.getInteger("user_id");
                User user = User.findById(userId);

                if (user != null) {
                    estudianteView.put("username", user.getString("name"));
                }

                estudiantes.add(estudianteView);
            }

            model.put("estudiantes", estudiantes);

            model.put(
                    "successMessage",
                    req.queryParams("successMessage")
            );

            model.put(
                    "errorMessage",
                    req.queryParams("errorMessage")
            );

            return new ModelAndView(model,"admin/estudiantes/estudiantesDashboard.mustache");

        }, new MustacheTemplateEngine());

        get("/admin/estudiantes/:id/edit", (req, res) -> {

            Map<String, Object> model = new HashMap<>();

            Integer nro_legajo = Integer.parseInt(req.params(":id"));
            
            // ESTUDIANTE
            Estudiante estudiante = Estudiante.findFirst("nro_legajo = ?", nro_legajo);

            if (estudiante == null) {
                res.redirect("/admin/estudiantes?errorMessage=Error: estudiante no encontrado");
                return null;
            }

            // PERSONA
            Integer dni = estudiante.getInteger("dni");

            Persona persona = Persona.findFirst("dni = ?", dni);

            // MODEL
            model.put("nro_legajo", estudiante.getInteger("nro_legajo"));
            model.put("email", estudiante.getString("email"));
            model.put("dni", persona.getInteger("dni"));
            model.put("nombre", persona.getString("nombre"));
            model.put("apellido", persona.getString("apellido"));
            model.put("fechaNacimiento", persona.getString("fecha_nacimiento"));
            model.put("telefono", persona.getString("telefono"));
            model.put("direccion",persona.getString("direccion"));

            return new ModelAndView(model,"admin/estudiantes/editarEstudiante.mustache");

        }, new MustacheTemplateEngine());

        post("/admin/estudiantes/:id/edit", (req, res) -> {

            Integer nro_legajo = Integer.parseInt(req.params(":id"));

            Estudiante estudiante = Estudiante.findFirst("nro_legajo = ?", nro_legajo);

            if (estudiante == null) {

                res.redirect(
                        "/admin/estudiantes?errorMessage=Estudiante no encontrado"
                );

                return null;
            }

            Integer dni = estudiante.getInteger("dni");

            // FORM
            String nombre = req.queryParams("nombre");
            String apellido = req.queryParams("apellido");
            String fechaNacimiento = req.queryParams("fecha_nacimiento");
            String telefono = req.queryParams("telefono");
            String direccion = req.queryParams("direccion");
            String email = req.queryParams("email");

            try {

                Base.openTransaction();
                
                // UPDATE PERSONA
                Base.exec(
                        "UPDATE Persona " +
                        "SET nombre = ?, apellido = ?, fecha_nacimiento = ?, telefono = ?, direccion = ? " +
                        "WHERE dni = ?",

                        nombre,
                        apellido,
                        fechaNacimiento,
                        telefono,
                        direccion,
                        dni
                );
                
                // UPDATE ESTUDIANTE
                Base.exec(
                        "UPDATE Estudiante SET email = ? WHERE nro_legajo = ?",
                        email,
                        nro_legajo
                );

                Base.commitTransaction();
                AuditoriaService.registrarAuditoria(req, "EDITAR_ESTUDIANTE", "Legajo: " + nro_legajo);
                res.redirect(
                        "/admin/estudiantes?successMessage=Estudiante actualizado correctamente"
                );

                return null;

            } catch (Exception e) {

                Base.rollbackTransaction();

                e.printStackTrace();

                res.redirect(
                        "/admin/estudiantes/" + nro_legajo + "/edit?errorMessage=Error al actualizar estudiante");

                return null;
            }

        });

        get("/admin/estudiantes/:id/delete", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            Integer nro_legajo = Integer.parseInt(req.params(":id"));

            Estudiante estudiante = Estudiante.findFirst("nro_legajo = ?", nro_legajo);

            if (estudiante == null) {
                res.redirect("/admin/estudiantes?errorMessage=Estudiante no encontrado");
                return null;
            }

            Integer dni = estudiante.getInteger("dni");

            Persona persona = Persona.findFirst("dni = ?", dni);

            Integer userId = estudiante.getInteger("user_id");

            User user = User.findById(userId);

            model.put("nro_legajo", nro_legajo);
            model.put("email", estudiante.getString("email"));

            if (persona != null) {
                model.put("dni", persona.getInteger("dni"));
                model.put("nombre", persona.getString("nombre"));
                model.put("apellido", persona.getString("apellido"));
            }

            if (user != null) {
                model.put("username", user.getString("name"));
            }

            return new ModelAndView(model, "admin/estudiantes/eliminarEstudiante.mustache");

        }, new MustacheTemplateEngine());

        post("/admin/estudiantes/:id/delete", (req, res) -> {

            Integer nro_legajo = Integer.parseInt(req.params(":id"));

            Estudiante estudiante = Estudiante.findFirst("nro_legajo = ?", nro_legajo);
            if (estudiante == null) {
                res.redirect("/admin/estudiantes?errorMessage=Estudiante no encontrado");
                return null;
            }

            Integer dni = estudiante.getInteger("dni");
            Integer userId = estudiante.getInteger("user_id");

            User user = User.findById(userId);

            try {
                Base.openTransaction();

                // ELIMINAR ESTUDIANTE
                Base.exec("DELETE FROM Estudiante WHERE nro_legajo = ?", nro_legajo);

                // ELIMINAR PERSONA
                Base.exec("DELETE FROM Persona WHERE dni = ?", dni);

                // RESET ROL USER
                if (user != null) {
                    user.set("rol", "UNASSIGNED");
                    user.saveIt();
                }

                AuditoriaService.registrarAuditoria(req, "ELIMINAR_ESTUDIANTE", "Legajo: " + nro_legajo);
                Base.commitTransaction();
                res.redirect("/admin/estudiantes?successMessage=Estudiante eliminado correctamente");

                return null;

            } catch (Exception e) {
                Base.rollbackTransaction();
                e.printStackTrace();
                res.redirect("/admin/estudiantes?errorMessage=Error al eliminar estudiante");

                return null;
            }

        });

        get("/admin/estudiantes/:dni/materias", (req, res) -> {
            Integer dni = Integer.parseInt(req.params(":dni"));

            Estudiante estudiante = Estudiante.findFirst("dni = ?", dni);

            if(estudiante == null) {
                res.redirect("/admin/estudiantes?errorMessage=Estudiante no encontrado");
                return null;
            }

            Persona persona = Persona.findFirst("dni = ?", dni);

            Map<String, Object> model = new HashMap<>();

            model.put("dni", estudiante.getDni());
            model.put("nro_legajo", estudiante.getNroLegajo());

            if (persona != null) {
                model.put("nombre", persona.getNombre());
                model.put("apellido", persona.getApellido());
            }

            model.put("materias", Materia.findAll());

            return new ModelAndView(model, "admin/estudiantes/inscribirMateria.mustache");

        }, new MustacheTemplateEngine());

        post("/admin/estudiantes/:dni/inscribir", (req, res) -> {
            Integer dni = Integer.parseInt(req.params(":dni"));
            Integer codMateria = Integer.parseInt(req.queryParams("cod_materia"));

            Estudiante estudiante =Estudiante.findFirst("dni = ?", dni);
            if (estudiante == null) {
                res.redirect("/admin/estudiantes?errorMessage=Estudiante no encontrado.");

                return null;
            }

            try {
                estudiante.inscribirseMateria(codMateria);

                res.redirect("/admin/estudiantes?successMessage=Materia asignada correctamente.");

            } catch (Exception e) {

                res.redirect("/admin/estudiantes?errorMessage=" + java.net.URLEncoder.encode(e.getMessage(), "UTF-8"));
            }

            return null;
        });

        get("/admin/estudiantes/listado", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            List<Estudiante> estudiantesDB = Estudiante.findAll();
            List<Map<String, Object>> estudiantes = new ArrayList<>();

            for (Estudiante estudiante : estudiantesDB) {

                Map<String, Object> estudianteView = new HashMap<>();

                estudianteView.put("id", estudiante.getInteger("nro_legajo"));
                estudianteView.put("email", estudiante.getString("email"));

                Integer dni = estudiante.getInteger("dni");

                Persona persona = Persona.findFirst("dni = ?", dni);

                if (persona != null) {
                    estudianteView.put("dni", persona.getInteger("dni"));
                    estudianteView.put("nombre", persona.getString("nombre"));
                    estudianteView.put("apellido", persona.getString("apellido"));
                    estudianteView.put("telefono", persona.getString("telefono")); 
                    estudianteView.put("direccion", persona.getString("direccion"));
                    estudianteView.put("fecha_nacimiento", persona.getString("fecha_nacimiento"));
                }

                Integer userId = estudiante.getInteger("user_id");

                User user = User.findById(userId);

                if (user != null) {
                    estudianteView.put("username", user.getString("name"));
                }

                estudiantes.add(estudianteView);
            }

            model.put("estudiantes", estudiantes);

            return new ModelAndView(model,"admin/estudiantes/listadoEstudiantes.mustache");

        }, new MustacheTemplateEngine());


        // CRUD plan de estudios
        // GENERAL
        get("/admin/planes", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            List<PlanDeEstudios> planesDB = PlanDeEstudios.findAll();
            List<Map<String, Object>> planes = new ArrayList<>();

            for (PlanDeEstudios plan : planesDB) {
                Map<String, Object> planView = new HashMap<>();
                planView.put("codPlan",    plan.getCod());
                planView.put("anio",       plan.getAño());
                planView.put("vigencia",   plan.getVigencia());
                planView.put("aniosTotal", plan.getAñosTotal());
                planView.put("cantMaterias", plan.getCantidadMaterias());

                // Nombre de la carrera asociada
                Carrera carrera = Carrera.findFirst("cod_carrera = ?", plan.getCod());
                planView.put("nombreCarrera", carrera != null ? carrera.getNombre() : "Sin carrera");

                planes.add(planView);
            }

            model.put("planes", planes);
            model.put("successMessage", req.queryParams("successMessage"));
            model.put("errorMessage",   req.queryParams("errorMessage"));

            return new ModelAndView(model, "admin/planes/planesDashboard.mustache");
        }, new MustacheTemplateEngine());

        // FORMULARIO CREAR
        get("/admin/planes/agregar", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            // Pasar lista de carreras para el <select>
            List<Carrera> carrerasDB = Carrera.findAll();
            List<Map<String, Object>> carreras = new ArrayList<>();
            for (Carrera c : carrerasDB) {
                Map<String, Object> cv = new HashMap<>();
                cv.put("codCarrera", c.getCodigo());
                cv.put("nombre",     c.getNombre());
                carreras.add(cv);
            }
            model.put("carreras", carreras);
            model.put("successMessage", req.queryParams("successMessage"));
            model.put("errorMessage",   req.queryParams("errorMessage"));

            return new ModelAndView(model, "admin/planes/agregarPlan.mustache");
        }, new MustacheTemplateEngine());

        // CREAR
        post("/admin/planes/agregar", (req, res) -> {
            String anioStr        = req.queryParams("anio");
            String vigenciaStr    = req.queryParams("vigencia");
            String aniosTotalStr  = req.queryParams("anios_total");
            String cantMatStr     = req.queryParams("cantidad_materias_total");
            String codCarreraStr  = req.queryParams("cod_carrera");

            if (anioStr.isEmpty() || vigenciaStr.isEmpty() || aniosTotalStr.isEmpty()
                    || cantMatStr.isEmpty() || codCarreraStr.isEmpty()) {
                res.redirect("/admin/planes/agregar?errorMessage=Todos los campos son obligatorios.");
                return null;
            }

            try {
                int año        = Integer.parseInt(anioStr);
                int vigencia   = Integer.parseInt(vigenciaStr);
                int aniosTotal = Integer.parseInt(aniosTotalStr);
                int cantMat    = Integer.parseInt(cantMatStr);
                int codCarrera = Integer.parseInt(codCarreraStr);

                Carrera carrera = Carrera.findFirst("cod_carrera = ?", codCarrera);
                if (carrera == null) {
                    res.redirect("/admin/planes/agregar?errorMessage=La carrera seleccionada no existe.");
                    return null;
                }

                PlanDeEstudios plan = new PlanDeEstudios();
                plan.setAño(año);
                plan.setVigencia(vigencia);
                plan.setAñosTotal(aniosTotal);
                plan.setCantidadMaterias(cantMat);
                plan.set("cod_carrera", codCarrera); // <-- directo, sin pasar por setCod()
                plan.saveIt();
                AuditoriaService.registrarAuditoria(req, "CREAR_PLAN", "Año: " + año);
                res.redirect("/admin/planes?successMessage=Plan creado correctamente.");
                return null;

            } catch (NumberFormatException e) {
                res.redirect("/admin/planes/agregar?errorMessage=Los campos numéricos deben ser números válidos.");
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                res.redirect("/admin/planes/agregar?errorMessage=Error al crear el plan: " + e.getMessage());
                return null;
            }
        });

        // FORMULARIO EDITAR
        get("/admin/planes/:id/edit", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            int codPlan = Integer.parseInt(req.params(":id"));

            PlanDeEstudios plan = PlanDeEstudios.findFirst("cod_plan = ?", codPlan);
            if (plan == null) {
                res.redirect("/admin/planes?errorMessage=Plan no encontrado.");
                return null;
            }

            model.put("codPlan",      plan.getCod());
            model.put("anio",         plan.getAño());
            model.put("vigencia",     plan.getVigencia());
            model.put("aniosTotal",   plan.getAñosTotal());
            model.put("cantMaterias", plan.getCantidadMaterias());

            // Lista de carreras separadas para el <select>
            List<Carrera> carrerasDB = Carrera.findAll();
            List<Map<String, Object>> carreraSelected    = new ArrayList<>();
            List<Map<String, Object>> carrerasNoSelected = new ArrayList<>();

            for (Carrera c : carrerasDB) {
                Map<String, Object> cv = new HashMap<>();
                cv.put("codCarrera", c.getCodigo());
                cv.put("nombre",     c.getNombre());

                if (c.getCodigo().equals(plan.getCod())) {
                    carreraSelected.add(cv);
                } else {
                    carrerasNoSelected.add(cv);
                }
            }

            model.put("carreraSelected",    carreraSelected);
            model.put("carrerasNoSelected", carrerasNoSelected);
            model.put("errorMessage", req.queryParams("errorMessage"));

            return new ModelAndView(model, "admin/planes/editarPlan.mustache");
        }, new MustacheTemplateEngine());

        // EDITAR
        post("/admin/planes/:id/edit", (req, res) -> {
            int codPlan = Integer.parseInt(req.params(":id"));

            PlanDeEstudios plan = PlanDeEstudios.findFirst("cod_plan = ?", codPlan);
            if (plan == null) {
                res.redirect("/admin/planes?errorMessage=Plan no encontrado.");
                return null;
            }

            String vigenciaStr   = req.queryParams("vigencia");
            String aniosTotalStr = req.queryParams("anios_total");
            String cantMatStr    = req.queryParams("cantidad_materias_total");
            String codCarreraStr = req.queryParams("cod_carrera");

            if (vigenciaStr.isEmpty() || aniosTotalStr.isEmpty()
                    || cantMatStr.isEmpty() || codCarreraStr.isEmpty()) {
                res.redirect("/admin/planes/" + codPlan + "/edit?errorMessage=Todos los campos son obligatorios.");
                return null;
            }

            try {
                Base.openTransaction();

                Base.exec(
                    "UPDATE PlanDeEstudios SET vigencia = ?, años_total = ?, " +
                    "cantidad_materias_total = ?, cod_carrera = ? WHERE cod_plan = ?",
                    Integer.parseInt(vigenciaStr),
                    Integer.parseInt(aniosTotalStr),
                    Integer.parseInt(cantMatStr),
                    Integer.parseInt(codCarreraStr),
                    codPlan
                );

                Base.commitTransaction();
                res.redirect("/admin/planes?successMessage=Plan actualizado correctamente.");
                return null;

            } catch (Exception e) {
                Base.rollbackTransaction();
                e.printStackTrace();
                res.redirect("/admin/planes/" + codPlan + "/edit?errorMessage=Error al actualizar: " + e.getMessage());
                return null;
            }
        });

        // CONFIRMAR ELIMINAR
        get("/admin/planes/:id/delete", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            int codPlan = Integer.parseInt(req.params(":id"));

            PlanDeEstudios plan = PlanDeEstudios.findFirst("cod_plan = ?", codPlan);
            if (plan == null) {
                res.redirect("/admin/planes?errorMessage=Plan no encontrado.");
                return null;
            }

            Carrera carrera = Carrera.findFirst("cod_carrera = ?", plan.getCod());

            model.put("codPlan",      plan.getCod());
            model.put("anio",         plan.getAño());
            model.put("vigencia",     plan.getVigencia());
            model.put("nombreCarrera", carrera != null ? carrera.getNombre() : "Sin carrera");

            // Listar materias asociadas
            List<Materia> materiasDB = Materia.where("cod_plan = ?", codPlan);
            List<Map<String, Object>> materias = new ArrayList<>();
            for (Materia m : materiasDB) {
                Map<String, Object> mv = new HashMap<>();
                mv.put("codMateria", m.getInteger("cod_materia"));
                mv.put("nombre",     m.getString("nombre"));
                materias.add(mv);
            }
            model.put("materias",      materias);
            model.put("tieneMaterias", !materias.isEmpty());

            return new ModelAndView(model, "admin/planes/eliminarPlan.mustache");
        }, new MustacheTemplateEngine());

        // ELIMINAR
        post("/admin/planes/:id/delete", (req, res) -> {
            int codPlan = Integer.parseInt(req.params(":id"));

            PlanDeEstudios plan = PlanDeEstudios.findFirst("cod_plan = ?", codPlan);
            if (plan == null) {
                res.redirect("/admin/planes?errorMessage=Plan no encontrado.");
                return null;
            }

            try {
                Base.openTransaction();

                // Primero eliminar materias asociadas (integridad referencial)
                Base.exec("DELETE FROM Materia WHERE cod_plan = ?", codPlan);
                Base.exec("DELETE FROM PlanDeEstudios WHERE cod_plan = ?", codPlan);

                AuditoriaService.registrarAuditoria(req, "ELIMINAR_PLAN", "Código plan: " + codPlan);    
                Base.commitTransaction();
                res.redirect("/admin/planes?successMessage=Plan eliminado correctamente.");
                return null;

            } catch (Exception e) {
                Base.rollbackTransaction();
                e.printStackTrace();
                res.redirect("/admin/planes?errorMessage=Error al eliminar el plan.");
                return null;
            }
        });

        // LISTAR MATERIAS DE UN PLAN
        get("/admin/planes/:id/materias", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            int codPlan = Integer.parseInt(req.params(":id"));

            PlanDeEstudios plan = PlanDeEstudios.findFirst("cod_plan = ?", codPlan);
            if (plan == null) {
                res.redirect("/admin/planes?errorMessage=Plan no encontrado.");
                return null;
            }

            Carrera carrera = Carrera.findFirst("cod_carrera = ?", plan.getCod());

            List<Materia> materiasDB = Materia.where("cod_plan = ?", codPlan);
            List<Map<String, Object>> materias = new ArrayList<>();
            for (Materia m : materiasDB) {
                Map<String, Object> mv = new HashMap<>();
                mv.put("codMateria",  m.getInteger("cod_materia"));
                mv.put("nombre",      m.getString("nombre"));
                mv.put("descripcion", m.getString("descripcion"));
                materias.add(mv);
            }

            model.put("codPlan",      plan.getCod());
            model.put("anio",         plan.getAño());
            model.put("vigencia",     plan.getVigencia());
            model.put("nombreCarrera", carrera != null ? carrera.getNombre() : "Sin carrera");
            model.put("materias",     materias);
            model.put("sinMaterias",  materias.isEmpty());

            return new ModelAndView(model, "admin/planes/materiasPlan.mustache");
        }, new MustacheTemplateEngine());

        // CRUD de carreras
        // LISTAR
        get("/admin/carreras", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            List<Carrera> carrerasDB = Carrera.findAll();
            List<Map<String, Object>> carreras = new ArrayList<>();

            for (Carrera c : carrerasDB) {
                Map<String, Object> cv = new HashMap<>();
                cv.put("codCarrera",  c.getCodigo());
                cv.put("nombre",      c.getNombre());
                cv.put("descripcion", c.getDescripcion());
                carreras.add(cv);
            }

            model.put("carreras",       carreras);
            model.put("successMessage", req.queryParams("successMessage"));
            model.put("errorMessage",   req.queryParams("errorMessage"));

            return new ModelAndView(model, "admin/carreras/carrerasDashboard.mustache");
        }, new MustacheTemplateEngine());

        // FORMULARIO CREAR
        get("/admin/carreras/agregar", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("successMessage", req.queryParams("successMessage"));
            model.put("errorMessage",   req.queryParams("errorMessage"));

            return new ModelAndView(model, "admin/carreras/agregarCarrera.mustache");
        }, new MustacheTemplateEngine());

        // CREAR
        post("/admin/carreras/agregar", (req, res) -> {
            String nombre      = req.queryParams("nombre");
            String descripcion = req.queryParams("descripcion");

            if (nombre == null || nombre.isEmpty()) {
                res.redirect("/admin/carreras/agregar?errorMessage=El nombre es obligatorio.");
                return null;
            }

            try {
                Carrera carreraExistente = Carrera.findFirst("nombre = ?", nombre);
                if (carreraExistente != null) {
                    res.redirect("/admin/carreras/agregar?errorMessage=Ya existe una carrera con ese nombre.");
                    return null;
                }

                Carrera carrera = new Carrera();
                carrera.setNombre(nombre);
                carrera.setDescripcion(descripcion);
                carrera.saveIt();
                AuditoriaService.registrarAuditoria(req, "CREAR_CARRERA", "Nombre: " + nombre);
                res.redirect("/admin/carreras?successMessage=Carrera creada correctamente.");
                return null;

            } catch (Exception e) {
                e.printStackTrace();
                res.redirect("/admin/carreras/agregar?errorMessage=Error al crear la carrera: " + e.getMessage());
                return null;
            }
        });

        // FORMULARIO EDITAR
        get("/admin/carreras/:id/edit", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            int codCarrera = Integer.parseInt(req.params(":id"));

            Carrera carrera = Carrera.findFirst("cod_carrera = ?", codCarrera);
            if (carrera == null) {
                res.redirect("/admin/carreras?errorMessage=Carrera no encontrada.");
                return null;
            }

            model.put("codCarrera",   carrera.getCodigo());
            model.put("nombre",       carrera.getNombre());
            model.put("descripcion",  carrera.getDescripcion());
            model.put("errorMessage", req.queryParams("errorMessage"));

            return new ModelAndView(model, "admin/carreras/editarCarrera.mustache");
        }, new MustacheTemplateEngine());

        // EDITAR
        post("/admin/carreras/:id/edit", (req, res) -> {
            int codCarrera = Integer.parseInt(req.params(":id"));

            Carrera carrera = Carrera.findFirst("cod_carrera = ?", codCarrera);
            if (carrera == null) {
                res.redirect("/admin/carreras?errorMessage=Carrera no encontrada.");
                return null;
            }

            String nombre      = req.queryParams("nombre");
            String descripcion = req.queryParams("descripcion");

            if (nombre == null || nombre.isEmpty()) {
                res.redirect("/admin/carreras/" + codCarrera + "/edit?errorMessage=El nombre es obligatorio.");
                return null;
            }

            try {
                Base.exec(
                    "UPDATE Carrera SET nombre = ?, descripcion = ? WHERE cod_carrera = ?",
                    nombre, descripcion, codCarrera
                );

                res.redirect("/admin/carreras?successMessage=Carrera actualizada correctamente.");
                return null;

            } catch (Exception e) {
                e.printStackTrace();
                res.redirect("/admin/carreras/" + codCarrera + "/edit?errorMessage=Error al actualizar: " + e.getMessage());
                return null;
            }
        });

        // CONFIRMAR ELIMINAR
        get("/admin/carreras/:id/delete", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            int codCarrera = Integer.parseInt(req.params(":id"));

            Carrera carrera = Carrera.findFirst("cod_carrera = ?", codCarrera);
            if (carrera == null) {
                res.redirect("/admin/carreras?errorMessage=Carrera no encontrada.");
                return null;
            }

            // Verificar si tiene planes asociados
            PlanDeEstudios planAsociado = PlanDeEstudios.findFirst("cod_carrera = ?", codCarrera);

            model.put("codCarrera",   carrera.getCodigo());
            model.put("nombre",       carrera.getNombre());
            model.put("descripcion",  carrera.getDescripcion());
            model.put("tienePlanes",  planAsociado != null);

            return new ModelAndView(model, "admin/carreras/eliminarCarrera.mustache");
        }, new MustacheTemplateEngine());

        // ELIMINAR
        post("/admin/carreras/:id/delete", (req, res) -> {
            int codCarrera = Integer.parseInt(req.params(":id"));

            Carrera carrera = Carrera.findFirst("cod_carrera = ?", codCarrera);
            if (carrera == null) {
                res.redirect("/admin/carreras?errorMessage=Carrera no encontrada.");
                return null;
            }

            // Bloquear si tiene planes asociados
            PlanDeEstudios planAsociado = PlanDeEstudios.findFirst("cod_carrera = ?", codCarrera);
            if (planAsociado != null) {
                res.redirect("/admin/carreras?errorMessage=No se puede eliminar una carrera con planes asociados.");
                return null;
            }

            try {
                Base.exec("DELETE FROM Carrera WHERE cod_carrera = ?", codCarrera);
                AuditoriaService.registrarAuditoria(req, "ELIMINAR_CARRERA", "Código: " + codCarrera);
                res.redirect("/admin/carreras?successMessage=Carrera eliminada correctamente.");
                return null;

            } catch (Exception e) {
                e.printStackTrace();
                res.redirect("/admin/carreras?errorMessage=Error al eliminar la carrera.");
                return null;
            }
        });
        
        // MANEJO DE ERRORES
        // 404 (ejemplo: ir a una ruta que no existe)
        
        notFound((req, res) -> {
            res.type("text/html");
            logger.warn("404 - Ruta no encontrada: {}", req.url());
            return "<h1>404 - Pagina no encontrada</h1><p>La ruta <b>" + req.url() + "</b> no existe.</p><a href='/dashboard'>Volver al inicio</a>";
        });

        // 500
        internalServerError((req, res) -> {
            res.type("text/html");
            logger.error("500 - Error interno en: {}", req.url());
            return "<h1>500 - Error interno del servidor</h1><p>Ocurrió un error inesperado. Intente más tarde.</p><a href='/dashboard'>Volver al inicio</a>";
        });

        // Excepciones no capturadas
        exception(Exception.class, (e, req, res) -> {
            logger.error("Excepción no manejada en {}: {}", req.url(), e.getMessage(), e);
            res.status(500);
            res.type("text/html");
            res.body("<h1>500 - Error interno del servidor</h1><p>Ocurrió un error inesperado. Intente más tarde.</p><a href='/dashboard'>Volver al inicio</a>");
        });

        // Acceso no autorizado (ejemplo: intenta entrar a admin sin estar logueado)
        exception(spark.HaltException.class, (e, req, res) -> {
            logger.warn("Acceso detenido en {}: status {}", req.url(), e.statusCode());
        });

        //! MATERIAS 
        // GENERAL
        get("/admin/materias", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            List<Materia> materiasDB = Materia.findAll();
            List<Map<String, Object>> materias = new ArrayList<>();

            for (Materia m : materiasDB) {
                Map<String, Object> mv = new HashMap<>();
                mv.put("codMateria",          m.getCodMateria());
                mv.put("nombre",      m.getNombre());
                mv.put("descripcion", m.getDescripcion());

                PlanDeEstudios plan = PlanDeEstudios.findFirst("cod_plan = ?", m.getCodPlan());
                mv.put("nombrePlan", plan != null ? "Plan " + plan.getAño() : "Sin plan");
                Carrera carrera = Carrera.findFirst("cod_carrera = ?", plan.getCodCarrera());
                mv.put("carrera", carrera != null ? carrera.getNombre() : "Sin carrera");

                materias.add(mv);
            }

            model.put("materias",       materias);
            model.put("successMessage", req.queryParams("successMessage"));
            model.put("errorMessage",   req.queryParams("errorMessage"));

            return new ModelAndView(model, "admin/materias/materiasDashboard.mustache");
        }, new MustacheTemplateEngine());

        // FORMULARIO CREAR
        get("/admin/materias/agregar", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            List<PlanDeEstudios> planesDB = PlanDeEstudios.findAll();
            List<Map<String, Object>> planes = new ArrayList<>();
            for (PlanDeEstudios p : planesDB) {
                Map<String, Object> pv = new HashMap<>();
                pv.put("codPlan",    p.getCod());
                pv.put("nombrePlan", "Plan " + p.getAño());
                planes.add(pv);
            }

            model.put("planes",         planes);
            model.put("sinPlanes",      planes.isEmpty());
            model.put("errorMessage",   req.queryParams("errorMessage"));

            return new ModelAndView(model, "admin/materias/agregarMateria.mustache");
        }, new MustacheTemplateEngine());

        // AGREGAR
        post("/admin/materias/agregar", (req, res) -> {
            String nombre      = req.queryParams("nombre");
            String codigo      = req.queryParams("cod_materia");
            String descripcion = req.queryParams("descripcion");
            String codPlanStr  = req.queryParams("cod_plan");

            if (nombre == null || nombre.isEmpty() || codigo == null || codigo.isEmpty() || codPlanStr == null || codPlanStr.isEmpty()) {
                res.redirect("/admin/materias/agregar?errorMessage=Nombre, codigo y plan son obligatorios.");
                return null;
            }

            try {
                int codPlan = Integer.parseInt(codPlanStr);
                int codMat = Integer.parseInt(codigo);

                Materia materia = new Materia();
                materia.setNombre(nombre);
                materia.setDescripcion(descripcion);
                materia.setCodMateria(codMat);
                materia.setCodPlan(codPlan);
                materia.saveIt();
                AuditoriaService.registrarAuditoria(req, "CREAR_MATERIA", "Nombre: " + nombre);

                res.redirect("/admin/materias?successMessage=Materia creada correctamente.");
                return null;

            } catch (Exception e) {
                e.printStackTrace();
                res.redirect("/admin/materias/agregar?errorMessage=Error al crear la materia: " + e.getMessage());
                return null;
            }
        });

        // FORMULARIO EDITAR
        get("/admin/materias/:id/edit", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            int codMateria = Integer.parseInt(req.params(":id"));

            Materia materia = Materia.findFirst("cod_materia = ?", codMateria);
            if (materia == null) {
                res.redirect("/admin/materias?errorMessage=Materia no encontrada.");
                return null;
            }

            List<PlanDeEstudios> planesDB = PlanDeEstudios.findAll();
            List<Map<String, Object>> planes = new ArrayList<>();
            for (PlanDeEstudios p : planesDB) {
                Map<String, Object> pv = new HashMap<>();
                pv.put("codPlan",    p.getCod());
                pv.put("nombrePlan", "Plan " + p.getAño());
                pv.put("selected",   p.getCod().equals(materia.getCodPlan()));
                planes.add(pv);
            }

            model.put("codMateria",   materia.getCodMateria());
            model.put("nombre",       materia.getNombre());
            model.put("Codigo",       materia.getCodMateria());
            model.put("descripcion",  materia.getDescripcion());
            model.put("planes",       planes);
            model.put("errorMessage", req.queryParams("errorMessage"));

            return new ModelAndView(model, "admin/materias/editarMateria.mustache");
        }, new MustacheTemplateEngine());

        // EDITAR
        post("/admin/materias/:id/edit", (req, res) -> {
            int codMateria = Integer.parseInt(req.params(":id"));

            Materia materia = Materia.findFirst("cod_materia = ?", codMateria);
            if (materia == null) {
                res.redirect("/admin/materias?errorMessage=Materia no encontrada.");
                return null;
            }

            String nombre      = req.queryParams("nombre");
            String descripcion = req.queryParams("descripcion");
            String codPlanStr  = req.queryParams("cod_plan");

            if (nombre == null || nombre.isEmpty() || codPlanStr == null || codPlanStr.isEmpty()) {
                res.redirect("/admin/materias/" + codMateria + "/edit?errorMessage=Nombre, Codigo y Plan son obligatorios.");
                return null;
            }

            try {
                Base.exec(
                    "UPDATE Materia SET nombre = ?, descripcion = ?, cod_plan = ? WHERE cod_materia = ?",
                    nombre, descripcion, Integer.parseInt(codPlanStr), codMateria
                );

                res.redirect("/admin/materias?successMessage=Materia actualizada correctamente.");
                return null;

            } catch (Exception e) {
                e.printStackTrace();
                res.redirect("/admin/materias/" + codMateria + "/edit?errorMessage=Error al actualizar: " + e.getMessage());
                return null;
            }
        });

        // CONFIRMAR ELIMINAR
        get("/admin/materias/:id/delete", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            int codMateria = Integer.parseInt(req.params(":id"));

            Materia materia = Materia.findFirst("cod_materia = ?", codMateria);
            if (materia == null) {
                res.redirect("/admin/materias?errorMessage=Materia no encontrada.");
                return null;
            }

            model.put("codMateria",  materia.getCodMateria());
            model.put("nombre",      materia.getNombre());
            model.put("descripcion", materia.getDescripcion());

            return new ModelAndView(model, "admin/materias/eliminarMateria.mustache");
        }, new MustacheTemplateEngine());

        // ELIMINAR
        post("/admin/materias/:id/delete", (req, res) -> {
            int codMateria = Integer.parseInt(req.params(":id"));

            Materia materia = Materia.findFirst("cod_materia = ?", codMateria);
            if (materia == null) {
                res.redirect("/admin/materias?errorMessage=Materia no encontrada.");
                return null;
            }

            try {
                Base.openTransaction();
                Base.exec("DELETE FROM PeriodoAcademico WHERE cod_materia = ?", codMateria);
                Base.exec("DELETE FROM Materia WHERE cod_materia = ?", codMateria);
                AuditoriaService.registrarAuditoria(req, "ELIMINAR_MATERIA", "Código materia: " + codMateria);
                Base.commitTransaction();

                res.redirect("/admin/materias?successMessage=Materia eliminada correctamente.");
                return null;

            } catch (Exception e) {
                Base.rollbackTransaction();
                e.printStackTrace();
                res.redirect("/admin/materias?errorMessage=Error al eliminar la materia.");
                return null;
            }
        });

        // LISTAR
        get("/admin/materias/listado", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            List<Materia> materiasDB = Materia.findAll();
            List<Map<String, Object>> materias = new ArrayList<>();

            for(Materia materia : materiasDB){
                Map<String, Object> materiaView = new HashMap<>();

                materiaView.put("codMateria", materia.getCodMateria());
                materiaView.put("nombre", materia.getNombre());
                materiaView.put("descripcion", materia.getDescripcion());

                PlanDeEstudios plan = PlanDeEstudios.findFirst("cod_plan = ?", materia.getCodPlan());
                materiaView.put("nombrePlan", plan != null ? "Plan " + plan.getAño() : "Sin plan");
                Carrera carrera = Carrera.findFirst("cod_carrera = ?", plan.getCodCarrera());
                materiaView.put("carrera", carrera != null ? carrera.getNombre() : "Sin carrera");

                materias.add(materiaView);
            }

            model.put("materias", materias);
            return new ModelAndView(model, "/admin/materias/listadoMaterias.mustache");
        }, new MustacheTemplateEngine());
        
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