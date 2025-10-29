-- Elimina la tabla 'users' si ya existe para asegurar un inicio limpio
DROP TABLE IF EXISTS users;

-- Crea la tabla 'users' con los campos originales, adaptados para SQLite
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT, -- Clave primaria autoincremental para SQLite
    name TEXT NOT NULL UNIQUE,            -- Nombre de usuario (TEXT es el tipo de cadena recomendado para SQLite), con restricción UNIQUE
    password TEXT NOT NULL                -- Contraseña hasheada (TEXT es el tipo de cadena recomendado para SQLite)
);

CREATE TABLE Persona (
    dni INTEGER PRIMARY KEY UNIQUE,
    nombre TEXT NOT NULL,
    apellido TEXT NOT NULL,
    edad INTEGER,
    nacimiento INTEGER,
    telefono INTEGER,
    direccion TEXT
);

CREATE TABLE Docente (
    dni INTEGER PRIMARY KEY UNIQUE,
    codigo_profesor INTEGER ,
    CONSTRAINT fk_dni1 Docente(dni) REFERENCES Persona(dni)
);

CREATE TABLE Estudiante (
    dni INTEGER PRIMARY KEY UNIQUE,
    nro_legajo INTEGER,
    email TEXT NOT NULL,
    CONSTRAINT fk_dni2 Estudiante(dni) REFERENCES Persona(dni)
);

create table Materia(
    codigo INTEGER NOT NULL PRIMARY KEY,
    nombre TEXT,
    descripcion TEXT,
    cod_plan INTEGER NOT NULL,
    CONSTRAINT fk_cod1 Materia(cod_plan) references PlanDeEstudios(cod_plan)
);

create table PlanDeEstudios(
   cod_plan INTEGER NOT NULL PRIMARY KEY,
   año DATE,
   vigencia INTEGER NOT NULL,
   años_total INTEGER NOT NULL,
   cantidad_materias_total INTEGER NOT NULL
);

create table carrera(
    cod_carrera INTEGER PRIMARY KEY ,
    nombre TEXT,
    descripcion TEXT,
);