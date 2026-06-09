package com.is1.proyecto.services.adminService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javalite.activejdbc.Base;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.is1.proyecto.models.Carrera;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.PlanDeEstudios;

public class AdminMateriaService {

    private static final Logger logger = LoggerFactory.getLogger(AdminMateriaService.class);

    // === LISTAR materias ===
    public static List<Map<String, Object>> listarMaterias() {
        List<Materia> materiasDB = Materia.findAll();
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Materia m : materiasDB) {
            Map<String, Object> vista = new HashMap<>();
            vista.put("codMateria", m.getCodMateria());
            vista.put("nombre", m.getNombre());
            vista.put("descripcion", m.getDescripcion());

            PlanDeEstudios plan = PlanDeEstudios.findFirst("cod_plan = ?", m.getCodPlan());
            vista.put("nombrePlan", plan != null ? "Plan " + plan.getAño() : "Sin plan");
            
            if (plan != null) {
                Carrera carrera = Carrera.findFirst("cod_carrera = ?", plan.getCodCarrera());
                vista.put("carrera", carrera != null ? carrera.getNombre() : "Sin carrera");
            } else {
                vista.put("carrera", "Sin carrera");
            }

            resultado.add(vista);
        }

        return resultado;
    }

    // === LISTAR materias (versión para listado completo) ===
    public static List<Map<String, Object>> listarMateriasParaListado() {
        // Es idéntico a listarMaterias(), pero lo mantengo separado por claridad
        return listarMaterias();
    }

    // === OBTENER lista de planes para selects ===
    public List<Map<String, Object>> listarPlanes() {
        List<PlanDeEstudios> planesDB = PlanDeEstudios.findAll();
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (PlanDeEstudios p : planesDB) {
            Map<String, Object> pv = new HashMap<>();
            pv.put("codPlan", p.getCod());
            pv.put("nombrePlan", "Plan " + p.getAño());
            resultado.add(pv);
        }

        return resultado;
    }

    // === OBTENER planes para select en edición (con "selected") ===
    public List<Map<String, Object>> listarPlanesConSelected(Integer codPlanActual) {
        List<PlanDeEstudios> planesDB = PlanDeEstudios.findAll();
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (PlanDeEstudios p : planesDB) {
            Map<String, Object> pv = new HashMap<>();
            pv.put("codPlan", p.getCod());
            pv.put("nombrePlan", "Plan " + p.getAño());
            pv.put("selected", p.getCod().equals(codPlanActual));
            resultado.add(pv);
        }

        return resultado;
    }

    // === OBTENER una materia para editar/eliminar ===
    public Map<String, Object> obtenerMateriaParaVista(Integer codMateria) {
        Materia materia = Materia.findFirst("cod_materia = ?", codMateria);
        if (materia == null) return null;

        Map<String, Object> vista = new HashMap<>();
        vista.put("codMateria", materia.getCodMateria());
        vista.put("nombre", materia.getNombre());
        vista.put("descripcion", materia.getDescripcion());
        vista.put("codPlan", materia.getCodPlan());

        return vista;
    }

    // === CREAR materia ===
    public void crearMateria(String nombre, String codigo, String descripcion, String codPlanStr) {
        // Validaciones
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new IllegalArgumentException("El código es obligatorio.");
        }
        if (codPlanStr == null || codPlanStr.trim().isEmpty()) {
            throw new IllegalArgumentException("El plan es obligatorio.");
        }

        int codPlan;
        int codMat;

        try {
            codPlan = Integer.parseInt(codPlanStr.trim());
            codMat = Integer.parseInt(codigo.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Código de materia y plan deben ser números válidos.");
        }

        // Verificar si ya existe una materia con ese código
        Materia materiaExistente = Materia.findFirst("cod_materia = ?", codMat);
        if (materiaExistente != null) {
            throw new IllegalArgumentException("Ya existe una materia con ese código.");
        }

        // Verificar que el plan exista
        PlanDeEstudios plan = PlanDeEstudios.findFirst("cod_plan = ?", codPlan);
        if (plan == null) {
            throw new IllegalArgumentException("El plan seleccionado no existe.");
        }

        try {
            Materia materia = new Materia();
            materia.setNombre(nombre.trim());
            materia.setDescripcion(descripcion);
            materia.setCodMateria(codMat);
            materia.setCodPlan(codPlan);
            materia.saveIt();

            logger.info("Materia creada: Código={}, Nombre={}", codMat, nombre);

        } catch (Exception e) {
            throw new RuntimeException("Error al crear la materia: " + e.getMessage(), e);
        }
    }

    // === EDITAR materia ===
    public void editarMateria(Integer codMateria, String nombre, String descripcion, String codPlanStr) {
        Materia materia = Materia.findFirst("cod_materia = ?", codMateria);
        if (materia == null) {
            throw new IllegalArgumentException("Materia no encontrada.");
        }

        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        if (codPlanStr == null || codPlanStr.trim().isEmpty()) {
            throw new IllegalArgumentException("El plan es obligatorio.");
        }

        try {
            Base.exec(
                "UPDATE Materia SET nombre = ?, descripcion = ?, cod_plan = ? WHERE cod_materia = ?",
                nombre.trim(), descripcion, Integer.parseInt(codPlanStr.trim()), codMateria
            );

            logger.info("Materia editada: codMateria={}", codMateria);

        } catch (Exception e) {
            throw new RuntimeException("Error al actualizar la materia: " + e.getMessage(), e);
        }
    }

    // === ELIMINAR materia ===
    public void eliminarMateria(Integer codMateria) {
        Materia materia = Materia.findFirst("cod_materia = ?", codMateria);
        if (materia == null) {
            throw new IllegalArgumentException("Materia no encontrada.");
        }

        try {
            Base.openTransaction();
            Base.exec("DELETE FROM PeriodoAcademico WHERE cod_materia = ?", codMateria);
            Base.exec("DELETE FROM Materia WHERE cod_materia = ?", codMateria);
            Base.commitTransaction();

            logger.info("Materia eliminada: codMateria={}", codMateria);

        } catch (Exception e) {
            Base.rollbackTransaction();
            throw new RuntimeException("Error al eliminar la materia: " + e.getMessage(), e);
        }
    }
}
