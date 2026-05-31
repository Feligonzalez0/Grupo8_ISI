-- Elimina la tabla 'users' si ya existe para asegurar un inicio limpio
-- DROP TABLE IF EXISTS users;

-- Crea la tabla 'users' con los campos originales, adaptados para SQLite
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    rol TEXT NOT NULL DEFAULT 'UNASSIGNED'
        CHECK (rol IN ('ALUMNO', 'DOCENTE', 'ADMINISTRADOR', 'UNASSIGNED'))
);

-- Administrador (contraseña '123') 
INSERT OR IGNORE INTO users (name, password, rol) VALUES (
    'admin',
    '$2a$12$AoKGRGy5pfvc9LVM2rhN6uJabTr/R9SV8rF9CsuePFuoskRa.9k9K',
    'ADMINISTRADOR'
);

CREATE TABLE IF NOT EXISTS Persona (
    dni INTEGER PRIMARY KEY,
    nombre TEXT NOT NULL,
    apellido TEXT NOT NULL,
    fecha_nacimiento TEXT,
    telefono TEXT,
    direccion TEXT
);

CREATE TABLE IF NOT EXISTS Docente (
    dni INTEGER,
    codigo_profesor INTEGER PRIMARY KEY AUTOINCREMENT,
    email TEXT,
    user_id INTEGER UNIQUE,

    FOREIGN KEY (dni) REFERENCES Persona(dni),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS Estudiante (
    dni INTEGER PRIMARY KEY,
    nro_legajo INTEGER NOT NULL UNIQUE,
    email TEXT NOT NULL,
    user_id INTEGER UNIQUE,

    FOREIGN KEY (dni) REFERENCES Persona(dni),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS PlanDeEstudios (
    cod_plan INTEGER PRIMARY KEY AUTOINCREMENT,
    año INTEGER NOT NULL,
    vigencia INTEGER NOT NULL,
    años_total INTEGER NOT NULL,
    cantidad_materias_total INTEGER NOT NULL,
    cod_carrera INTEGER NOT NULL,
    FOREIGN KEY (cod_carrera) REFERENCES Carrera(cod_carrera)
);

CREATE TABLE IF NOT EXISTS Materia (
    cod_materia INTEGER PRIMARY KEY,
    nombre TEXT,
    descripcion TEXT,
    cod_plan INTEGER NOT NULL,

    FOREIGN KEY (cod_plan)
        REFERENCES PlanDeEstudios(cod_plan)
);

CREATE TABLE IF NOT EXISTS Carrera (
    cod_carrera INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT,
    descripcion TEXT
);