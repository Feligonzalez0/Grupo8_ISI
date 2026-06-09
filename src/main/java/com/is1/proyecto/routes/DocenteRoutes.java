package com.is1.proyecto.routes;

import com.is1.proyecto.controller.DocenteController.DocenteAlumnosController;
import com.is1.proyecto.controller.DocenteController.DocenteExamenController;
import com.is1.proyecto.controller.DocenteController.DocenteMaterialController;
import com.is1.proyecto.controller.DocenteController.DocenteNotasController;

import static spark.Spark.get;
import static spark.Spark.post;
import spark.template.mustache.MustacheTemplateEngine;

public class DocenteRoutes {

    public static void register(MustacheTemplateEngine engine) {
        DocenteExamenController  examenes  = new DocenteExamenController();
        DocenteAlumnosController alumnos   = new DocenteAlumnosController();
        DocenteNotasController   notas     = new DocenteNotasController();
        DocenteMaterialController material = new DocenteMaterialController();
        get("/docente/examenes/crear",        examenes::showCrearExamen,       engine);
        post("/docente/examenes/crear",        examenes::handleCrearExamen           );

        get("/docente/alumnos",               alumnos::showAlumnos,            engine);
        post("/docente/alumnos/estadoCursada", alumnos::handleEstadoCursada          );

        get("/docente/notas",                 notas::showNotas,                engine);
        post("/docente/notas/cargar",          notas::handleCargarNota               );

        get("/docente/material",              material::showMaterial,          engine);
        post("/docente/material/subir",        material::handleSubirMaterial         );
        get("/material/descargar/:id",         material::handleDescargarMaterial     );
    }
}