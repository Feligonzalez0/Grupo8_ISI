package com.is1.proyecto.services.adminService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.javalite.activejdbc.Base;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.is1.proyecto.models.Docente;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.PeriodoAcademico;
import com.is1.proyecto.models.Persona;
import com.is1.proyecto.models.User;

/**
 * Servicio de gestión de docentes.
 *
 * Responsabilidades:
 *   - Crear un docente junto con su Persona y actualizar el rol del User asociado.
 *   - Editar datos de un docente (Persona + email) en una transacción.
 *   - Eliminar un docente, su Persona y resetear el rol del User.
 *   - Construir las listas de vista (List<Map>) que el controller pasa a Mustache.
 *
 * No conoce nada de Spark (Request, Response, sesiones).
 */
public class AdminDocenteService {

    private static final Logger logger = LoggerFactory.getLogger(AdminDocenteService.class);

    // LISTAR
    public static List<Map<String, Object>> listarDocentes() {
        List<Docente> docentesDB = Docente.findAll();
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Docente docente : docentesDB) {
            Map<String, Object> vista = new HashMap<>();

            vista.put("id",    docente.getInteger("codigo_profesor"));
            vista.put("email", docente.getString("email"));

            Integer dni = docente.getInteger("dni");
            Persona persona = Persona.findFirst("dni = ?", dni);
            if (persona != null) {
                vista.put("dni",              persona.getInteger("dni"));
                vista.put("nombre",           persona.getString("nombre"));
                vista.put("apellido",         persona.getString("apellido"));
                vista.put("telefono",         persona.getString("telefono"));
                vista.put("direccion",        persona.getString("direccion"));
                vista.put("fecha_nacimiento", persona.getString("fecha_nacimiento"));
            }

            Integer userId = docente.getInteger("user_id");
            User user = User.findById(userId);
            if (user != null) {
                vista.put("username", user.getString("name"));
            }

            resultado.add(vista);
        }

