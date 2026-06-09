package com.is1.proyecto.controller;
 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
 
import org.javalite.activejdbc.Base;
 
import com.is1.proyecto.models.Docente;
import com.is1.proyecto.models.Estado;
import com.is1.proyecto.models.Estudiante;
import com.is1.proyecto.models.ExamenFinal;
import com.is1.proyecto.models.InscripcionExamen;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.MaterialEstudio;
import com.is1.proyecto.models.PeriodoAcademico;
import com.is1.proyecto.models.Persona;
import com.is1.proyecto.services.AuditoriaService;
 
import spark.ModelAndView;
import spark.Request;
import spark.Response;
 
/**
 * Controller que maneja todas las rutas del área de docentes (/docente/*).
 */
public class DocenteController {

    private static final String UPLOAD_DIR = "materialEstudio";

    public DocenteController() {
        new java.io.File(UPLOAD_DIR).mkdirs();
    }

    // GET /docente/examenes/crear  — formulario para crear un examen final
    public ModelAndView showCrearExamen(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Docente docente = Docente.findFirst("user_id = ?", userId);

        if(docente == null) {
            res.redirect("/dashboard?error=No se encontró el perfil de docente.");
            return null;
        }

        List<PeriodoAcademico> periodos = PeriodoAcademico.where("codigo_profesor = ? AND cargo = ?", docente.getCodigoProfesor(), "RESPONSABLE_DE_CATEDRA");

        List<Map<String, Object>> materiasView = new ArrayList<>();
        for(PeriodoAcademico p : periodos) {
            Materia m = Materia.findFirst("cod_materia = ?", p.getCodMateria());
            if(m != null) {
                Map<String, Object> mv = new HashMap<>();
                mv.put("cod_materia", m.getCodMateria());
                mv.put("nombre", m.getNombre());
                materiasView.add(mv);
            }
        }

        Map<String, Object> model = new HashMap<>();
        model.put("materias", materiasView);
        if(materiasView.isEmpty()) {
            model.put("errorMessage", "No tenés materias asignadas como Responsable de Cátedra.");
        }

        String error = req.queryParams("errorMessage");
        if(error != null) {
            model.put("errorMessage", error);
        }
 
        return new ModelAndView(model, "docente/crearExamen.mustache");
    }

