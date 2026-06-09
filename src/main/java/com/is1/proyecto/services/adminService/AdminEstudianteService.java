package com.is1.proyecto.services.adminService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javalite.activejdbc.Base;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.is1.proyecto.models.Estudiante;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.Persona;
import com.is1.proyecto.models.User;

public class AdminEstudianteService {

    private static final Logger logger = LoggerFactory.getLogger(AdminEstudianteService.class);

    // === LISTAR estudiantes (vista para dashboard) ===
    public static List<Map<String, Object>> listarEstudiantes() {
        List<Estudiante> estudiantesDB = Estudiante.findAll();
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Estudiante estudiante : estudiantesDB) {
            Map<String, Object> vista = new HashMap<>();

            vista.put("id", estudiante.getInteger("nro_legajo"));
            vista.put("email", estudiante.getString("email"));

            Integer dni = estudiante.getInteger("dni");
            Persona persona = Persona.findFirst("dni = ?", dni);
            if (persona != null) {
                vista.put("dni", persona.getInteger("dni"));
                vista.put("nombre", persona.getString("nombre"));
                vista.put("apellido", persona.getString("apellido"));
                vista.put("telefono", persona.getString("telefono"));
                vista.put("direccion", persona.getString("direccion"));
                vista.put("fecha_nacimiento", persona.getString("fecha_nacimiento"));
            }

            Integer userId = estudiante.getInteger("user_id");
            User user = User.findById(userId);
            if (user != null) {
                vista.put("username", user.getString("name"));
            }

            resultado.add(vista);
        }