        return resultado;
    }

    // OBTENER UNO (para formularios de editar/eliminar)
    public Map<String, Object> obtenerDocenteParaVista(Integer codigoProfesor) {
        Docente docente = Docente.findFirst("codigo_profesor = ?", codigoProfesor);
        if (docente == null) return null;

        Integer dni     = docente.getInteger("dni");
        Integer userId  = docente.getInteger("user_id");
        Persona persona = Persona.findFirst("dni = ?", dni);
        User    user    = User.findById(userId);

        Map<String, Object> vista = new HashMap<>();
        vista.put("codigoProfesor", docente.getInteger("codigo_profesor"));
        vista.put("email",          docente.getString("email"));

        if (persona != null) {
            vista.put("dni",             persona.getInteger("dni"));
            vista.put("nombre",          persona.getString("nombre"));
            vista.put("apellido",        persona.getString("apellido"));
            vista.put("fechaNacimiento", persona.getString("fecha_nacimiento"));
            vista.put("telefono",        persona.getString("telefono"));
            vista.put("direccion",       persona.getString("direccion"));
        }
        if (user != null) {
            vista.put("username", user.getString("name"));
        }

        return vista;
    }

    // CREAR
    public void crearDocente(String nombre, String apellido, String dniString,
                              String email, String fechaNacimiento,
                              String telefono, String direccion, String username) {

        // --- Campos obligatorios ---
        if (estaVacio(nombre) || estaVacio(apellido) || estaVacio(dniString)
                || estaVacio(email) || estaVacio(fechaNacimiento)
                || estaVacio(telefono) || estaVacio(direccion) || estaVacio(username)) {
            throw new IllegalArgumentException("Todos los campos son obligatorios.");
        }

        // --- Formato de email ---
        if (!esEmailValido(email)) {
            throw new IllegalArgumentException("Ingrese un email valido.");
        }

        // --- Email único entre docentes ---
        if (Docente.findFirst("email = ?", email) != null) {
            throw new IllegalArgumentException("Ya existe un docente con ese email.");
        }

        // --- El User debe existir ---
        User usuarioExistente = User.findFirst("name = ?", username);
        if (usuarioExistente == null) {
            throw new IllegalArgumentException("No existe un usuario con ese nombre.");
        }

        // --- El User no puede estar ya asociado a otro docente ---
        if (Docente.findFirst("user_id = ?", usuarioExistente.getId()) != null) {
            throw new IllegalArgumentException("Ese usuario ya esta asociado a otro docente.");
        }

        // --- El User no puede ser ADMINISTRADOR ---
        if ("ADMINISTRADOR".equals(usuarioExistente.getString("rol"))) {
            throw new IllegalArgumentException("No puedes asignar un administrador como docente.");
        }

        // --- DNI debe ser numérico ---
        Integer dni;
        try {
            dni = Integer.parseInt(dniString.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("DNI debe ser un numero valido.");
        }

        // --- DNI único en Persona ---
        if (Persona.findFirst("dni = ?", dni) != null) {
            throw new IllegalArgumentException("Ya existe una persona registrada con ese DNI.");
        }

        // --- Persistencia ---
        try {
            Persona persona = new Persona();
            persona.setDni(dni);
            persona.setNombre(nombre.trim());
            persona.setApellido(apellido.trim());
            persona.setFechaNacimiento(fechaNacimiento.trim());
            persona.setTelefono(telefono.trim());
            persona.setDireccion(direccion.trim());

            Docente docente = new Docente();
            docente.setDNI(dni);
            docente.setEmail(email.trim());
            docente.set("user_id", usuarioExistente.getId());
            docente.saveIt();

            usuarioExistente.set("rol", "DOCENTE");
            usuarioExistente.saveIt();
            persona.saveIt();

            logger.info("Docente creado: DNI={}, usuario={}", dni, username);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("UNIQUE constraint failed: Persona.dni")) {
                throw new IllegalArgumentException("Ya existe una persona registrada con ese DNI.");
            }
            throw new RuntimeException("Error al agregar docente: " + msg, e);
        }
    }

    // EDITAR
    public void editarDocente(Integer codigoProfesor, String nombre, String apellido,
                               String fechaNacimiento, String telefono,
                               String direccion, String email) {

        Docente docente = Docente.findFirst("codigo_profesor = ?", codigoProfesor);
        if (docente == null) {
            throw new IllegalArgumentException("Docente no encontrado.");
        }

        Integer dni = docente.getInteger("dni");

        try {
            Base.openTransaction();

            Base.exec(
                "UPDATE Persona " +
                "SET nombre = ?, apellido = ?, fecha_nacimiento = ?, telefono = ?, direccion = ? " +
                "WHERE dni = ?",
                nombre, apellido, fechaNacimiento, telefono, direccion, dni
            );

            Base.exec(
                "UPDATE Docente SET email = ? WHERE codigo_profesor = ?",
                email, codigoProfesor
            );

            Base.commitTransaction();
            logger.info("Docente editado: codigoProfesor={}", codigoProfesor);

        } catch (Exception e) {
            Base.rollbackTransaction();
            throw new RuntimeException("Error al actualizar docente.", e);
        }
    }

    // ELIMINAR
    public void eliminarDocente(Integer codigoProfesor) {

        Docente docente = Docente.findFirst("codigo_profesor = ?", codigoProfesor);
        if (docente == null) {
            throw new IllegalArgumentException("Docente no encontrado.");
        }

        Integer dni    = docente.getInteger("dni");
        Integer userId = docente.getInteger("user_id");
        User    user   = User.findById(userId);

        try {
            Base.openTransaction();

            Base.exec("DELETE FROM Docente WHERE codigo_profesor = ?", codigoProfesor);
            Base.exec("DELETE FROM Persona WHERE dni = ?", dni);

            if (user != null) {
                user.set("rol", "UNASSIGNED");
                user.saveIt();
            }

            Base.commitTransaction();
            logger.info("Docente eliminado: codigoProfesor={}", codigoProfesor);

        } catch (Exception e) {
            Base.rollbackTransaction();
            throw new RuntimeException("Error al eliminar docente.", e);
        }
    }

    public Map<String, Object> obtenerVistaMateriasDocente(Integer codigoProfesor) {
        Docente docente = Docente.findFirst(
                "codigo_profesor = ?",
                codigoProfesor
        );

        if (docente == null) {
            return null;
        }

        Persona persona = Persona.findFirst(
                "dni = ?",
                docente.getInteger("dni")
        );

        List<PeriodoAcademico> asignacionesDB =
                PeriodoAcademico.where(
                        "codigo_profesor = ?",
                        codigoProfesor
                );

        List<Map<String, Object>> asignaciones = new ArrayList<>();

        for (PeriodoAcademico pa : asignacionesDB) {

            Materia materia = Materia.findFirst(
                    "cod_materia = ?",
                    pa.getInteger("cod_materia")
            );

            Map<String, Object> item = new HashMap<>();

            item.put("id", pa.getId());
            item.put("fecha", pa.getString("fecha"));
            item.put("cargo", pa.getString("cargo"));

            item.put(
                    "nombreMateria",
                    materia != null
                            ? materia.getString("nombre")
                            : "Materia desconocida"
            );

            asignaciones.add(item);
        }

        List<Materia> todasMaterias = Materia.findAll();

        Set<Integer> materiasAsignadas = asignacionesDB.stream()
                .map(pa -> pa.getInteger("cod_materia"))
                .collect(Collectors.toSet());

        List<Map<String, Object>> materiasDisponibles =
                new ArrayList<>();

        for (Materia materia : todasMaterias) {

            Integer codigo = materia.getInteger("cod_materia");

            if (!materiasAsignadas.contains(codigo)) {

                Map<String, Object> item = new HashMap<>();

                item.put("cod_Materia", codigo);
                item.put("nombre", materia.getString("nombre"));

                materiasDisponibles.add(item);
            }
        }

        Map<String, Object> model = new HashMap<>();

        model.put("codigoProfesor", codigoProfesor);

        model.put(
                "nombreDocente",
                persona.getString("nombre") + " " +
                persona.getString("apellido")
        );

        model.put("asignaciones", asignaciones);
        model.put("sinAsignaciones", asignaciones.isEmpty());

        model.put("materiasDisponibles", materiasDisponibles);
        model.put("sinMaterias", materiasDisponibles.isEmpty());

        return model;
    }

    public void asignarMateria(Integer codigoProfesor, String codMateriaStr, String fecha, String cargo) {
        if (codMateriaStr == null || codMateriaStr.isEmpty() || fecha == null || fecha.isEmpty() || cargo == null || cargo.isEmpty()) {
            throw new IllegalArgumentException("Todos los campos son obligatorios.");
        }

        Integer codMateria;

        try {
            codMateria = Integer.parseInt(codMateriaStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Código de materia inválido.");
        }

        PeriodoAcademico existente =
                PeriodoAcademico.findFirst(
                        "codigo_profesor = ? AND cod_materia = ?",
                        codigoProfesor,
                        codMateria
                );

        if (existente != null) {
            throw new IllegalArgumentException(
                    "Ese docente ya está asignado a esa materia."
            );
        }

        PeriodoAcademico asignacion =
                new PeriodoAcademico();

        asignacion.set(
                "codigo_profesor",
                codigoProfesor
        );

        asignacion.set(
                "cod_materia",
                codMateria
        );

        asignacion.set(
                "fecha",
                fecha
        );

        asignacion.set(
                "cargo",
                cargo
        );

        asignacion.saveIt();
    }
    
    public void quitarMateria(
            Integer codigoProfesor,
            Integer asignacionId
    ) {

        PeriodoAcademico asignacion =
                PeriodoAcademico.findById(asignacionId);

        if (asignacion == null) {
            throw new IllegalArgumentException(
                    "Asignación inexistente."
            );
        }

        if (!codigoProfesor.equals(
                asignacion.getInteger("codigo_profesor"))) {

            throw new IllegalArgumentException(
                    "La asignación no pertenece al docente."
            );
        }

        asignacion.delete();
    }
    // HELPERS PRIVADOS
    private boolean estaVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    /**
     * Validación básica de formato de email.
     * Extraído del método privado esEmailValido() de App.java.
     */
    private boolean esEmailValido(String email) {
        if (email == null) return false;
        return email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }
}