    // POST /docente/examenes/crear
    public Object handleCrearExamen(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Docente docente = Docente.findFirst("user_id = ?", userId);
        if(docente == null) {
            res.redirect("/dashboard");
            return null;
        }

        String codMateriaStr = req.queryParams("cod_materia");
        String fecha = req.queryParams("fecha");

        if(codMateriaStr == null || codMateriaStr.isEmpty() || fecha == null || fecha.isEmpty()) {
            res.redirect("/docente/examenes/crear?errorMessage=Todos los campos son obligatorios.");
            return null;
        }
 
        Integer codMateria = Integer.parseInt(codMateriaStr);
 
        PeriodoAcademico periodo = PeriodoAcademico.findFirst("codigo_profesor = ? AND cod_materia = ? AND cargo = ?", docente.getCodigoProfesor(), codMateria, "RESPONSABLE_DE_CATEDRA");
        if(periodo == null) {
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
    }

    // GET /docente/alumnos  — alumnos inscriptos a las materias del docente
    public ModelAndView showAlumnos(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Docente docente = Docente.findFirst("user_id = ?", userId);
        if(docente == null) {
            res.redirect("/dashboard?error=No se encontró el perfil de docente.");
            return null;
        }

        List<PeriodoAcademico> periodos = PeriodoAcademico.where("codigo_profesor = ?", docente.getCodigoProfesor());

        List<Map<String, Object>> materiasView = new ArrayList<>();
        for(PeriodoAcademico p : periodos) {
            Materia m = Materia.findFirst("cod_materia = ?", p.getCodMateria());
            if(m != null) {
                Map<String, Object> mv = new HashMap<>();
                mv.put("codMateria", m.getCodMateria());
                mv.put("nombre", m.getNombre());
                mv.put("fecha", p.getFecha());
                materiasView.add(mv);
            }
        }

        String codMateriaStr = req.queryParams("cod_materia");
        String fechaFiltro = req.queryParams("fecha");
        List<Map<String, Object>> alumnosView = new ArrayList<>();

        if(codMateriaStr != null && !codMateriaStr.isEmpty()) {
            int codMateria = Integer.parseInt(codMateriaStr);
            PeriodoAcademico perm = PeriodoAcademico.findFirst("codigo_profesor = ? AND cod_materia = ?", docente.getCodigoProfesor(), codMateria);

            if(perm == null) {
                res.redirect("/docente/alumnos?errorMessage=No tenés permiso para esa materia.");
                return null;
            }

            List<Estado> estados;
            if(fechaFiltro != null && !fechaFiltro.isEmpty()) {
                estados = Estado.where("cod_materia = ?", codMateria);
                List<Estado> filtrados = new ArrayList<>();

                for(Estado e : estados) {
                    PeriodoAcademico pa = PeriodoAcademico.findFirst("cod_materia = ? AND codigo_profesor = ? AND fecha LIKE ?", codMateria, docente.getCodigoProfesor(), fechaFiltro + "%");
                    if(pa != null) {
                        filtrados.add(e);
                    }
                }

                estados = filtrados;
            } else {
                estados = Estado.where("cod_materia = ?", codMateria);
            }

            for(Estado e : estados) {
                Estudiante estudiante = Estudiante.findFirst("dni = ?", e.getDniEstudiante());
                Persona persona = Persona.findFirst("dni = ?", e.getDniEstudiante());

                String estadoStr = e.getString("estado");
                String estadoClass;
                if("APROBADO".equals(estadoStr)){
                    estadoClass = "bg-green-100 text-green-700";
                } else if("REGULAR".equals(estadoStr)) {
                    estadoClass = "bg-blue-100 text-blue-700";
                } else if("LIBRE".equals(estadoStr)) {
                    estadoClass = "bg-red-100 text-red-700";
                }   else    {
                    estadoClass = "bg-yellow-100 text-yellow-700";
                }

                Map<String, Object> av = new HashMap<>();
                av.put("dni", e.getDniEstudiante());
                av.put("nroLegajo", estudiante != null ? estudiante.getNroLegajo() : "-");
                av.put("nombre", persona    != null ? persona.getNombre()   : "");
                av.put("apellido", persona    != null ? persona.getApellido() : "");
                av.put("email", estudiante != null ? estudiante.getEmail() : "");
                av.put("estado", estadoStr);
                av.put("esInscripto", "INSCRIPTO".equals(estadoStr));
                av.put("codMateria",  codMateria);
                av.put("estadoClass", estadoClass);
                alumnosView.add(av);
            }
        }

        List<String> fechasUnicas = new ArrayList<>();
        for(PeriodoAcademico p : periodos) {
            String fecha = p.getFecha();
            if(fecha != null && fecha.length() >= 4) {
                String anio = fecha.substring(0, 4);

                if(!fechasUnicas.contains(anio)) {
                    fechasUnicas.add(anio);
                }
            }
        }

        List<Map<String, Object>> fechasView = new ArrayList<>();
        for(String f : fechasUnicas) {
            Map<String, Object> fv = new HashMap<>();
            fv.put("fecha", f);
            fechasView.add(fv);
        }
 
        Map<String, Object> model = new HashMap<>();
        model.put("materias", materiasView);
        model.put("sinMaterias", materiasView.isEmpty());
        model.put("alumnos", alumnosView);
        model.put("sinAlumnos", alumnosView.isEmpty());
        model.put("mostrarTabla", codMateriaStr != null && !codMateriaStr.isEmpty());
        model.put("fechas", fechasView);
        model.put("successMessage", req.queryParams("successMessage"));
        model.put("errorMessage", req.queryParams("errorMessage"));

        return new ModelAndView(model, "docente/alumnosInscriptos.mustache");
    }

    // POST /docente/alumnos/estadoCursada  — marcar alumno como regular o libre
    public Object handleEstadoCursada(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Docente docente = Docente.findFirst("user_id = ?", userId);
        if(docente == null) {
            res.redirect("/dashboard?error=No se encontró el perfil de docente.");
            return null;
        }
 
        String dniEstudianteStr = req.queryParams("dni_estudiante");
        String codMateriaStr = req.queryParams("cod_materia");
        String nuevoEstado = req.queryParams("nuevo_estado");
 
        if(dniEstudianteStr == null || dniEstudianteStr.isEmpty() || codMateriaStr == null || codMateriaStr.isEmpty() || nuevoEstado == null || nuevoEstado.isEmpty()) {
            res.redirect("/docente/alumnos?errorMessage=Datos incompletos.");
            return null;
        }

        if(!"REGULAR".equals(nuevoEstado) && !"LIBRE".equals(nuevoEstado)) {
            res.redirect("/docente/alumnos?errorMessage=Estado inválido.");
            return null;
        }

        int dniEstudiante = Integer.parseInt(dniEstudianteStr);
        int codMateria = Integer.parseInt(codMateriaStr);

        PeriodoAcademico perm = PeriodoAcademico.findFirst("codigo_profesor = ? AND cod_materia = ?", docente.getCodigoProfesor(), codMateria);

        if(perm == null) {
            res.redirect("/docente/alumnos?errorMessage=No tenés permiso para esa materia.");
            return null;
        }

        Estado estadoActual = Estado.findFirst("dni_estudiante = ? AND cod_materia = ?", dniEstudiante, codMateria);
        if(estadoActual == null) {
            res.redirect("/docente/alumnos?errorMessage=No se encontró el estado del alumno en esa materia.");
            return null;
        }

        if(!"INSCRIPTO".equals(estadoActual.getString("estado"))) {
            res.redirect("/docente/alumnos?errorMessage=Solo se puede cambiar el estado de alumnos INSCRIPTOS.");
            return null;
        }

        try {
            Base.openTransaction();
            Base.exec("UPDATE Estado SET estado = ? WHERE dni_estudiante = ? AND cod_materia = ?", nuevoEstado, dniEstudiante, codMateria);
            Base.commitTransaction();

            String msg = "REGULAR".equals(nuevoEstado) ? "Alumno marcado como Regular correctamente." : "Alumno marcado como Libre correctamente.";
            AuditoriaService.registrarAuditoria(req, "CAMBIAR_ESTADO_CURSADA", "dni:" + dniEstudiante + " codMateria:" + codMateria + " nuevoEstado:" + nuevoEstado);

            res.redirect("/docente/alumnos?cod_materia=" + codMateria + "&successMessage=" + java.net.URLEncoder.encode(msg, "UTF-8"));
        } catch (Exception e) {
            Base.rollbackTransaction();
            e.printStackTrace();

            res.redirect("/docente/alumnos?errorMessage=Error al actualizar el estado: " + e.getMessage());
        }

        return null;
    }

    // GET /docente/notas  — cargar notas de exámenes finales
    public ModelAndView showNotas(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Docente docente = Docente.findFirst("user_id = ?", userId);
        if(docente == null) {
            res.redirect("/dashboard?error=No se encontró el perfil de docente.");
            return null;
        }

        List<ExamenFinal> examenesDB = ExamenFinal.where("codigo_profesor = ?", docente.getCodigoProfesor());

        List<Map<String, Object>> examenesView = new ArrayList<>();
        for(ExamenFinal ex : examenesDB) {
            Materia mat = Materia.findFirst("cod_materia = ?", ex.getCodMateria());
            Map<String, Object> ev = new HashMap<>();
            ev.put("idExamen", ex.getId());
            ev.put("nombreMateria", mat != null ? mat.getNombre() : "Sin nombre");
            ev.put("fecha", ex.getFecha());
            examenesView.add(ev);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("examenes", examenesView);
        model.put("sinExamenes", examenesView.isEmpty());

        String idExamenStr = req.queryParams("id_examen");
        if(idExamenStr != null && !idExamenStr.isEmpty()) {
            int idExamen = Integer.parseInt(idExamenStr);

            for(Map<String, Object> ev : examenesView) {
                ev.put("seleccionado", ev.get("idExamen").equals(idExamen));
            }

            ExamenFinal examen = ExamenFinal.findById(idExamen);
            if(examen == null || !examen.getCodigoProfesor().equals(docente.getCodigoProfesor())) {
                res.redirect("/docente/notas?errorMessage=Examen no encontrado o sin permiso.");
                return null;
            }

            Materia materia = Materia.findFirst("cod_materia = ?", examen.getCodMateria());
            List<InscripcionExamen> inscripciones = InscripcionExamen.where("id_examen = ?", idExamen);

            List<Map<String, Object>> alumnosView = new ArrayList<>();
            for(InscripcionExamen insc : inscripciones) {
                Estudiante estudiante = Estudiante.findFirst("dni = ?", insc.getDniEstudiante());
                Persona persona = Persona.findFirst("dni = ?", insc.getDniEstudiante());

                Estado estadoActual = Estado.findFirst("dni_estudiante = ? AND cod_materia = ?", insc.getDniEstudiante(), examen.getCodMateria());

                String estadoStr = estadoActual != null ? estadoActual.getString("estado") : "REGULAR";
                boolean yaCalificado = "APROBADO".equals(estadoStr) || "LIBRE".equals(estadoStr);

                String estadoClass;
                if("APROBADO".equals(estadoStr)) {
                    estadoClass = "bg-green-100 text-green-700";
                } else if("LIBRE".equals(estadoStr)) {
                    estadoClass = "bg-red-100 text-red-700";
                } else if("REGULAR".equals(estadoStr)) {
                    estadoClass = "bg-blue-100 text-blue-700";
                }   else    {
                    estadoClass = "bg-yellow-100 text-yellow-700";
                }

                String nombre = persona != null ? persona.getNombre()   : "";
                String apellido = persona != null ? persona.getApellido() : "";
                String iniciales = ((!nombre.isEmpty()   ? String.valueOf(nombre.charAt(0)) : "") + (!apellido.isEmpty() ? String.valueOf(apellido.charAt(0)) : "")).toUpperCase();

                Map<String, Object> av = new HashMap<>();
                av.put("dni", insc.getDniEstudiante());
                av.put("nombre", nombre);
                av.put("apellido", apellido);
                av.put("iniciales", iniciales);
                av.put("email", estudiante != null ? estudiante.getEmail()     : "");
                av.put("nroLegajo", estudiante != null ? estudiante.getNroLegajo() : "-");
                av.put("codMateria", examen.getCodMateria());
                av.put("idExamen", idExamen);
                av.put("estadoActual", estadoStr);
                av.put("estadoClass", estadoClass);
                av.put("yaCalificado", yaCalificado);
                alumnosView.add(av);
            }

            model.put("examenSeleccionado", true);
            model.put("nombreMateriaSeleccionada", materia != null ? materia.getNombre() : "Sin nombre");
            model.put("fechaExamen", examen.getFecha());
            model.put("totalAlumnos", alumnosView.size());
            model.put("alumnos", alumnosView);
            model.put("sinAlumnos", alumnosView.isEmpty());
            model.put("idExamenActual", idExamen);
        }

        model.put("successMessage", req.queryParams("successMessage"));
        model.put("errorMessage", req.queryParams("errorMessage"));
 
        return new ModelAndView(model, "docente/notasFinales.mustache");
    }

    // POST /docente/notas/cargar  — guardar resultado de un alumno en examen final
    public Object handleCargarNota(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Docente docente = Docente.findFirst("user_id = ?", userId);
        if(docente == null) {
            res.redirect("/dashboard?error=No se encontró el perfil de docente.");
            return null;
        }

        String idExamenStr = req.queryParams("id_examen");
        String dniEstudianteStr = req.queryParams("dni_estudiante");
        String codMateriaStr = req.queryParams("cod_materia");
        String resultado = req.queryParams("resultado");

        if(idExamenStr == null || idExamenStr.isEmpty() || dniEstudianteStr == null || dniEstudianteStr.isEmpty() || codMateriaStr == null || codMateriaStr.isEmpty() || resultado == null || resultado.isEmpty()) {
            res.redirect("/docente/notas?errorMessage=Datos incompletos. Seleccioná un resultado.");
            return null;
        }

        if(!"APROBADO".equals(resultado) && !"LIBRE".equals(resultado)) {
            res.redirect("/docente/notas?errorMessage=Resultado inválido.");
            return null;
        }

        int idExamen = Integer.parseInt(idExamenStr);
        int dniEstudiante = Integer.parseInt(dniEstudianteStr);
        int codMateria = Integer.parseInt(codMateriaStr);

        ExamenFinal examen = ExamenFinal.findById(idExamen);
        if(examen == null || !examen.getCodigoProfesor().equals(docente.getCodigoProfesor())) {
            res.redirect("/docente/notas?errorMessage=No tenés permiso para ese examen.");
            return null;
        }

        InscripcionExamen inscripcion = InscripcionExamen.findFirst("id_examen = ? AND dni_estudiante = ?", idExamen, dniEstudiante);
        if(inscripcion == null) {
            res.redirect("/docente/notas?id_examen=" + idExamen + "&errorMessage=El alumno no está inscripto a ese examen.");
            return null;
        }

        Estado estadoActual = Estado.findFirst("dni_estudiante = ? AND cod_materia = ?", dniEstudiante, codMateria);
        if(estadoActual == null) {
            res.redirect("/docente/notas?id_examen=" + idExamen + "&errorMessage=No se encontró el estado del alumno en esa materia.");
            return null;
        }

        String estadoStr = estadoActual.getString("estado");
        if("APROBADO".equals(estadoStr) || "LIBRE".equals(estadoStr)) {
            res.redirect("/docente/notas?id_examen=" + idExamen + "&errorMessage=El alumno ya tiene un resultado cargado.");
            return null;
        }

        try {
            Base.openTransaction();
            Base.exec("UPDATE Estado SET estado = ? WHERE dni_estudiante = ? AND cod_materia = ?", resultado, dniEstudiante, codMateria);
            Base.commitTransaction();
 
            String msg = "APROBADO".equals(resultado) ? "Alumno aprobado correctamente." : "Resultado libre registrado.";
            AuditoriaService.registrarAuditoria(req, "CARGAR_NOTA", "idExamen:" + idExamen + " dni:" + dniEstudiante + " resultado:" + resultado);

            res.redirect("/docente/notas?id_examen=" + idExamen + "&successMessage=" + java.net.URLEncoder.encode(msg, "UTF-8"));
        } catch (Exception e) {
            Base.rollbackTransaction();
            e.printStackTrace();

            res.redirect("/docente/notas?id_examen=" + idExamen + "&errorMessage=Error al guardar el resultado: " + e.getMessage());
        }

        return null;
    }

    // GET /docente/material  — ver y subir materiales de estudio
    public ModelAndView showMaterial(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Docente docente = Docente.findFirst("user_id = ?", userId);
        if(docente == null) {
            res.redirect("/dashboard?error=No se encontró el perfil de docente.");
            return null;
        }

        List<PeriodoAcademico> periodos = PeriodoAcademico.where("codigo_profesor = ?", docente.getCodigoProfesor());
        List<Map<String, Object>> materiasView = new ArrayList<>();
        for(PeriodoAcademico p : periodos) {
            Materia m = Materia.findFirst("cod_materia = ?", p.getCodMateria());
            if(m != null) {
                Map<String, Object> mv = new HashMap<>();
                mv.put("codMateria", m.getCodMateria());
                mv.put("nombre",     m.getNombre());
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

    // POST /docente/material/subir  — subir archivo de material de estudio
    public Object handleSubirMaterial(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        Docente docente = Docente.findFirst("user_id = ?", userId);
        if(docente == null) {
            res.redirect("/dashboard");
            return null;
        }

        req.attribute("org.eclipse.jetty.multipartConfig", new javax.servlet.MultipartConfigElement(UPLOAD_DIR));

        try {
            String nombre = req.raw().getPart("nombre") != null ? new String(req.raw().getPart("nombre").getInputStream().readAllBytes()) : "";
            String descripcion = req.raw().getPart("descripcion") != null ? new String(req.raw().getPart("descripcion").getInputStream().readAllBytes()) : "";
            String codMateriaStr = req.raw().getPart("cod_materia") != null ? new String(req.raw().getPart("cod_materia").getInputStream().readAllBytes()) : "";

            javax.servlet.http.Part filePart = req.raw().getPart("archivo");

            if(nombre.isEmpty() || codMateriaStr.isEmpty() || filePart == null || filePart.getSize() == 0) {
                res.redirect("/docente/material?errorMessage=Todos los campos son obligatorios.");
                return null;
            }
 
            String nombreArchivo = filePart.getSubmittedFileName();

            String extension = nombreArchivo.substring(nombreArchivo.lastIndexOf(".") + 1).toLowerCase();
            List<String> formatosPermitidos = List.of("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "jpg", "png");
            if (!formatosPermitidos.contains(extension)) {
                res.redirect("/docente/material?errorMessage=Formato no permitido. Usá: PDF, DOC, DOCX, PPT, PPTX, XLS, XLSX, JPG, PNG.");
                return null;
            }

            int codMateria = Integer.parseInt(codMateriaStr);

            PeriodoAcademico perm = PeriodoAcademico.findFirst("codigo_profesor = ? AND cod_materia = ?", docente.getCodigoProfesor(), codMateria);
            if(perm == null) {
                res.redirect("/docente/material?errorMessage=No tenés permiso para esa materia.");
                return null;
            }

            String nombreUnico = System.currentTimeMillis() + "_" + nombreArchivo;
            String rutaArchivo = UPLOAD_DIR + "/" + nombreUnico;

            try (java.io.InputStream input = filePart.getInputStream(); java.io.FileOutputStream output = new java.io.FileOutputStream(rutaArchivo)) {
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
            AuditoriaService.registrarAuditoria(req, "SUBIR_MATERIAL", "codigoProfesor:" + docente.getCodigoProfesor() + " idMaterial:" + material.getId() + " nombre:" + nombre);

            res.redirect("/docente/material?successMessage=Material subido correctamente.");

        } catch (Exception e) {
            e.printStackTrace();
            res.redirect("/docente/material?errorMessage=Error al subir el archivo: " + e.getMessage());
        }

        return null;
    }

    // GET /material/descargar/:id  — descargar archivo de material de estudio
    public Object handleDescargarMaterial(Request req, Response res) {
        Integer id = Integer.parseInt(req.params(":id"));
        MaterialEstudio material = MaterialEstudio.findById(id);
        if (material == null) {
            res.redirect("/dashboard?error=Material no encontrado.");
            return null;
        }

        java.io.File archivo = new java.io.File(material.getRutaArchivo());
        if(!archivo.exists()) {
            res.redirect("/dashboard?error=El archivo no existe en el servidor.");
            return null;
        }

        res.raw().setContentType("application/octet-stream");
        res.raw().setHeader("Content-Disposition", "attachment; filename=\"" + material.getNombreArchivo() + "\"");

        try (java.io.InputStream is = new java.io.FileInputStream(archivo); java.io.OutputStream os = res.raw().getOutputStream()) {
            is.transferTo(os);
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }

        AuditoriaService.registrarAuditoria(req, "DESCARGAR_MATERIAL", "idMaterial:" + material.getId() + " nombre:" + material.getNombreArchivo());

        return null;
    }
}
