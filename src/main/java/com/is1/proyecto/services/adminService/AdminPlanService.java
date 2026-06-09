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

public class AdminPlanService {

    private static final Logger logger = LoggerFactory.getLogger(AdminPlanService.class);

    // === LISTAR planes ===
    public static List<Map<String, Object>> listarPlanes() {
        List<PlanDeEstudios> planesDB = PlanDeEstudios.findAll();
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (PlanDeEstudios plan : planesDB) {
            Map<String, Object> vista = new HashMap<>();
            vista.put("codPlan", plan.getCod());
            vista.put("anio", plan.getAño());
            vista.put("vigencia", plan.getVigencia());
            vista.put("aniosTotal", plan.getAñosTotal());
            vista.put("cantMaterias", plan.getCantidadMaterias());

            Carrera carrera = Carrera.findFirst("cod_carrera = ?", plan.getCod());
            vista.put("nombreCarrera", carrera != null ? carrera.getNombre() : "Sin carrera");

            resultado.add(vista);
        }

        return resultado;
    }

    // === OBTENER un plan para editar ===
    public Map<String, Object> obtenerPlanParaVista(Integer codPlan) {
        PlanDeEstudios plan = PlanDeEstudios.findFirst("cod_plan = ?", codPlan);
        if (plan == null) return null;

        Map<String, Object> vista = new HashMap<>();
        vista.put("codPlan", plan.getCod());
        vista.put("anio", plan.getAño());
        vista.put("vigencia", plan.getVigencia());
        vista.put("aniosTotal", plan.getAñosTotal());
        vista.put("cantMaterias", plan.getCantidadMaterias());

        return vista;
    }

    // === OBTENER lista de carreras para selects ===
    public List<Map<String, Object>> listarCarreras() {
        List<Carrera> carrerasDB = Carrera.findAll();
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Carrera c : carrerasDB) {
            Map<String, Object> cv = new HashMap<>();
            cv.put("codCarrera", c.getCodigo());
            cv.put("nombre", c.getNombre());
            resultado.add(cv);
        }

