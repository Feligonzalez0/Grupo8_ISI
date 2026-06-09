package com.is1.proyecto.services.adminService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javalite.activejdbc.Base;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.is1.proyecto.models.Carrera;
import com.is1.proyecto.models.PlanDeEstudios;

public class AdminCarreraService {

    private static final Logger logger = LoggerFactory.getLogger(AdminCarreraService.class);

    // === LISTAR carreras ===
    public static List<Map<String, Object>> listarCarreras() {
        List<Carrera> carrerasDB = Carrera.findAll();
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Carrera c : carrerasDB) {
            Map<String, Object> vista = new HashMap<>();
            vista.put("codCarrera", c.getCodigo());
            vista.put("nombre", c.getNombre());
            vista.put("descripcion", c.getDescripcion());
            resultado.add(vista);
        }

        return resultado;
    }

    // === OBTENER una carrera para editar/eliminar ===
    public Map<String, Object> obtenerCarreraParaVista(Integer codCarrera) {
        Carrera carrera = Carrera.findFirst("cod_carrera = ?", codCarrera);
        if (carrera == null) return null;

        Map<String, Object> vista = new HashMap<>();
        vista.put("codCarrera", carrera.getCodigo());
        vista.put("nombre", carrera.getNombre());
        vista.put("descripcion", carrera.getDescripcion());

        return vista;
    }

    // === OBTENER vista para confirmar eliminación ===
    public Map<String, Object> obtenerVistaEliminarCarrera(Integer codCarrera) {
        Carrera carrera = Carrera.findFirst("cod_carrera = ?", codCarrera);
        if (carrera == null) return null;

        // Verificar si tiene planes asociados
        PlanDeEstudios planAsociado = PlanDeEstudios.findFirst("cod_carrera = ?", codCarrera);

        Map<String, Object> vista = new HashMap<>();
        vista.put("codCarrera", carrera.getCodigo());
        vista.put("nombre", carrera.getNombre());
        vista.put("descripcion", carrera.getDescripcion());
        vista.put("tienePlanes", planAsociado != null);

        return vista;
    }

    // === CREAR carrera ===
    public void crearCarrera(String nombre, String descripcion) {
        // Validaciones
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }

        nombre = nombre.trim();

        // Verificar si ya existe una carrera con ese nombre
        Carrera carreraExistente = Carrera.findFirst("nombre = ?", nombre);
        if (carreraExistente != null) {
            throw new IllegalArgumentException("Ya existe una carrera con ese nombre.");
        }

        try {
            Carrera carrera = new Carrera();
            carrera.setNombre(nombre);
            carrera.setDescripcion(descripcion);
            carrera.saveIt();

            logger.info("Carrera creada: Nombre={}", nombre);

        } catch (Exception e) {
            throw new RuntimeException("Error al crear la carrera: " + e.getMessage(), e);
        }
    }

    // === EDITAR carrera ===
    public void editarCarrera(Integer codCarrera, String nombre, String descripcion) {
        Carrera carrera = Carrera.findFirst("cod_carrera = ?", codCarrera);
        if (carrera == null) {
            throw new IllegalArgumentException("Carrera no encontrada.");
        }

        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }

        try {
            Base.exec(
                "UPDATE Carrera SET nombre = ?, descripcion = ? WHERE cod_carrera = ?",
                nombre.trim(), descripcion, codCarrera
            );

            logger.info("Carrera editada: codCarrera={}", codCarrera);

        } catch (Exception e) {
            throw new RuntimeException("Error al actualizar la carrera: " + e.getMessage(), e);
        }
    }

    // === ELIMINAR carrera ===
    public void eliminarCarrera(Integer codCarrera) {
        Carrera carrera = Carrera.findFirst("cod_carrera = ?", codCarrera);
        if (carrera == null) {
            throw new IllegalArgumentException("Carrera no encontrada.");
        }

        // Verificar si tiene planes asociados
        PlanDeEstudios planAsociado = PlanDeEstudios.findFirst("cod_carrera = ?", codCarrera);
        if (planAsociado != null) {
            throw new IllegalArgumentException("No se puede eliminar una carrera con planes asociados.");
        }

        try {
            Base.exec("DELETE FROM Carrera WHERE cod_carrera = ?", codCarrera);

            logger.info("Carrera eliminada: codCarrera={}", codCarrera);

        } catch (Exception e) {
            throw new RuntimeException("Error al eliminar la carrera: " + e.getMessage(), e);
        }
    }
}