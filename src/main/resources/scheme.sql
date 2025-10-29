-- Elimina la tabla 'users' si ya existe para asegurar un inicio limpio
DROP TABLE IF EXISTS users;

-- Crea la tabla 'users' con los campos originales, adaptados para SQLite
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT, -- Clave primaria autoincremental para SQLite
    name TEXT NOT NULL UNIQUE,          -- Nombre de usuario (TEXT es el tipo de cadena recomendado para SQLite), con restricción UNIQUE
    password TEXT NOT NULL           -- Contraseña hasheada (TEXT es el tipo de cadena recomendado para SQLite)
);

create table Materia(
    codigo  INT NOT NULL PRIMARY KEY,
    nombre TEXT,
    descripcion TEXT,
    cod_plan INTEGER not null,
    constraint  fk  Materia(cod_plan) references Plan_De_Estudios(cod_plan)
);


create table Plan_De_Estudios(
   cod_plan INTEGER not null PRIMARY KEY,
   año date,
   vigencia INTEGER not null,
   años_total INTEGER not null,
   cantidad_materias_total INTEGER not null,

);

create table carrera(
    codigo  INT NOT NULL PRIMARY KEY,
    nombre TEXT,
    descripcion TEXT,

);