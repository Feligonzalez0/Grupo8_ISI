package com.is1.proyecto.controller;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public interface CrudController {
    
    ModelAndView listar(Request req, Response res);
    ModelAndView nuevo(Request req, Response res);
    Object crear(Request req, Response res);
    ModelAndView editar(Request req, Response res);
    Object actualizar(Request req, Response res);
    Object eliminar(Request req, Response res);
}