        return resultado;
    }

    // === OBTENER un estudiante para editar/eliminar ===
    public Map<String, Object> obtenerEstudianteParaVista(Integer nroLegajo) {
        Estudiante estudiante = Estudiante.findFirst("nro_legajo = ?", nroLegajo);
        if (estudiante == null) return null;

        Integer dni = estudiante.getInteger("dni");
        Integer userId = estudiante.getInteger("user_id");
        Persona persona = Persona.findFirst("dni = ?", dni);
        User user = User.findById(userId);

        Map<String, Object> vista = new HashMap<>();
        vista.put("nro_legajo", estudiante.getInteger("nro_legajo"));
        vista.put("email", estudiante.getString("email"));

        if (persona != null) {
            vista.put("dni", persona.getInteger("dni"));
            vista.put("nombre", persona.getString("nombre"));
            vista.put("apellido", persona.getString("apellido"));
            vista.put("fechaNacimiento", persona.getString("fecha_nacimiento"));
            vista.put("telefono", persona.getString("telefono"));
            vista.put("direccion", persona.getString("direccion"));
        }

        if (user != null) {
            vista.put("username", user.getString("name"));
        }

        return vista;
    }

    // === CREAR estudiante ===
    public void crearEstudiante(String nombre, String apellido, String dniString,
                                String email, String fechaNacimiento,
                                String telefono, String direccion,
                                String username, String nroLegajoString) {

        // Validaciones
        if (estaVacio(nombre) || estaVacio(apellido) || estaVacio(dniString)
                || estaVacio(email) || estaVacio(fechaNacimiento)
                || estaVacio(telefono) || estaVacio(direccion)
                || estaVacio(username) || estaVacio(nroLegajoString)) {
            throw new IllegalArgumentException("Todos los campos son obligatorios.");
        }

        if (!esEmailValido(email)) {
            throw new IllegalArgumentException("Ingrese un email válido.");
        }

        // Email único
        if (Estudiante.findFirst("email = ?", email) != null) {
            throw new IllegalArgumentException("Ya existe un estudiante con ese email.");
        }

        // User existe
        User usuarioExistente = User.findFirst("name = ?", username);
        if (usuarioExistente == null) {
            throw new IllegalArgumentException("No existe un usuario con ese nombre.");
        }

        // User ya asociado a otro estudiante
        if (Estudiante.findFirst("user_id = ?", usuarioExistente.getId()) != null) {
            throw new IllegalArgumentException("Ese usuario ya está asociado a otro estudiante.");
        }

        // User no puede ser ADMIN
        if ("ADMINISTRADOR".equals(usuarioExistente.getString("rol"))) {
            throw new IllegalArgumentException("No puedes asignar un administrador como estudiante.");
        }

        // DNI numérico
        Integer dni;
        try {
            dni = Integer.parseInt(dniString.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("DNI debe ser un número válido.");
        }

        // DNI único en Persona
        if (Persona.findFirst("dni = ?", dni) != null) {
            throw new IllegalArgumentException("Ya existe una persona registrada con ese DNI.");
        }

        // Nro legajo numérico y único
        Integer nroLegajo;
        try {
            nroLegajo = Integer.parseInt(nroLegajoString.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Nro legajo debe ser un número válido.");
        }

        if (Estudiante.findFirst("nro_legajo = ?", nroLegajo) != null) {
            throw new IllegalArgumentException("Ya existe un estudiante con ese legajo.");
        }

        // Persistir
        try {
            Persona persona = new Persona();
            persona.setDni(dni);
            persona.setNombre(nombre.trim());
            persona.setApellido(apellido.trim());
            persona.setFechaNacimiento(fechaNacimiento.trim());
            persona.setTelefono(telefono.trim());
            persona.setDireccion(direccion.trim());

            Estudiante estudiante = new Estudiante();
            estudiante.setDni(dni);
            estudiante.setEmail(email.trim());
            estudiante.setNroLegajo(nroLegajo);
            estudiante.set("user_id", usuarioExistente.getId());

            estudiante.saveIt();
            usuarioExistente.set("rol", "ALUMNO");
            usuarioExistente.saveIt();
            persona.saveIt();

            logger.info("Estudiante creado: DNI={}, legajo={}", dni, nroLegajo);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("UNIQUE constraint failed: Persona.dni")) {
                throw new IllegalArgumentException("Ya existe una persona registrada con ese DNI.");
            }
            throw new RuntimeException("Error al agregar estudiante: " + msg, e);
        }
    }

    // === EDITAR estudiante ===
    public void editarEstudiante(Integer nroLegajo, String nombre, String apellido,
                                 String fechaNacimiento, String telefono,
                                 String direccion, String email) {

        Estudiante estudiante = Estudiante.findFirst("nro_legajo = ?", nroLegajo);
        if (estudiante == null) {
            throw new IllegalArgumentException("Estudiante no encontrado.");
        }

        Integer dni = estudiante.getInteger("dni");

        try {
            Base.openTransaction();

            Base.exec(
                "UPDATE Persona SET nombre = ?, apellido = ?, fecha_nacimiento = ?, telefono = ?, direccion = ? WHERE dni = ?",
                nombre, apellido, fechaNacimiento, telefono, direccion, dni
            );

            Base.exec(
                "UPDATE Estudiante SET email = ? WHERE nro_legajo = ?",
                email, nroLegajo
            );

            Base.commitTransaction();
            logger.info("Estudiante editado: legajo={}", nroLegajo);

        } catch (Exception e) {
            Base.rollbackTransaction();
            throw new RuntimeException("Error al actualizar estudiante.", e);
        }
    }

    // === ELIMINAR estudiante ===
    public void eliminarEstudiante(Integer nroLegajo) {
        Estudiante estudiante = Estudiante.findFirst("nro_legajo = ?", nroLegajo);
        if (estudiante == null) {
            throw new IllegalArgumentException("Estudiante no encontrado.");
        }

        Integer dni = estudiante.getInteger("dni");
        Integer userId = estudiante.getInteger("user_id");
        User user = User.findById(userId);

        try {
            Base.openTransaction();

            Base.exec("DELETE FROM Estudiante WHERE nro_legajo = ?", nroLegajo);
            Base.exec("DELETE FROM Persona WHERE dni = ?", dni);

            if (user != null) {
                user.set("rol", "UNASSIGNED");
                user.saveIt();
            }

            Base.commitTransaction();
            logger.info("Estudiante eliminado: legajo={}", nroLegajo);

        } catch (Exception e) {
            Base.rollbackTransaction();
            throw new RuntimeException("Error al eliminar estudiante.", e);
        }
    }

    // === Obtener vista para inscribir materias ===
    public Map<String, Object> obtenerVistaMateriasEstudiante(Integer nroLegajo) {
        Estudiante estudiante = Estudiante.findFirst("nro_legajo = ?", nroLegajo);
        if (estudiante == null) return null;

        Persona persona = Persona.findFirst("dni = ?", estudiante.getInteger("dni"));

        Map<String, Object> model = new HashMap<>();
        model.put("nro_legajo", estudiante.getNroLegajo());
        model.put("dni", estudiante.getDni());

        if (persona != null) {
            model.put("nombre", persona.getNombre());
            model.put("apellido", persona.getApellido());
        }
        
        // Listar materias disponibles
        List<Materia> materiasDisponibles = Materia.findBySQL(
            "SELECT m.* FROM Materia m " +
            "LEFT JOIN Estado es ON m.cod_materia = es.cod_materia AND es.nro_legajo = ? " +
            "WHERE es.cod_materia IS NULL",
            nroLegajo
        );
        
        // Aquí puedes filtrar materias no cursadas si lo necesitas
        model.put("materias", materiasDisponibles);

        return model;
    }

    // === Inscribir materia ===
    public void inscribirMateria(Integer nroLegajo, Integer codMateria) {
        Estudiante estudiante = Estudiante.findFirst("nro_legajo = ?", nroLegajo);
        if (estudiante == null) {
            throw new IllegalArgumentException("Estudiante no encontrado.");
        }

        try {
            estudiante.inscribirseMateria(codMateria);
            logger.info("Materia {} inscrita a estudiante legajo {}", codMateria, nroLegajo);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // === HELPERS privados ===
    private boolean estaVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private boolean esEmailValido(String email) {
        if (email == null) return false;
        return email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }
}