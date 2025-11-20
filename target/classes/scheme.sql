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
    fecha_nacimiento INTEGER,
    telefono INTEGER,
    direccion TEXT
);

CREATE TABLE Docente (
    dni INTEGER PRIMARY KEY UNIQUE,
    codigo_profesor INTEGER NOT NULL,
    email TEXT,
    CONSTRAINT fk_dni1 FOREIGN KEY (dni) REFERENCES Persona(dni)
);

CREATE TABLE Estudiante (
    dni INTEGER PRIMARY KEY UNIQUE,
    nro_legajo INTEGER NOT NULL,
    email TEXT NOT NULL,
    CONSTRAINT fk_dni2 FOREIGN KEY (dni) REFERENCES Persona(dni)
);

CREATE TABLE PlanDeEstudios(
   cod_plan INTEGER NOT NULL PRIMARY KEY,
   año DATE,
   vigencia INTEGER NOT NULL,
   años_total INTEGER NOT NULL,
   cantidad_materias_total INTEGER NOT NULL
);

CREATE TABLE Materia(
    cod_materia INTEGER NOT NULL PRIMARY KEY,
    nombre TEXT,
    descripcion TEXT,
    cod_plan INTEGER NOT NULL,
    CONSTRAINT fk_cod_materia FOREIGN KEY (cod_plan) references PlanDeEstudio(cod_plan)
);

CREATE TABLE Carrera(
    cod_carrera INTEGER PRIMARY KEY,
    nombre TEXT,
    descripcion TEXT
);