        return resultado;
    }

    // === OBTENER carreras separadas (seleccionada vs no seleccionadas) ===
    public Map<String, Object> obtenerCarrerasParaSelect(Integer codPlanActual) {
        PlanDeEstudios plan = PlanDeEstudios.findFirst("cod_plan = ?", codPlanActual);
        if (plan == null) return null;

        List<Carrera> carrerasDB = Carrera.findAll();
        List<Map<String, Object>> carreraSelected = new ArrayList<>();
        List<Map<String, Object>> carrerasNoSelected = new ArrayList<>();

        for (Carrera c : carrerasDB) {
            Map<String, Object> cv = new HashMap<>();
            cv.put("codCarrera", c.getCodigo());
            cv.put("nombre", c.getNombre());

            if (c.getCodigo().equals(plan.getCod())) {
                carreraSelected.add(cv);
            } else {
                carrerasNoSelected.add(cv);
            }
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("carreraSelected", carreraSelected);
        resultado.put("carrerasNoSelected", carrerasNoSelected);

        return resultado;
    }

    // === CREAR plan ===
    public void crearPlan(String anioStr, String vigenciaStr, String aniosTotalStr,
                          String cantMatStr, String codCarreraStr) {

        // Validaciones
        if (anioStr == null || anioStr.trim().isEmpty() ||
            vigenciaStr == null || vigenciaStr.trim().isEmpty() ||
            aniosTotalStr == null || aniosTotalStr.trim().isEmpty() ||
            cantMatStr == null || cantMatStr.trim().isEmpty() ||
            codCarreraStr == null || codCarreraStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Todos los campos son obligatorios.");
        }

        int año, vigencia, aniosTotal, cantMat, codCarrera;

        try {
            año = Integer.parseInt(anioStr.trim());
            vigencia = Integer.parseInt(vigenciaStr.trim());
            aniosTotal = Integer.parseInt(aniosTotalStr.trim());
            cantMat = Integer.parseInt(cantMatStr.trim());
            codCarrera = Integer.parseInt(codCarreraStr.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Los campos numéricos deben ser números válidos.");
        }

        Carrera carrera = Carrera.findFirst("cod_carrera = ?", codCarrera);
        if (carrera == null) {
            throw new IllegalArgumentException("La carrera seleccionada no existe.");
        }

        try {
            PlanDeEstudios plan = new PlanDeEstudios();
            plan.setAño(año);
            plan.setVigencia(vigencia);
            plan.setAñosTotal(aniosTotal);
            plan.setCantidadMaterias(cantMat);
            plan.set("cod_carrera", codCarrera);
            plan.saveIt();

            logger.info("Plan creado: Año={}, Carrera={}", año, codCarrera);

        } catch (Exception e) {
            throw new RuntimeException("Error al crear el plan: " + e.getMessage(), e);
        }
    }

    // === EDITAR plan ===
    public void editarPlan(Integer codPlan, String vigenciaStr, String aniosTotalStr,
                           String cantMatStr, String codCarreraStr) {

        PlanDeEstudios plan = PlanDeEstudios.findFirst("cod_plan = ?", codPlan);
        if (plan == null) {
            throw new IllegalArgumentException("Plan no encontrado.");
        }

        if (vigenciaStr == null || vigenciaStr.trim().isEmpty() ||
            aniosTotalStr == null || aniosTotalStr.trim().isEmpty() ||
            cantMatStr == null || cantMatStr.trim().isEmpty() ||
            codCarreraStr == null || codCarreraStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Todos los campos son obligatorios.");
        }

        try {
            Base.openTransaction();

            Base.exec(
                "UPDATE PlanDeEstudios SET vigencia = ?, años_total = ?, " +
                "cantidad_materias_total = ?, cod_carrera = ? WHERE cod_plan = ?",
                Integer.parseInt(vigenciaStr.trim()),
                Integer.parseInt(aniosTotalStr.trim()),
                Integer.parseInt(cantMatStr.trim()),
                Integer.parseInt(codCarreraStr.trim()),
                codPlan
            );

            Base.commitTransaction();
            logger.info("Plan editado: codPlan={}", codPlan);

        } catch (Exception e) {
            Base.rollbackTransaction();
            throw new RuntimeException("Error al actualizar el plan: " + e.getMessage(), e);
        }
    }

    // === ELIMINAR plan ===
    public void eliminarPlan(Integer codPlan) {
        PlanDeEstudios plan = PlanDeEstudios.findFirst("cod_plan = ?", codPlan);
        if (plan == null) {
            throw new IllegalArgumentException("Plan no encontrado.");
        }

        try {
            Base.openTransaction();

            // Eliminar materias asociadas (integridad referencial)
            Base.exec("DELETE FROM Materia WHERE cod_plan = ?", codPlan);
            Base.exec("DELETE FROM PlanDeEstudios WHERE cod_plan = ?", codPlan);

            Base.commitTransaction();
            logger.info("Plan eliminado: codPlan={}", codPlan);

        } catch (Exception e) {
            Base.rollbackTransaction();
            throw new RuntimeException("Error al eliminar el plan: " + e.getMessage(), e);
        }
    }

    // === OBTENER vista para confirmar eliminación ===
    public Map<String, Object> obtenerVistaEliminarPlan(Integer codPlan) {
        PlanDeEstudios plan = PlanDeEstudios.findFirst("cod_plan = ?", codPlan);
        if (plan == null) return null;

        Carrera carrera = Carrera.findFirst("cod_carrera = ?", plan.getCod());

        List<Materia> materiasDB = Materia.where("cod_plan = ?", codPlan);
        List<Map<String, Object>> materias = new ArrayList<>();

        for (Materia m : materiasDB) {
            Map<String, Object> mv = new HashMap<>();
            mv.put("codMateria", m.getInteger("cod_materia"));
            mv.put("nombre", m.getString("nombre"));
            materias.add(mv);
        }

        Map<String, Object> vista = new HashMap<>();
        vista.put("codPlan", plan.getCod());
        vista.put("anio", plan.getAño());
        vista.put("vigencia", plan.getVigencia());
        vista.put("nombreCarrera", carrera != null ? carrera.getNombre() : "Sin carrera");
        vista.put("materias", materias);
        vista.put("tieneMaterias", !materias.isEmpty());

        return vista;
    }

    // === LISTAR materias de un plan ===
    public Map<String, Object> obtenerMateriasPorPlan(Integer codPlan) {
        PlanDeEstudios plan = PlanDeEstudios.findFirst("cod_plan = ?", codPlan);
        if (plan == null) return null;

        Carrera carrera = Carrera.findFirst("cod_carrera = ?", plan.getCod());

        List<Materia> materiasDB = Materia.where("cod_plan = ?", codPlan);
        List<Map<String, Object>> materias = new ArrayList<>();

        for (Materia m : materiasDB) {
            Map<String, Object> mv = new HashMap<>();
            mv.put("codMateria", m.getInteger("cod_materia"));
            mv.put("nombre", m.getString("nombre"));
            mv.put("descripcion", m.getString("descripcion"));
            materias.add(mv);
        }

        Map<String, Object> vista = new HashMap<>();
        vista.put("codPlan", plan.getCod());
        vista.put("anio", plan.getAño());
        vista.put("vigencia", plan.getVigencia());
        vista.put("nombreCarrera", carrera != null ? carrera.getNombre() : "Sin carrera");
        vista.put("materias", materias);
        vista.put("sinMaterias", materias.isEmpty());

        return vista;
    }
}