package com.is1.proyecto; // Define el paquete de la aplicación, debe coincidir con la estructura de carpetas.

import java.util.ArrayList;
import java.util.HashMap; // Utilidad para serializar/deserializar objetos Java a/desde JSON.
import java.util.List;
import java.util.Map; // Importa los métodos estáticos principales de Spark (get, post, before, after, etc.).

import org.javalite.activejdbc.Base; // Clase central de ActiveJDBC para gestionar la conexión a la base de datos.
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper; // Representa un modelo de datos y el nombre de la vista a renderizar.
import com.is1.proyecto.config.DBConfigSingleton; // Motor de plantillas Mustache para Spark.
import com.is1.proyecto.controller.*;
import com.is1.proyecto.filters.AuthFilter;
import com.is1.proyecto.models.*;
import com.is1.proyecto.routes.AdminRoutes;
import com.is1.proyecto.routes.AuthRoutes;
import com.is1.proyecto.routes.DashboardRoutes;
import com.is1.proyecto.services.*;
import spark.ModelAndView; 
import spark.Request;
import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get; // Modelo de ActiveJDBC que representa la tabla 'users'.
import static spark.Spark.halt;
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

    // Instancia estática y final de ObjectMapper para la
    // serialización/deserialización JSON.
    // Se inicializa una sola vez para ser reutilizada en toda la aplicación.
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private static void ejecutarScheme() {
    try {
        String sql = new String(
            App.class.getClassLoader()
                .getResourceAsStream("scheme.sql")
                .readAllBytes()
        );
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
        port(8080); // Configura el puerto en el que la aplicación Spark escuchará las peticiones
                    // (por defecto es 8080).

        // Obtener la instancia única del singleton de configuración de la base de
        // datos.
        DBConfigSingleton dbConfig = DBConfigSingleton.getInstance();
        logger.info("Base de datos usada: {}", dbConfig.getDbUrl());
        AuthFilter.registerAll(dbConfig);

        try {
            Base.open(
                dbConfig.getDriver(),
                dbConfig.getDbUrl(),
                dbConfig.getUser(),
                dbConfig.getPass()
            );

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
            Integer codigoProfesor;
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

        // ===== DASHBOARD ESTUDIANTE =====
        get("/estudiante/dashboard", (req, res) -> {
            Integer userId = req.session().attribute("userId");
            String userRol = req.session().attribute("userRol");
 
            if (userId == null || !"ALUMNO".equals(userRol)) {
                res.redirect("/dashboard?error=Acceso no autorizado.");
                return null;
            }
 
            Estudiante estudiante = Estudiante.findFirst("user_id = ?", userId);
            if (estudiante == null) {
                res.redirect("/dashboard?error=No se encontro el perfil de estudiante.");
                return null;
            }
 
            Persona persona = Persona.findFirst("dni = ?", estudiante.getDni());
 
            Map<String, Object> model = new HashMap<>();
            model.put("nombre",    persona != null ? persona.getNombre()   : "");
            model.put("apellido",  persona != null ? persona.getApellido() : "");
            model.put("nroLegajo", estudiante.getNroLegajo());
 
            String error = req.queryParams("error");
            if (error != null) model.put("errorMessage", error);
 
            return new ModelAndView(model, "estudiante/estudianteDashboard.mustache");
        }, new MustacheTemplateEngine());
        
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

        get("/estudiante/inscripcion", (req, res) -> {
            Integer userId = req.session().attribute("userId");
            String userRol = req.session().attribute("userRol");

            if(userId == null || !"ALUMNO".equals(userRol)) {
                res.redirect("/dashboard?error=Acceso no autorizado.");
                return null;
            }

            Estudiante estudiante = Estudiante.findFirst("user_id = ?", userId);
            if(estudiante == null) {
                res.redirect("/dashboard?error=No se encontró el perfil de estudiante.");
                return null;
            }

            Persona persona = Persona.findFirst("dni = ?", estudiante.getDni());

            List<Estado> estadosActuales = Estado.where("dni_estudiante = ?", estudiante.getDni());
            List<Integer> codMateriasInscriptas = new ArrayList<>();
            for(Estado e : estadosActuales) {
                codMateriasInscriptas.add(e.getCodMateria());
            }

            List<Integer> codMateriasAprobadas = new ArrayList<>();
            for(Estado e : estadosActuales) {
                if("APROBADO".equals(e.getString("estado"))) {
                    codMateriasAprobadas.add(e.getCodMateria());
                }
            }

            InscripcionCarrera inscripcionCarrera = InscripcionCarrera.findFirst("dni_estudiante = ?", estudiante.getDni());

            if(inscripcionCarrera == null) {
                res.redirect("/dashboard?error=No estás inscripto en ninguna carrera.");
                return null;
            }

            PlanDeEstudios plan = PlanDeEstudios.findFirst( "cod_carrera = ?", inscripcionCarrera.getCodCarrera());
            if(plan == null) {
                res.redirect("/dashboard?error=La carrera no tiene plan de estudios.");
                return null;
            }

            List<Materia> materiasDelPlan = Materia.where("cod_plan = ?", plan.getCod());

            List<Map<String, Object>> materiasDisponibles = new ArrayList<>();
            List<Map<String, Object>> materiasInscriptas  = new ArrayList<>();

            for(Materia m : materiasDelPlan) {
                Integer codMat = m.getCodMateria();

                if(codMateriasInscriptas.contains(codMat)) {
                    Map<String, Object> mv = new HashMap<>();

                    mv.put("codMateria", codMat);
                    mv.put("nombre", m.getNombre());

                    Estado est = Estado.findFirst("dni_estudiante = ? AND cod_materia = ?", estudiante.getDni(), codMat);
                    mv.put("estado", est != null ? est.getString("estado") : "");

                    materiasInscriptas.add(mv);

                    continue;
                }

                List<Correlatividad> correlativas = Correlatividad.where("cod_materia = ?", codMat);
                boolean cumpleCorrelativas = true;
                List<String> faltantes = new ArrayList<>();

                for(Correlatividad c : correlativas) {
                    if(!codMateriasAprobadas.contains(
                            c.getCodCorrelativa())) {

                        cumpleCorrelativas = false;

                        Materia mc = Materia.findFirst("cod_materia = ?", c.getCodCorrelativa());

                        faltantes.add(mc != null ? mc.getNombre() : "Cód. " + c.getCodCorrelativa());
                    }
                }

                Map<String, Object> mv = new HashMap<>();

                mv.put("codMateria", codMat);
                mv.put("nombre", m.getNombre());
                mv.put("descripcion", m.getDescripcion());
                mv.put("puedeInscribirse", cumpleCorrelativas);
                mv.put("tieneCorrelativas", !correlativas.isEmpty());
                mv.put("correlativasFaltantes", String.join(", ", faltantes));

                materiasDisponibles.add(mv);
            }
    
            Map<String, Object> model = new HashMap<>();
            model.put("dni", estudiante.getDni());
            model.put("nroLegajo", estudiante.getNroLegajo());
            model.put("nombre", persona != null ? persona.getNombre() : "");
            model.put("apellido", persona != null ? persona.getApellido() : "");
            model.put("materias", materiasDisponibles);
            model.put("sinMaterias", materiasDisponibles.isEmpty());
            model.put("inscriptas", materiasInscriptas);
            model.put("tieneInscriptas", !materiasInscriptas.isEmpty());
            model.put("successMessage", req.queryParams("successMessage"));
            model.put("errorMessage", req.queryParams("errorMessage"));
    
            return new ModelAndView(model, "estudiante/inscripcion.mustache");

        }, new MustacheTemplateEngine());
    
        post("/estudiante/inscripcion", (req, res) -> {
            Integer userId = req.session().attribute("userId");
            String userRol = req.session().attribute("userRol");
    
            if(userId == null || !"ALUMNO".equals(userRol)) {
                res.redirect("/dashboard?error=Acceso no autorizado.");
                return null;
            }
    
            Estudiante estudiante = Estudiante.findFirst("user_id = ?", userId);
            if(estudiante == null) {
                res.redirect("/dashboard?error=No se encontró el perfil de estudiante.");
                return null;
            }
    
            String codMateriaStr = req.queryParams("cod_materia");
            if(codMateriaStr == null || codMateriaStr.isEmpty()) {
                res.redirect("/estudiante/inscripcion?errorMessage=Debe seleccionar una materia.");
                return null;
            }
    
            int codMateria = Integer.parseInt(codMateriaStr);
    
            Materia materia = Materia.findFirst("cod_materia = ?", codMateria);

            if(materia == null) {
                res.redirect(
                    "/estudiante/inscripcion?errorMessage=La materia no existe."
                );
                return null;
            }

            Estado yaInscripto = Estado.findFirst("dni_estudiante = ? AND cod_materia = ?", estudiante.getDni(), codMateria);
            if (yaInscripto != null) {
                res.redirect("/estudiante/inscripcion?errorMessage=Ya estás inscripto en esa materia.");

                return null;
            }

            List<Correlatividad> correlativas = Correlatividad.where("cod_materia = ?", codMateria);
            if(!correlativas.isEmpty()) {
                List<Estado> aprobadas = Estado.where("dni_estudiante = ? AND estado = 'APROBADO'", estudiante.getDni());
                List<Integer> codAprobadas = new ArrayList<>();
                for(Estado e : aprobadas) codAprobadas.add(e.getCodMateria());
    
                List<String> faltantes = new ArrayList<>();
                for(Correlatividad c : correlativas) {
                    if(!codAprobadas.contains(c.getCodCorrelativa())) {
                        Materia mc = Materia.findFirst("cod_materia = ?", c.getCodCorrelativa());
                        faltantes.add(mc != null ? mc.getNombre() : "Cód. " + c.getCodCorrelativa());
                    }
                }

                if(!faltantes.isEmpty()) {
                    String msg = "No cumplís las correlatividades. Te falta aprobar: " + String.join(", ", faltantes);

                    try {
                        res.redirect("/estudiante/inscripcion?errorMessage=" + java.net.URLEncoder.encode(msg, "UTF-8"));
                    } catch (Exception ex) {
                        res.redirect("/estudiante/inscripcion?errorMessage=Correlativas incompletas.");
                    }
                    return null;
                }
            }
    
            try {
                estudiante.inscribirseMateria(codMateria);
               AuditoriaService.registrarAuditoria(req, "INSCRIBIR_MATERIA", "dni:" + estudiante.getDni() + " codMateria:" + codMateria); 
                res.redirect("/estudiante/inscripcion?successMessage=Te inscribiste correctamente a la materia.");
            } catch (Exception e) {
                try {
                    res.redirect("/estudiante/inscripcion?errorMessage=" + java.net.URLEncoder.encode(e.getMessage(), "UTF-8"));
                } catch (Exception ex) {
                    res.redirect("/estudiante/inscripcion?errorMessage=Error al inscribirse.");
                }
            }

            return null;
        });
        
        get("/estudiante/carrera", (req, res) -> {

            Integer userId = req.session().attribute("userId");
            Estudiante estudiante = Estudiante.findFirst("user_id = ?", userId);
    
            if(estudiante == null) {
                res.redirect("/dashboard?error=No se encontró el perfil de estudiante.");
                return null;
            }
    
            Persona persona = Persona.findFirst("dni = ?", estudiante.getDni());
    
            Map<String, Object> model = new HashMap<>();
            model.put("nombre",    persona != null ? persona.getNombre()   : "");
            model.put("apellido",  persona != null ? persona.getApellido() : "");
            model.put("nroLegajo", estudiante.getNroLegajo());

            InscripcionCarrera inscripcion = InscripcionCarrera.findFirst("dni_estudiante = ?", estudiante.getDni());
    
            if(inscripcion != null) {
                Carrera carreraActual = Carrera.findFirst("cod_carrera = ?", inscripcion.getCodCarrera());
                model.put("yaInscripto",       true);
                model.put("carreraActual",     carreraActual != null ? carreraActual.getNombre()      : "Sin nombre");
                model.put("descripcionActual", carreraActual != null ? carreraActual.getDescripcion() : "");
                model.put("situacionCarrera",  inscripcion.getSituacion() != null ? inscripcion.getSituacion().name() : "");
                model.put("esIngresante",      Situacion.INGRESANTE.equals(inscripcion.getSituacion()));
                model.put("esAvanzado",        Situacion.AVANZADO.equals(inscripcion.getSituacion()));
            } else {
                List<Carrera> carrerasDB = Carrera.findAll();
                List<Map<String, Object>> carreras = new ArrayList<>();

                for(Carrera c : carrerasDB) {
                    Map<String, Object> cv = new HashMap<>();
                    cv.put("codCarrera",  c.getCodigo());
                    cv.put("nombre",      c.getNombre());
                    cv.put("descripcion", c.getDescripcion() != null ? c.getDescripcion() : "");
                    carreras.add(cv);
                }

                model.put("yaInscripto", false);
                model.put("carreras",    carreras);
                model.put("sinCarreras", carreras.isEmpty());
            }
    
            String success = req.queryParams("successMessage");
            String error   = req.queryParams("errorMessage");

            if(success != null) {
                model.put("successMessage", success);
            }
            if(error   != null) {
            model.put("errorMessage",   error);
            }
    
            return new ModelAndView(model, "estudiante/inscripcionCarrera.mustache");
    
        }, new MustacheTemplateEngine());

        post("/estudiante/carrera/inscribir", (req, res) -> {
    
            Integer userId = req.session().attribute("userId");
            Estudiante estudiante = Estudiante.findFirst("user_id = ?", userId);
    
            if(estudiante == null) {
                res.redirect("/dashboard?error=No se encontró el perfil de estudiante.");
                return null;
            }

            InscripcionCarrera inscripcionExistente = InscripcionCarrera.findFirst("dni_estudiante = ?", estudiante.getDni());
            if(inscripcionExistente != null) {
                res.redirect("/estudiante/carrera?errorMessage=Ya estás inscripto en una carrera.");
                return null;
            }
    
            String codCarreraStr = req.queryParams("cod_carrera");
            if (codCarreraStr == null || codCarreraStr.isEmpty()) {
                res.redirect("/estudiante/carrera?errorMessage=Debe seleccionar una carrera.");
                return null;
            }
    
            Integer codCarrera;
            try {
                codCarrera = Integer.parseInt(codCarreraStr);
            } catch (NumberFormatException e) {
                res.redirect("/estudiante/carrera?errorMessage=Carrera inválida.");
                return null;
            }
    
            Carrera carrera = Carrera.findFirst("cod_carrera = ?", codCarrera);
            if(carrera == null) {
                res.redirect("/estudiante/carrera?errorMessage=La carrera seleccionada no existe.");
                return null;
            }
    
            try {
                estudiante.inscribirseCarrera(codCarrera);
                AuditoriaService.registrarAuditoria(req, "INSCRIBIR_CARRERA", "dni:" + estudiante.getDni() + " codCarrera:" + codCarrera); 
                res.redirect("/estudiante/carrera?successMessage=Te inscribiste correctamente a " + carrera.getNombre() + ".");
            } catch (Exception e) {
                try {
                    res.redirect("/estudiante/carrera?errorMessage=" + java.net.URLEncoder.encode(e.getMessage(), "UTF-8"));
                } catch (Exception ex) {
                    res.redirect("/estudiante/carrera?errorMessage=Error al inscribirse a la carrera.");
                }
            }

            return null;
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
            String nroLegajoString = req.queryParams("nro_legajo");

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
     registrarRutasDocente();
   
     registrarRutasEstudiante();
     
     

    } // Fin del método main

    // HELPERS
    public static boolean esEmailValido(String email) {
    String regex = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    return email != null && email.matches(regex);
    }
    
    private static boolean isAdmin(Request req) {
        String rol = req.session().attribute("userRol");
        return "ADMINISTRADOR".equals(rol);
    }
    
    private static boolean isDocente(Request req) {
        String rol = req.session().attribute("userRol");
        return "DOCENTE".equals(rol);
    }


    
private static void registrarRutasDocente() {
 
    before("/docente/*", (req, res) -> {
        Boolean loggedIn = req.session().attribute("loggedIn");
 
        if (loggedIn == null || !loggedIn) {
            res.redirect("/");
            halt();
        }
 
        String rol = req.session().attribute("userRol");
 
        if (!"DOCENTE".equals(rol) && !"ADMINISTRADOR".equals(rol))  {
            res.redirect("/dashboard");
            halt();
        }
    });
 
    get("/docente/examenes/crear", (req, res) -> {
 
        Integer userId = req.session().attribute("userId");
        Docente docente = Docente.findFirst("user_id = ?", userId);
        System.out.println("codigoProfesor = " + docente.getCodigoProfesor());
 
        if (docente == null) {
            res.redirect("/dashboard?error=No se encontró el perfil de docente.");
            return null;
        }
 
        // Solo materias donde el docente es Responsable_de_Catedra
        List<PeriodoAcademico> periodos = PeriodoAcademico.where(
            "codigo_profesor = ? AND cargo = ?",
            docente.getCodigoProfesor(), "RESPONSABLE_DE_CATEDRA"
        );
        System.out.println("periodos encontrados = " + periodos.size());

 
        List<Map<String, Object>> materiasView = new ArrayList<>();
        for (PeriodoAcademico p : periodos) {
            Materia m = Materia.findFirst("cod_materia = ?", p.getCodMateria());
            if (m != null) {
                Map<String, Object> mv = new HashMap<>();
                mv.put("cod_materia", m.getCodMateria());
                mv.put("nombre", m.getNombre());
                materiasView.add(mv);
            }
        }
 
        Map<String, Object> model = new HashMap<>();
        model.put("materias", materiasView);
        if (materiasView.isEmpty()) {
            model.put("errorMessage", "No tenés materias asignadas como Responsable de Cátedra.");
        }
 
        String error = req.queryParams("errorMessage");
        if (error != null) model.put("errorMessage", error);
 
        return new ModelAndView(model, "docente/crearExamen.mustache");
 
    }, new MustacheTemplateEngine());

    post("/docente/examenes/crear", (req, res) -> {

    Integer userId = req.session().attribute("userId");
    Docente docente = Docente.findFirst("user_id = ?", userId);

    if (docente == null) {
        res.redirect("/dashboard");
        return null;
    }    

    String codMateriaStr = req.queryParams("cod_materia");
    String fecha         = req.queryParams("fecha");

    if (codMateriaStr == null || codMateriaStr.isEmpty() || fecha == null || fecha.isEmpty()) {
        res.redirect("/docente/examenes/crear?errorMessage=Todos los campos son obligatorios.");
        return null;
    }

    Integer codMateria = Integer.parseInt(codMateriaStr);

    // Verificar que la materia le pertenece al docente como Responsable
    PeriodoAcademico periodo = PeriodoAcademico.findFirst(
        "codigo_profesor = ? AND cod_materia = ? AND cargo = ?",
        docente.getCodigoProfesor(), codMateria, "RESPONSABLE_DE_CATEDRA"
    );
    if (periodo == null) {
        res.redirect("/docente/examenes/crear?errorMessage=No tenés permiso para esa materia.");
        return null;
    }

    try {
        ExamenFinal examen = new ExamenFinal();
        examen.setCodigoProfesor(docente.getCodigoProfesor());
        examen.setCodMateria(codMateria);
        examen.setFecha(fecha);
        examen.saveIt();
            AuditoriaService.registrarAuditoria(req, "CREAR_EXAMEN", "codigoProfesor:" + docente.getCodigoProfesor() + " codMateria:" + codMateria + " fecha:" + fecha);

        res.redirect("/dashboard?message=Examen creado correctamente.");
    } catch (Exception e) {
        res.redirect("/docente/examenes/crear?errorMessage=Error al crear el examen: " + e.getMessage());
    }
    return null;
});

    get("/docente/alumnos", (req, res) -> {
        Integer userId = req.session().attribute("userId");
        Docente docente = Docente.findFirst("user_id = ?", userId);

        if (docente == null) {
            res.redirect("/dashboard?error=No se encontró el perfil de docente.");
            return null;
        }

        // Materias asignadas al docente
        List<PeriodoAcademico> periodos = PeriodoAcademico.where(
            "codigo_profesor = ?", docente.getCodigoProfesor()
        );

        // Armar lista de materias para el filtro
        List<Map<String, Object>> materiasView = new ArrayList<>();
        for (PeriodoAcademico p : periodos) {
            Materia m = Materia.findFirst("cod_materia = ?", p.getCodMateria());
            if (m != null) {
                Map<String, Object> mv = new HashMap<>();
                mv.put("codMateria", m.getCodMateria());
                mv.put("nombre", m.getNombre());
                mv.put("fecha", p.getFecha());
                materiasView.add(mv);
            }
        }

        // Filtros opcionales
        String codMateriaStr = req.queryParams("cod_materia");
        String fechaFiltro   = req.queryParams("fecha");

        List<Map<String, Object>> alumnosView = new ArrayList<>();

        if (codMateriaStr != null && !codMateriaStr.isEmpty()) {
            int codMateria = Integer.parseInt(codMateriaStr);

            // Verificar que la materia pertenece al docente
            PeriodoAcademico perm = PeriodoAcademico.findFirst(
                "codigo_profesor = ? AND cod_materia = ?",
                docente.getCodigoProfesor(), codMateria
            );

            if (perm == null) {
                res.redirect("/docente/alumnos?errorMessage=No tenés permiso para esa materia.");
                return null;
            }

            // Buscar alumnos inscriptos a esa materia
            List<Estado> estados;
            if (fechaFiltro != null && !fechaFiltro.isEmpty()) {
                // Filtrar también por año de la fecha del periodo
                estados = Estado.where("cod_materia = ?", codMateria);
                List<Estado> filtrados = new ArrayList<>();
                for (Estado e : estados) {
                    PeriodoAcademico pa = PeriodoAcademico.findFirst(
                        "cod_materia = ? AND codigo_profesor = ? AND fecha LIKE ?",
                        codMateria, docente.getCodigoProfesor(), fechaFiltro + "%"
                    );
                    if (pa != null) filtrados.add(e);
                }
                estados = filtrados;
            } else {
                estados = Estado.where("cod_materia = ?", codMateria);
            }

            for (Estado e : estados) {
                Estudiante estudiante = Estudiante.findFirst("dni = ?", e.getDniEstudiante());
                Persona persona = Persona.findFirst("dni = ?", e.getDniEstudiante());

                Map<String, Object> av = new HashMap<>();
                av.put("dni",       e.getDniEstudiante());
                av.put("nroLegajo", estudiante != null ? estudiante.getNroLegajo() : "-");
                av.put("nombre",    persona != null ? persona.getNombre() : "");
                av.put("apellido",  persona != null ? persona.getApellido() : "");
                av.put("email",     estudiante != null ? estudiante.getEmail() : "");
                av.put("estado",    e.getString("estado"));
                av.put("esInscripto", "INSCRIPTO".equals(e.getString("estado")));
                av.put("codMateria", codMateria); 

                // Badge de color según estado
                String estadoClass;
                if ("APROBADO".equals(e.getString("estado"))) {
                    estadoClass = "bg-green-100 text-green-700";
                } else if ("REGULAR".equals(e.getString("estado"))) {
                    estadoClass = "bg-blue-100 text-blue-700";
                } else if ("LIBRE".equals(e.getString("estado"))) {
                    estadoClass = "bg-red-100 text-red-700";
                } else {
                    estadoClass = "bg-yellow-100 text-yellow-700"; // INSCRIPTO
                }
                av.put("estadoClass", estadoClass);

                alumnosView.add(av);
            }
            
    }

        // Fechas únicas para el filtro de período
        List<String> fechasUnicas = new ArrayList<>();
        for (PeriodoAcademico p : periodos) {
            String fecha = p.getFecha();
            if(fecha != null && fecha.length () >= 4){
                String anio = p.getFecha().substring(0, 4);
                if (!fechasUnicas.contains(anio)) {
                    fechasUnicas.add(anio);
                }
            }
        }
        List<Map<String, Object>> fechasView = new ArrayList<>();
        for (String f : fechasUnicas) {
            Map<String, Object> fv = new HashMap<>();
            fv.put("fecha", f);
            fechasView.add(fv);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("materias",       materiasView);
        model.put("sinMaterias",    materiasView.isEmpty());
        model.put("alumnos",        alumnosView);
        model.put("sinAlumnos",     alumnosView.isEmpty());
        model.put("mostrarTabla",   codMateriaStr != null && !codMateriaStr.isEmpty());
        model.put("fechas",         fechasView);
        model.put("successMessage", req.queryParams("successMessage"));
        model.put("errorMessage",   req.queryParams("errorMessage"));

        return new ModelAndView(model, "docente/alumnosInscriptos.mustache");

    }, new MustacheTemplateEngine());

    //! Cargar Notas finales a examentes
    get("/docente/notas", (req, res) -> {
 
        Integer userId = req.session().attribute("userId");
        Docente docente = Docente.findFirst("user_id = ?", userId);
 
        if (docente == null) {
            res.redirect("/dashboard?error=No se encontró el perfil de docente.");
            return null;
        }
 
        // Examenes del Docente
        List<ExamenFinal> examenesDB = ExamenFinal.where(
            "codigo_profesor = ?", docente.getCodigoProfesor()
        );
 
        // Armar Lista de examenes para el filtro
        List<Map<String, Object>> examenesView = new ArrayList<>();
        for (ExamenFinal ex : examenesDB) {
            Materia mat = Materia.findFirst("cod_materia = ?", ex.getCodMateria());
            Map<String, Object> ev = new HashMap<>();
            ev.put("idExamen",      ex.getId());
            ev.put("nombreMateria", mat != null ? mat.getNombre() : "Sin nombre");
            ev.put("fecha",         ex.getFecha());
            examenesView.add(ev);
        }
 
        Map<String, Object> model = new HashMap<>();
        model.put("examenes",    examenesView);
        model.put("sinExamenes", examenesView.isEmpty());
 
        // Si viene ?id_examen=X cargamos la tabla de alumnos
        String idExamenStr = req.queryParams("id_examen");
        if (idExamenStr != null && !idExamenStr.isEmpty()) {
 
            int idExamen = Integer.parseInt(idExamenStr);
 
            // Marcar el seleccionado en el <select>
            for (Map<String, Object> ev : examenesView) {
                ev.put("seleccionado", ev.get("idExamen").equals(idExamen));
            }
 
            ExamenFinal examen = ExamenFinal.findById(idExamen);
 
            // Verificar que el examen le pertenece al docente
            if (examen == null || !examen.getCodigoProfesor().equals(docente.getCodigoProfesor())) {
                res.redirect("/docente/notas?errorMessage=Examen no encontrado o sin permiso.");
                return null;
            }
 
            Materia materia = Materia.findFirst("cod_materia = ?", examen.getCodMateria());
 
            // Buscar alumnos inscriptos a ese examen
            List<InscripcionExamen> inscripciones = InscripcionExamen.where("id_examen = ?", idExamen);
 
            List<Map<String, Object>> alumnosView = new ArrayList<>();
            for (InscripcionExamen insc : inscripciones) {
 
                Estudiante estudiante = Estudiante.findFirst("dni = ?", insc.getDniEstudiante());
                Persona    persona    = Persona.findFirst("dni = ?", insc.getDniEstudiante());
 
                Estado estadoActual = Estado.findFirst(
                    "dni_estudiante = ? AND cod_materia = ?",
                    insc.getDniEstudiante(), examen.getCodMateria()
                );
 
                String estadoStr     = estadoActual != null ? estadoActual.getString("estado") : "REGULAR";
                boolean yaCalificado = "APROBADO".equals(estadoStr) || "LIBRE".equals(estadoStr);
 
                String estadoClass;
                if ("APROBADO".equals(estadoStr)) {
                    estadoClass = "bg-green-100 text-green-700";
                } else if ("LIBRE".equals(estadoStr)) {
                    estadoClass = "bg-red-100 text-red-700";
                } else if ("REGULAR".equals(estadoStr)) {
                    estadoClass = "bg-blue-100 text-blue-700";
                } else {
                    estadoClass = "bg-yellow-100 text-yellow-700";
                }
 
                String nombre   = persona != null ? persona.getNombre()   : "";
                String apellido = persona != null ? persona.getApellido() : "";
                String iniciales = (
                    (!nombre.isEmpty()   ? String.valueOf(nombre.charAt(0))   : "") +
                    (!apellido.isEmpty() ? String.valueOf(apellido.charAt(0)) : "")
                ).toUpperCase();
 
                Map<String, Object> av = new HashMap<>();
                av.put("dni",          insc.getDniEstudiante());
                av.put("nombre",       nombre);
                av.put("apellido",     apellido);
                av.put("iniciales",    iniciales);
                av.put("email",        estudiante != null ? estudiante.getEmail()    : "");
                av.put("nroLegajo",    estudiante != null ? estudiante.getNroLegajo() : "-");
                av.put("codMateria",   examen.getCodMateria());
                av.put("idExamen",     idExamen);
                av.put("estadoActual", estadoStr);
                av.put("estadoClass",  estadoClass);
                av.put("yaCalificado", yaCalificado);
 
                alumnosView.add(av);
            }
 
            model.put("examenSeleccionado",        true);
            model.put("nombreMateriaSeleccionada", materia != null ? materia.getNombre() : "Sin nombre");
            model.put("fechaExamen",               examen.getFecha());
            model.put("totalAlumnos",              alumnosView.size());
            model.put("alumnos",                   alumnosView);
            model.put("sinAlumnos",                alumnosView.isEmpty());
            model.put("idExamenActual",            idExamen);
        }
 
        model.put("successMessage", req.queryParams("successMessage"));
        model.put("errorMessage",   req.queryParams("errorMessage"));
 
        return new ModelAndView(model, "docente/notasFinales.mustache");
 
    }, new MustacheTemplateEngine());
 
 
    // POST: guarda el resultado de un alumno en un examen final
    post("/docente/notas/cargar", (req, res) -> {
 
        Integer userId = req.session().attribute("userId");
        Docente docente = Docente.findFirst("user_id = ?", userId);
 
        if (docente == null) {
            res.redirect("/dashboard?error=No se encontró el perfil de docente.");
            return null;
        }
 
        String idExamenStr      = req.queryParams("id_examen");
        String dniEstudianteStr = req.queryParams("dni_estudiante");
        String codMateriaStr    = req.queryParams("cod_materia");
        String resultado        = req.queryParams("resultado");
        String notaNumero       = req.queryParams("nota_final");
 
        // Validaciones básicas
        if (idExamenStr == null      || idExamenStr.isEmpty()      ||
            dniEstudianteStr == null || dniEstudianteStr.isEmpty() ||
            codMateriaStr == null    || codMateriaStr.isEmpty()    ||
            resultado == null        || resultado.isEmpty()) {
 
            res.redirect("/docente/notas?errorMessage=Datos incompletos. Seleccioná un resultado.");
            return null;
        }
 
        if (!"APROBADO".equals(resultado) && !"LIBRE".equals(resultado)) {
            res.redirect("/docente/notas?errorMessage=Resultado inválido.");
            return null;
        }
 
        int idExamen      = Integer.parseInt(idExamenStr);
        int dniEstudiante = Integer.parseInt(dniEstudianteStr);
        int codMateria    = Integer.parseInt(codMateriaStr);
 
        // Verificar que el examen le pertenece al docente
        ExamenFinal examen = ExamenFinal.findById(idExamen);
        if (examen == null || !examen.getCodigoProfesor().equals(docente.getCodigoProfesor())) {
            res.redirect("/docente/notas?errorMessage=No tenés permiso para ese examen.");
            return null;
        }
 
        // Verificar que el alumno está inscripto a ese examen
        InscripcionExamen inscripcion = InscripcionExamen.findFirst(
            "id_examen = ? AND dni_estudiante = ?", idExamen, dniEstudiante
        );
        if (inscripcion == null) {
            res.redirect("/docente/notas?id_examen=" + idExamen +
                "&errorMessage=El alumno no está inscripto a ese examen.");
            return null;
        }
 
        // Verificar que todavía no fue calificado
        Estado estadoActual = Estado.findFirst(
            "dni_estudiante = ? AND cod_materia = ?", dniEstudiante, codMateria
        );
        if (estadoActual == null) {
            res.redirect("/docente/notas?id_examen=" + idExamen +
                "&errorMessage=No se encontró el estado del alumno en esa materia.");
            return null;
        }
 
        String estadoStr = estadoActual.getString("estado");
        if ("APROBADO".equals(estadoStr) || "LIBRE".equals(estadoStr)) {
            res.redirect("/docente/notas?id_examen=" + idExamen +
                "&errorMessage=El alumno ya tiene un resultado cargado.");
            return null;
        }
 
        // Actualizar el estado
        try {
            Base.openTransaction();
 
            Base.exec(
                "UPDATE Estado SET estado = ? WHERE dni_estudiante = ? AND cod_materia = ?",
                resultado, dniEstudiante, codMateria
            );
 
            Base.commitTransaction();
 
            String msg = "APROBADO".equals(resultado)
                ? "Alumno aprobado correctamente."
                : "Resultado libre registrado.";
 
             AuditoriaService.registrarAuditoria(req, "CARGAR_NOTA", "idExamen:" + idExamen + " dni:" + dniEstudiante + " resultado:" + resultado); 
            res.redirect("/docente/notas?id_examen=" + idExamen +
                "&successMessage=" + java.net.URLEncoder.encode(msg, "UTF-8"));
 
        } catch (Exception e) {
            Base.rollbackTransaction();
            e.printStackTrace();
            res.redirect("/docente/notas?id_examen=" + idExamen +
                "&errorMessage=Error al guardar el resultado: " + e.getMessage());
        }
 
        return null;
    });




    //!
    //!
    //!
    post("/docente/alumnos/estadoCursada", (req, res) -> {
 
        Integer userId = req.session().attribute("userId");
        Docente docente = Docente.findFirst("user_id = ?", userId);
 
        if (docente == null) {
            res.redirect("/dashboard?error=No se encontró el perfil de docente.");
            return null;
        }
 
        String dniEstudianteStr = req.queryParams("dni_estudiante");
        String codMateriaStr    = req.queryParams("cod_materia");
        String nuevoEstado      = req.queryParams("nuevo_estado");
 
        // Validaciones básicas
        if (dniEstudianteStr == null || dniEstudianteStr.isEmpty() ||
            codMateriaStr == null    || codMateriaStr.isEmpty()    ||
            nuevoEstado == null      || nuevoEstado.isEmpty()) {
 
            res.redirect("/docente/alumnos?errorMessage=Datos incompletos.");
            return null;
        }
 
        if (!"REGULAR".equals(nuevoEstado) && !"LIBRE".equals(nuevoEstado)) {
            res.redirect("/docente/alumnos?errorMessage=Estado inválido.");
            return null;
        }
 
        int dniEstudiante = Integer.parseInt(dniEstudianteStr);
        int codMateria    = Integer.parseInt(codMateriaStr);
 
        // Verificar que la materia le pertenece al docente
        PeriodoAcademico perm = PeriodoAcademico.findFirst(
            "codigo_profesor = ? AND cod_materia = ?",
            docente.getCodigoProfesor(), codMateria
        );
        if (perm == null) {
            res.redirect("/docente/alumnos?errorMessage=No tenés permiso para esa materia.");
            return null;
        }
 
        // Verificar que el alumno tiene esa materia en INSCRIPTO
        Estado estadoActual = Estado.findFirst(
            "dni_estudiante = ? AND cod_materia = ?", dniEstudiante, codMateria
        );
        if (estadoActual == null) {
            res.redirect("/docente/alumnos?errorMessage=No se encontró el estado del alumno en esa materia.");
            return null;
        }
 
        if (!"INSCRIPTO".equals(estadoActual.getString("estado"))) {
            res.redirect("/docente/alumnos?errorMessage=Solo se puede cambiar el estado de alumnos INSCRIPTOS.");
            return null;
        }
 
        try {
            Base.openTransaction();
 
            Base.exec(
                "UPDATE Estado SET estado = ? WHERE dni_estudiante = ? AND cod_materia = ?",
                nuevoEstado, dniEstudiante, codMateria
            );
 
            Base.commitTransaction();
 
            String msg = "REGULAR".equals(nuevoEstado)
                ? "Alumno marcado como Regular correctamente."
                : "Alumno marcado como Libre correctamente.";
 
            AuditoriaService.registrarAuditoria(req, "CAMBIAR_ESTADO_CURSADA", "dni:" + dniEstudiante + " codMateria:" + codMateria + " nuevoEstado:" + nuevoEstado); 
            res.redirect("/docente/alumnos?cod_materia=" + codMateria +
                "&successMessage=" + java.net.URLEncoder.encode(msg, "UTF-8"));
 
        } catch (Exception e) {
            Base.rollbackTransaction();
            e.printStackTrace();
            res.redirect("/docente/alumnos?errorMessage=Error al actualizar el estado: " + e.getMessage());
        }
 
        return null;
    });
    //!
    //!
    //!
    get("/estudiante/avance", (req, res) -> {
            Integer userId = req.session().attribute("userId");
            Estudiante estudiante = Estudiante.findFirst("user_id = ?", userId);

            if (estudiante == null) {
                res.redirect("/dashboard?error=No se encontró el perfil de estudiante.");
                return null;
            }

            Persona persona = Persona.findFirst("dni = ?", estudiante.getDni());

            // Todos los estados del estudiante
            List<Estado> estados = Estado.where("dni_estudiante = ?", estudiante.getDni());

            List<Map<String, Object>> aprobadas  = new ArrayList<>();
            List<Map<String, Object>> pendientes = new ArrayList<>();

            for (Estado e : estados) {
                Materia materia = Materia.findFirst("cod_materia = ?", e.getCodMateria());
                if (materia == null) continue;

                Map<String, Object> mv = new HashMap<>();
                mv.put("nombre",     materia.getNombre());
                mv.put("codMateria", materia.getCodMateria());
                mv.put("estado",     e.getString("estado"));

                String estadoClass;
                if ("APROBADO".equals(e.getString("estado"))) {
                    estadoClass = "bg-green-100 text-green-700";
                } else if ("REGULAR".equals(e.getString("estado"))) {
                    estadoClass = "bg-blue-100 text-blue-700";
                } else if ("LIBRE".equals(e.getString("estado"))) {
                    estadoClass = "bg-red-100 text-red-700";
                } else {
                    estadoClass = "bg-yellow-100 text-yellow-700"; // INSCRIPTO
                }
                mv.put("estadoClass", estadoClass);

                if ("APROBADO".equals(e.getString("estado"))) {
                    aprobadas.add(mv);
                } else {
                    pendientes.add(mv);
                }
            }

            // Buscar el plan del estudiante para calcular porcentaje
            // El plan se obtiene a través de la materia → cod_plan → PlanDeEstudios
            int totalMaterias = 0;
            if (!estados.isEmpty()) {
                Estado primerEstado = estados.get(0);
                Materia primerMateria = Materia.findFirst("cod_materia = ?", primerEstado.getCodMateria());
                if (primerMateria != null) {
                    PlanDeEstudios plan = PlanDeEstudios.findFirst("cod_plan = ?", primerMateria.getCodPlan());
                    if (plan != null) {
                        totalMaterias = plan.getCantidadMaterias();
                    }
                }
            }

            int cantAprobadas = aprobadas.size();
            double porcentaje = totalMaterias > 0
                ? Math.round((cantAprobadas * 100.0 / totalMaterias) * 10.0) / 10.0
                : 0.0;

            Map<String, Object> model = new HashMap<>();
            model.put("nombre",        persona != null ? persona.getNombre() : "");
            model.put("apellido",      persona != null ? persona.getApellido() : "");
            model.put("nroLegajo",     estudiante.getNroLegajo());
            model.put("aprobadas",     aprobadas);
            model.put("pendientes",    pendientes);
            model.put("cantAprobadas", cantAprobadas);
            model.put("totalMaterias", totalMaterias);
            model.put("porcentaje",    porcentaje);
            model.put("sinEstados",    estados.isEmpty());

            return new ModelAndView(model, "estudiante/avanceAcademico.mustache");

        }, new MustacheTemplateEngine());

        // Configurar carpeta de uploads
        String uploadDir = "materialEstudio";
        new java.io.File(uploadDir).mkdirs();

        // GET: ver materiales subidos y formulario
        get("/docente/material", (req, res) -> {
            Integer userId = req.session().attribute("userId");
            Docente docente = Docente.findFirst("user_id = ?", userId);

            if (docente == null) {
                res.redirect("/dashboard?error=No se encontró el perfil de docente.");
                return null;
            }

            // Materias del docente
            List<PeriodoAcademico> periodos = PeriodoAcademico.where(
                "codigo_profesor = ?", docente.getCodigoProfesor()
            );
            List<Map<String, Object>> materiasView = new ArrayList<>();
            for (PeriodoAcademico p : periodos) {
                Materia m = Materia.findFirst("cod_materia = ?", p.getCodMateria());
                if (m != null) {
                    Map<String, Object> mv = new HashMap<>();
                    mv.put("codMateria", m.getCodMateria());
                    mv.put("nombre",     m.getNombre());
                    materiasView.add(mv);
                }
            }

            // Materiales ya subidos
            List<MaterialEstudio> materialesDB = MaterialEstudio.where(
                "codigo_profesor = ?", docente.getCodigoProfesor()
            );
            List<Map<String, Object>> materiales = new ArrayList<>();
            for (MaterialEstudio mat : materialesDB) {
                Materia m = Materia.findFirst("cod_materia = ?", mat.getCodMateria());
                Map<String, Object> mv = new HashMap<>();
                mv.put("id",            mat.getId());
                mv.put("nombre",        mat.getNombre());
                mv.put("descripcion",   mat.getDescripcion());
                mv.put("nombreArchivo", mat.getNombreArchivo());
                mv.put("fechaSubida",   mat.getFechaSubida());
                mv.put("nombreMateria", m != null ? m.getNombre() : "Sin materia");
                mv.put("idDescarga",    mat.getId());
                materiales.add(mv);
            }

            Map<String, Object> model = new HashMap<>();
            model.put("materias",       materiasView);
            model.put("sinMaterias",    materiasView.isEmpty());
            model.put("materiales",     materiales);
            model.put("sinMateriales",  materiales.isEmpty());
            model.put("successMessage", req.queryParams("successMessage"));
            model.put("errorMessage",   req.queryParams("errorMessage"));

            return new ModelAndView(model, "docente/materialEstudio.mustache");

        }, new MustacheTemplateEngine());

        // POST: subir archivo
        post("/docente/material/subir", (req, res) -> {
            Integer userId = req.session().attribute("userId");
            Docente docente = Docente.findFirst("user_id = ?", userId);

            if (docente == null) {
                res.redirect("/dashboard");
                return null;
            }

            // Habilitar multipart
            req.attribute("org.eclipse.jetty.multipartConfig",
                new javax.servlet.MultipartConfigElement("materialEstudio"));

            try {
                // Leer campos del form
                String nombre      = req.raw().getPart("nombre") != null
                    ? new String(req.raw().getPart("nombre").getInputStream().readAllBytes())
                    : "";
                String descripcion = req.raw().getPart("descripcion") != null
                    ? new String(req.raw().getPart("descripcion").getInputStream().readAllBytes())
                    : "";
                String codMateriaStr = req.raw().getPart("cod_materia") != null
                    ? new String(req.raw().getPart("cod_materia").getInputStream().readAllBytes())
                    : "";

                javax.servlet.http.Part filePart = req.raw().getPart("archivo");

                if (nombre.isEmpty() || codMateriaStr.isEmpty() || filePart == null || filePart.getSize() == 0) {
                    res.redirect("/docente/material?errorMessage=Todos los campos son obligatorios.");
                    return null;
                }

                // Validar formato
                String nombreArchivo = filePart.getSubmittedFileName();
                String extension = nombreArchivo.substring(nombreArchivo.lastIndexOf(".") + 1).toLowerCase();
                List<String> formatosPermitidos = List.of("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "jpg", "png");

                if (!formatosPermitidos.contains(extension)) {
                    res.redirect("/docente/material?errorMessage=Formato no permitido. Usá: PDF, DOC, DOCX, PPT, PPTX, XLS, XLSX, JPG, PNG.");
                    return null;
                }

                // Verificar que la materia pertenece al docente
                int codMateria = Integer.parseInt(codMateriaStr);
                PeriodoAcademico perm = PeriodoAcademico.findFirst(
                    "codigo_profesor = ? AND cod_materia = ?",
                    docente.getCodigoProfesor(), codMateria
                );
                if (perm == null) {
                    res.redirect("/docente/material?errorMessage=No tenés permiso para esa materia.");
                    return null;
                }

                // Guardar archivo con nombre único
                String nombreUnico = System.currentTimeMillis() + "_" + nombreArchivo;
                String rutaArchivo = uploadDir + "/" + nombreUnico;

                try (java.io.InputStream input = filePart.getInputStream();
                    java.io.FileOutputStream output = new java.io.FileOutputStream(rutaArchivo)) {
                    input.transferTo(output);
                }

                // Guardar en DB
                String fecha = java.time.LocalDate.now().toString();
                MaterialEstudio material = new MaterialEstudio();
                material.setCodMateria(codMateria);
                material.setCodigoProfesor(docente.getCodigoProfesor());
                material.setNombre(nombre);
                material.setDescripcion(descripcion);
                material.setNombreArchivo(nombreArchivo);
                material.setRutaArchivo(rutaArchivo);
                material.setFechaSubida(fecha);
                material.saveIt();

                AuditoriaService.registrarAuditoria(req, "SUBIR_MATERIAL", "codigoProfesor:" + docente.getCodigoProfesor() + " idMaterial:" + material.getId() + " nombre:" + nombre); 
                res.redirect("/docente/material?successMessage=Material subido correctamente.");

            } catch (Exception e) {
                e.printStackTrace();
                res.redirect("/docente/material?errorMessage=Error al subir el archivo: " + e.getMessage());
            }

            return null;
        });

        // GET: descargar archivo
        get("/material/descargar/:id", (req, res) -> {
            Integer id = Integer.parseInt(req.params(":id"));
            MaterialEstudio material = MaterialEstudio.findById(id);

            if (material == null) {
                res.redirect("/dashboard?error=Material no encontrado.");
                return null;
            }

            java.io.File archivo = new java.io.File(material.getRutaArchivo());
            if (!archivo.exists()) {
                res.redirect("/dashboard?error=El archivo no existe en el servidor.");
                return null;
            }

            res.raw().setContentType("application/octet-stream");
            res.raw().setHeader("Content-Disposition", "attachment; filename=\"" + material.getNombreArchivo() + "\"");

            try (java.io.InputStream is = new java.io.FileInputStream(archivo);
                java.io.OutputStream os = res.raw().getOutputStream()) {
                is.transferTo(os);
                os.flush();
            }
            AuditoriaService.registrarAuditoria(req, "DESCARGAR_MATERIAL", "idMaterial:" + material.getId() + " nombre:" + material.getNombreArchivo());

            return null;
        });

    }





    private static void registrarRutasEstudiante() {

    before("/estudiante/*", (req, res) -> {
        Boolean loggedIn = req.session().attribute("loggedIn");
        if (loggedIn == null || !loggedIn) {
            res.redirect("/");
            halt();
        }
        String rol = req.session().attribute("userRol");
       if (!"ALUMNO".equals(rol) && !"ADMINISTRADOR".equals(rol)) {
            res.redirect("/dashboard");
            halt();
        }
    });

    // GET: ver exámenes disponibles para inscribirse
    get("/estudiante/examenes", (req, res) -> {

        Integer userId = req.session().attribute("userId");
        Estudiante estudiante = Estudiante.findFirst("user_id = ?", userId);

        if (estudiante == null) {
            res.redirect("/dashboard?error=No se encontró el perfil de estudiante.");
            return null;
        }

        // Materias en estado REGULAR del estudiante
        List<Estado> regulares = Estado.where(
            "dni_estudiante = ? AND estado = ?",
            estudiante.getDni(), "REGULAR"
        );

        List<Map<String, Object>> examenesView = new ArrayList<>();

        for (Estado e : regulares) {
            Integer codMateria = e.getCodMateria();

            // Verificar si ya está inscripto a un examen de esta materia
            boolean yaInscripto = InscripcionExamen.yaInscripto(estudiante.getDni(), codMateria);

            // Exámenes disponibles para esa materia
            List<ExamenFinal> examenes = ExamenFinal.where("cod_materia = ?", codMateria);

            Materia materia = Materia.findFirst("cod_materia = ?", codMateria);

            for (ExamenFinal ex : examenes) {
                Map<String, Object> ev = new HashMap<>();
                ev.put("idExamen", ex.getId());
                ev.put("nombreMateria", materia != null ? materia.getNombre() : "Sin nombre");
                ev.put("fecha", ex.getFecha());
                ev.put("yaInscripto", yaInscripto);
                examenesView.add(ev);
            }
        }

        Map<String, Object> model = new HashMap<>();
        model.put("examenes", examenesView);
        model.put("sinExamenes", examenesView.isEmpty());

        String success = req.queryParams("successMessage");
        String error   = req.queryParams("errorMessage");
        if (success != null) model.put("successMessage", success);
        if (error   != null) model.put("errorMessage", error);

        return new ModelAndView(model, "estudiante/examenesDisponibles.mustache");

    }, new MustacheTemplateEngine());

    // POST: inscribirse a un examen
    post("/estudiante/examenes/:id/inscribir", (req, res) -> {

        Integer userId = req.session().attribute("userId");
        Estudiante estudiante = Estudiante.findFirst("user_id = ?", userId);

        if (estudiante == null) {
            res.redirect("/dashboard");
            return null;
        }

        Integer idExamen = Integer.parseInt(req.params(":id"));
        ExamenFinal examen = ExamenFinal.findById(idExamen);

        if (examen == null) {
            res.redirect("/estudiante/examenes?errorMessage=Examen no encontrado.");
            return null;
        }

        // Verificar que tiene esa materia en REGULAR
        Estado estado = Estado.findFirst(
            "dni_estudiante = ? AND cod_materia = ? AND estado = ?",
            estudiante.getDni(), examen.getCodMateria(), "REGULAR"
        );
        if (estado == null) {
            res.redirect("/estudiante/examenes?errorMessage=No tenes esa materia en estado Regular.");
            return null;
        }

        // Verificar que no está ya inscripto a otro examen de esa materia
        if (InscripcionExamen.yaInscripto(estudiante.getDni(), examen.getCodMateria())) {
            res.redirect("/estudiante/examenes?errorMessage=Ya estas inscripto a un examen de esa materia.");
            return null;
        }

        try {
            InscripcionExamen inscripcion = new InscripcionExamen();
            inscripcion.setDniEstudiante(estudiante.getDni());
            inscripcion.setIdExamen(idExamen);
            inscripcion.saveIt();
            AuditoriaService.registrarAuditoria(req, "INSCRIBIR_EXAMEN", "dni:" + estudiante.getDni() + " idExamen:" + idExamen); 

            res.redirect("/estudiante/examenes?successMessage=Inscripcion realizada correctamente.");
        } catch (Exception e) {
            res.redirect("/estudiante/examenes?errorMessage=Error al inscribirse: " + e.getMessage());
        }
        return null;
    });

    // GET: ver materiales disponibles para el estudiante
    get("/estudiante/material", (req, res) -> {
        Integer userId = req.session().attribute("userId");
        Estudiante estudiante = Estudiante.findFirst("user_id = ?", userId);

        if (estudiante == null) {
            res.redirect("/dashboard?error=No se encontró el perfil de estudiante.");
            return null;
        }

        // Materias en las que está inscripto
        List<Estado> estados = Estado.where("dni_estudiante = ?", estudiante.getDni());

        List<Map<String, Object>> materiales = new ArrayList<>();
        for (Estado e : estados) {
            List<MaterialEstudio> matsDB = MaterialEstudio.where("cod_materia = ?", e.getCodMateria());
            Materia materia = Materia.findFirst("cod_materia = ?", e.getCodMateria());

            for (MaterialEstudio mat : matsDB) {
                Map<String, Object> mv = new HashMap<>();
                mv.put("id",            mat.getId());
                mv.put("nombre",        mat.getNombre());
                mv.put("descripcion",   mat.getDescripcion());
                mv.put("nombreArchivo", mat.getNombreArchivo());
                mv.put("fechaSubida",   mat.getFechaSubida());
                mv.put("nombreMateria", materia != null ? materia.getNombre() : "Sin materia");
                materiales.add(mv);
            }
        }

        Map<String, Object> model = new HashMap<>();
        model.put("materiales",    materiales);
        model.put("sinMateriales", materiales.isEmpty());

        return new ModelAndView(model, "estudiante/materialEstudio.mustache");

    }, new MustacheTemplateEngine());
}
} // Fin de la clase App