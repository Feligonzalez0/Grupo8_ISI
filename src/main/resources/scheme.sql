CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    rol TEXT NOT NULL DEFAULT 'UNASSIGNED' CHECK (rol IN ('ALUMNO', 'DOCENTE', 'ADMINISTRADOR', 'UNASSIGNED'))
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

    FOREIGN KEY (cod_plan) REFERENCES PlanDeEstudios(cod_plan)
);

CREATE TABLE IF NOT EXISTS Carrera (
    cod_carrera INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT,
    descripcion TEXT
);

CREATE TABLE IF NOT EXISTS Correlatividad (
    cod_materia INTEGER NOT NULL,
    cod_correlativa INTEGER NOT NULL,

    PRIMARY KEY (cod_materia, cod_correlativa),

    FOREIGN KEY (cod_materia) REFERENCES Materia(cod_materia),
    FOREIGN KEY (cod_correlativa) REFERENCES Materia(cod_materia),

    CHECK (cod_materia != cod_correlativa)-- SPLIT
);

CREATE TABLE IF NOT EXISTS Estado (
    dni_estudiante INTEGER NOT NULL,
    cod_materia INTEGER NOT NULL,
    estado TEXT NOT NULL CHECK (estado IN ('INSCRIPTO','REGULAR','APROBADO','LIBRE')),

    PRIMARY KEY (dni_estudiante, cod_materia),

    FOREIGN KEY (dni_estudiante) REFERENCES Estudiante(dni),
    FOREIGN KEY (cod_materia) REFERENCES Materia(cod_materia)
);

CREATE TABLE IF NOT EXISTS PeriodoAcademico (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo_profesor INTEGER NOT NULL,
    cod_materia INTEGER NOT NULL,
    fecha TEXT NOT NULL,
    cargo TEXT NOT NULL CHECK (cargo IN ('RESPONSABLE_DE_CATEDRA', 'JEFE_DE_TRABAJOS_PRACTICOS', 'AYUDANTE')),

    FOREIGN KEY (codigo_profesor) REFERENCES Docente(codigo_profesor),
    FOREIGN KEY (cod_materia) REFERENCES Materia(cod_materia)
);

CREATE TABLE IF NOT EXISTS ExamenFinal(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo_profesor INTEGER NOT NULL,
    cod_materia INTEGER NOT NULL,
    fecha TEXT NOT NULL,
    
    FOREIGN KEY (codigo_profesor) REFERENCES Docente(codigo_profesor),
    FOREIGN KEY (cod_materia) REFERENCES Materia(cod_materia)
);

CREATE TABLE IF NOT EXISTS InscripcionExamen (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    dni_estudiante INTEGER NOT NULL,
    id_examen      INTEGER NOT NULL,
    FOREIGN KEY (dni_estudiante) REFERENCES Estudiante(dni),
    FOREIGN KEY (id_examen)      REFERENCES ExamenFinal(id),
    UNIQUE (dni_estudiante, id_examen)
);

INSERT INTO users (name, password, rol) VALUES ('docente1', '$2a$12$AoKGRGy5pfvc9LVM2rhN6uJabTr/R9SV8rF9CsuePFuoskRa.9k9K', 'DOCENTE');
INSERT INTO users (name, password, rol) VALUES ('alumno1', '$2a$12$AoKGRGy5pfvc9LVM2rhN6uJabTr/R9SV8rF9CsuePFuoskRa.9k9K', 'ALUMNO');


INSERT INTO Persona (dni, nombre, apellido, fecha_nacimiento, telefono, direccion) VALUES (11111111, 'Carlos', 'Lopez', '1985-03-10', '3511111111', 'Av. Siempre Viva 1');
INSERT INTO Persona (dni, nombre, apellido, fecha_nacimiento, telefono, direccion) VALUES (22222222, 'Maria', 'Gomez', '2001-06-15', '3522222222', 'Calle Falsa 456');

INSERT INTO Docente (dni, email, user_id) VALUES (11111111, 'carlos@test.com', 2);
INSERT INTO Estudiante (dni, nro_legajo, email, user_id) VALUES (22222222, 1001, 'maria@test.com', 3);


INSERT INTO Carrera (nombre, descripcion) VALUES ('Ingenieria en Sistemas', 'Carrera de sistemas');
INSERT INTO PlanDeEstudios (año, vigencia, años_total, cantidad_materias_total, cod_carrera) VALUES (2020, 5, 5, 30, 1);
INSERT INTO Materia (cod_materia, nombre, descripcion, cod_plan) VALUES (101, 'Algoritmos', 'Materia de algoritmos', 1);

INSERT INTO PeriodoAcademico (codigo_profesor, cod_materia, fecha, cargo) VALUES (1, 101, '2024-01-01', 'RESPONSABLE_DE_CATEDRA');


INSERT INTO Estado (dni_estudiante, cod_materia, estado) VALUES (22222222, 101, 'REGULAR');