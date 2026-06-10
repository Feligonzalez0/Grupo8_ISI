package com.is1.proyecto.controller.DocenteController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.models.Docente;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.MaterialEstudio;
import com.is1.proyecto.models.PeriodoAcademico;
import com.is1.proyecto.services.AuditoriaService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public class DocenteMaterialController {

    private static final String UPLOAD_DIR = "materialEstudio";

    public DocenteMaterialController() {
        new java.io.File(UPLOAD_DIR).mkdirs();
    }

    // GET /docente/material
    public ModelAndView showMaterial(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Docente docente = Docente.findFirst("user_id = ?", userId);
        if(docente == null) { res.redirect("/dashboard?error=No se encontró el perfil de docente."); return null; }

        List<PeriodoAcademico> periodos = PeriodoAcademico.where("codigo_profesor = ?", docente.getCodigoProfesor());
        List<Map<String, Object>> materiasView = new ArrayList<>();
        for(PeriodoAcademico p : periodos) {
            Materia m = Materia.findFirst("cod_materia = ?", p.getCodMateria());
            if(m != null) {
                Map<String, Object> mv = new HashMap<>();
                mv.put("codMateria", m.getCodMateria());
                mv.put("nombre", m.getNombre());
                materiasView.add(mv);
            }
        }

        List<MaterialEstudio> materialesDB = MaterialEstudio.where("codigo_profesor = ?", docente.getCodigoProfesor());
        List<Map<String, Object>> materiales = new ArrayList<>();
        for(MaterialEstudio mat : materialesDB) {
            Materia m = Materia.findFirst("cod_materia = ?", mat.getCodMateria());
            Map<String, Object> mv = new HashMap<>();
            mv.put("id", mat.getId());
            mv.put("nombre", mat.getNombre());
            mv.put("descripcion", mat.getDescripcion());
            mv.put("nombreArchivo", mat.getNombreArchivo());
            mv.put("fechaSubida", mat.getFechaSubida());
            mv.put("nombreMateria", m != null ? m.getNombre() : "Sin materia");
            mv.put("idDescarga", mat.getId());
            materiales.add(mv);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("materias", materiasView);
        model.put("sinMaterias", materiasView.isEmpty());
        model.put("materiales", materiales);
        model.put("sinMateriales", materiales.isEmpty());
        model.put("successMessage", req.queryParams("successMessage"));
        model.put("errorMessage", req.queryParams("errorMessage"));
        return new ModelAndView(model, "docente/materialEstudio.mustache");
    }

    // POST /docente/material/subir
    public Object handleSubirMaterial(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Docente docente = Docente.findFirst("user_id = ?", userId);
        if(docente == null) { res.redirect("/dashboard"); return null; }

        req.attribute("org.eclipse.jetty.multipartConfig", new javax.servlet.MultipartConfigElement(UPLOAD_DIR));

        try {
            String nombre = req.raw().getPart("nombre") != null ? new String(req.raw().getPart("nombre").getInputStream().readAllBytes()) : "";
            String descripcion  = req.raw().getPart("descripcion") != null ? new String(req.raw().getPart("descripcion").getInputStream().readAllBytes()) : "";
            String codMateriaStr= req.raw().getPart("cod_materia") != null ? new String(req.raw().getPart("cod_materia").getInputStream().readAllBytes()) : "";
            javax.servlet.http.Part filePart = req.raw().getPart("archivo");

            if(nombre.isEmpty() || codMateriaStr.isEmpty() || filePart == null || filePart.getSize() == 0) {
                res.redirect("/docente/material?errorMessage=Todos los campos son obligatorios."); return null;
            }

            String nombreArchivo = filePart.getSubmittedFileName();
            String extension = nombreArchivo.substring(nombreArchivo.lastIndexOf(".") + 1).toLowerCase();
            if(!List.of("pdf","doc","docx","ppt","pptx","xls","xlsx","jpg","png").contains(extension)) {
                res.redirect("/docente/material?errorMessage=Formato no permitido."); return null;
            }

            int codMateria = Integer.parseInt(codMateriaStr);
            PeriodoAcademico perm = PeriodoAcademico.findFirst(
                "codigo_profesor = ? AND cod_materia = ?", docente.getCodigoProfesor(), codMateria);
            if(perm == null) { 
                res.redirect("/docente/material?errorMessage=No tenés permiso para esa materia."); return null; 
            }

            String rutaArchivo = UPLOAD_DIR + "/" + System.currentTimeMillis() + "_" + nombreArchivo;
            try (java.io.InputStream input = filePart.getInputStream();
                 java.io.FileOutputStream output = new java.io.FileOutputStream(rutaArchivo)) {
                input.transferTo(output);
            }

            MaterialEstudio material = new MaterialEstudio();
            material.setCodMateria(codMateria);
            material.setCodigoProfesor(docente.getCodigoProfesor());
            material.setNombre(nombre);
            material.setDescripcion(descripcion);
            material.setNombreArchivo(nombreArchivo);
            material.setRutaArchivo(rutaArchivo);
            material.setFechaSubida(java.time.LocalDate.now().toString());
            material.saveIt();

            AuditoriaService.registrarAuditoria(req, "SUBIR_MATERIAL",
                "codigoProfesor:" + docente.getCodigoProfesor() + " idMaterial:" + material.getId() + " nombre:" + nombre);
            res.redirect("/docente/material?successMessage=Material subido correctamente.");
        } catch (Exception e) {
            e.printStackTrace();
            res.redirect("/docente/material?errorMessage=Error al subir el archivo: " + e.getMessage());
        }
        return null;
    }

    // GET /material/descargar/:id
    public Object handleDescargarMaterial(Request req, Response res) {
        Integer id = Integer.parseInt(req.params(":id"));
        MaterialEstudio material = MaterialEstudio.findById(id);
        if(material == null) {
             res.redirect("/dashboard?error=Material no encontrado."); return null; 
        }

        java.io.File archivo = new java.io.File(material.getRutaArchivo());
        if(!archivo.exists()) { res.redirect("/dashboard?error=El archivo no existe en el servidor."); return null; }

        res.raw().setContentType("application/octet-stream");
        res.raw().setHeader("Content-Disposition", "attachment; filename=\"" + material.getNombreArchivo() + "\"");

        try (java.io.InputStream is = new java.io.FileInputStream(archivo);
             java.io.OutputStream os = res.raw().getOutputStream()) {
            is.transferTo(os);
            os.flush();
        } catch (Exception e) { e.printStackTrace(); }

        AuditoriaService.registrarAuditoria(req, "DESCARGAR_MATERIAL",
            "idMaterial:" + material.getId() + " nombre:" + material.getNombreArchivo());
        return null;
    }
}