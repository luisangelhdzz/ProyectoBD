-- =====================================================================
--  LIGA DE BASEBALL MEXICANA - DATOS DE PRUEBA
--  Luis Angel Eduardo Hernández Hernández - 24051002 - Grupo 3A4
--
--  Dos registros en cada tabla (todas menos Usuario y Bitacora).
--
--  Ejecutar como root:   mysql -t -u root -p < datos_prueba.sql
--
--  Los ID no se escriben a mano: MySQL los pone con AUTO_INCREMENT y
--  LAST_INSERT_ID() guarda el que acaba de poner en una variable (@...),
--  que luego se usa como llave foránea en las tablas hijas. Así no
--  choca con los registros que ya tienes en la base.
-- =====================================================================

USE BD_LIGA_BASEBALL;
SET NAMES utf8mb4;   -- para que los acentos se guarden bien

-- Usuario final que queda en ID_Usuario de cada registro
SET @usuario = (SELECT ID_Usuario FROM Usuario WHERE Login_Usuario = 'luis');

-- Corriéndolo con  mysql < datos_prueba.sql : si algo marca error,
-- se detiene ahí y no se guarda nada a medias
START TRANSACTION;


-- ---------------------------------------------------------------------
-- 1. Estado
-- ---------------------------------------------------------------------
INSERT INTO Estado (Nombre_Estado, ID_Usuario)
VALUES ('Nuevo León', @usuario);
SET @nuevo_leon = LAST_INSERT_ID();

INSERT INTO Estado (Nombre_Estado, ID_Usuario)
VALUES ('Yucatán', @usuario);
SET @yucatan = LAST_INSERT_ID();


-- ---------------------------------------------------------------------
-- 2. Ciudad
-- ---------------------------------------------------------------------
INSERT INTO Ciudad (ID_Estado, Nombre_Ciudad, ID_Usuario)
VALUES (@nuevo_leon, 'Monterrey', @usuario);
SET @monterrey = LAST_INSERT_ID();

INSERT INTO Ciudad (ID_Estado, Nombre_Ciudad, ID_Usuario)
VALUES (@yucatan, 'Mérida', @usuario);
SET @merida = LAST_INSERT_ID();


-- ---------------------------------------------------------------------
-- 3. Equipo
-- ---------------------------------------------------------------------
INSERT INTO Equipo (ID_Ciudad, Nombre_Equipo, ID_Usuario)
VALUES (@monterrey, 'Sultanes', @usuario);
SET @sultanes = LAST_INSERT_ID();

INSERT INTO Equipo (ID_Ciudad, Nombre_Equipo, ID_Usuario)
VALUES (@merida, 'Leones', @usuario);
SET @leones = LAST_INSERT_ID();


-- ---------------------------------------------------------------------
-- 4. Jugador   (estatura entre 1.40 y 2.30, peso entre 40 y 200)
-- ---------------------------------------------------------------------
INSERT INTO Jugador (ID_Equipo, Nombre_Jugador, Apellido_Jugador,
                     Estatura_Jugador, Peso_Jugador,
                     Fecha_Nacimiento, Telefono_Jugador, ID_Usuario)
VALUES (@sultanes, 'Carlos', 'Treviño', 1.80, 85.50,
        '1997-03-12', '8110000001', @usuario);
SET @carlos = LAST_INSERT_ID();

INSERT INTO Jugador (ID_Equipo, Nombre_Jugador, Apellido_Jugador,
                     Estatura_Jugador, Peso_Jugador,
                     Fecha_Nacimiento, Telefono_Jugador, ID_Usuario)
VALUES (@leones, 'Jorge', 'Canul', 1.75, 78.00,
        '1999-08-25', '9990000002', @usuario);
SET @jorge = LAST_INSERT_ID();


-- ---------------------------------------------------------------------
-- 5. Manager
-- ---------------------------------------------------------------------
INSERT INTO Manager (ID_Equipo, Nombre_Manager, Fecha_Nacimiento,
                     Telefono_Manager, Correo_Manager, ID_Usuario)
VALUES (@sultanes, 'Roberto Garza', '1975-06-02',
        '8110000003', 'roberto.garza@example.com', @usuario);

INSERT INTO Manager (ID_Equipo, Nombre_Manager, Fecha_Nacimiento,
                     Telefono_Manager, Correo_Manager, ID_Usuario)
VALUES (@leones, 'Miguel Pech', '1978-11-19',
        '9990000004', 'miguel.pech@example.com', @usuario);


-- ---------------------------------------------------------------------
-- 6. Posicion
-- ---------------------------------------------------------------------
INSERT INTO Posicion (Nombre_Posicion, ID_Usuario)
VALUES ('Jardinero central', @usuario);
SET @jardinero = LAST_INSERT_ID();

INSERT INTO Posicion (Nombre_Posicion, ID_Usuario)
VALUES ('Campo corto', @usuario);
SET @campo_corto = LAST_INSERT_ID();


-- ---------------------------------------------------------------------
-- 7. Estadio
-- ---------------------------------------------------------------------
INSERT INTO Estadio (ID_Ciudad, Nombre_Estadio, ID_Usuario)
VALUES (@monterrey, 'Estadio Monterrey', @usuario);
SET @estadio_monterrey = LAST_INSERT_ID();

INSERT INTO Estadio (ID_Ciudad, Nombre_Estadio, ID_Usuario)
VALUES (@merida, 'Parque Kukulcán', @usuario);
SET @parque_kukulcan = LAST_INSERT_ID();


-- ---------------------------------------------------------------------
-- 8. Temporada
-- ---------------------------------------------------------------------
INSERT INTO Temporada (Anio_Temporada, Inicio_Temporada, Fin_Temporada, ID_Usuario)
VALUES (2025, '2025-04-11', '2025-08-31', @usuario);
SET @temporada_2025 = LAST_INSERT_ID();

INSERT INTO Temporada (Anio_Temporada, Inicio_Temporada, Fin_Temporada, ID_Usuario)
VALUES (2026, '2026-04-10', '2026-08-30', @usuario);
SET @temporada_2026 = LAST_INSERT_ID();


-- ---------------------------------------------------------------------
-- 9. Partido   (el local y el visitante tienen que ser distintos)
-- ---------------------------------------------------------------------
-- Sultanes 5 - 3 Leones, en Monterrey
INSERT INTO Partido (ID_Temporada, ID_Estadio, ID_Equipo_Local,
                     ID_Equipo_Visitante, Fecha_Partido,
                     Carreras_Local, Carreras_Visitante, ID_Usuario)
VALUES (@temporada_2025, @estadio_monterrey, @sultanes,
        @leones, '2025-05-16 19:30:00', 5, 3, @usuario);
SET @partido_1 = LAST_INSERT_ID();

-- Leones 4 - 2 Sultanes, en Mérida
INSERT INTO Partido (ID_Temporada, ID_Estadio, ID_Equipo_Local,
                     ID_Equipo_Visitante, Fecha_Partido,
                     Carreras_Local, Carreras_Visitante, ID_Usuario)
VALUES (@temporada_2026, @parque_kukulcan, @leones,
        @sultanes, '2026-04-17 19:00:00', 4, 2, @usuario);


-- ---------------------------------------------------------------------
-- 10. Alineacion   (turno al bat del 1 al 9)
-- ---------------------------------------------------------------------
INSERT INTO Alineacion (ID_Equipo, ID_Jugador, ID_Partido,
                        ID_Posicion, Orden_Bateo, ID_Usuario)
VALUES (@sultanes, @carlos, @partido_1, @jardinero, 1, @usuario);

INSERT INTO Alineacion (ID_Equipo, ID_Jugador, ID_Partido,
                        ID_Posicion, Orden_Bateo, ID_Usuario)
VALUES (@leones, @jorge, @partido_1, @campo_corto, 2, @usuario);


-- ---------------------------------------------------------------------
-- 11. Carrera   (carreras de cada jugador en el partido 1)
-- ---------------------------------------------------------------------
INSERT INTO Carrera (ID_Equipo, ID_Partido, ID_Jugador, Total_Carreras, ID_Usuario)
VALUES (@sultanes, @partido_1, @carlos, 2, @usuario);

INSERT INTO Carrera (ID_Equipo, ID_Partido, ID_Jugador, Total_Carreras, ID_Usuario)
VALUES (@leones, @partido_1, @jorge, 1, @usuario);


-- ---------------------------------------------------------------------
-- 12. Patrocinador
-- ---------------------------------------------------------------------
INSERT INTO Patrocinador (Nombre_Patrocinador, ID_Usuario)
VALUES ('Aceros del Norte', @usuario);
SET @aceros = LAST_INSERT_ID();

INSERT INTO Patrocinador (Nombre_Patrocinador, ID_Usuario)
VALUES ('Refrescos Maya', @usuario);
SET @refrescos = LAST_INSERT_ID();


-- ---------------------------------------------------------------------
-- 13. Contrato
-- ---------------------------------------------------------------------
INSERT INTO Contrato (ID_Equipo, ID_Patrocinador, Inicio_Contrato, Fin_Contrato, ID_Usuario)
VALUES (@sultanes, @aceros, '2026-01-01', '2026-12-31', @usuario);

INSERT INTO Contrato (ID_Equipo, ID_Patrocinador, Inicio_Contrato, Fin_Contrato, ID_Usuario)
VALUES (@leones, @refrescos, '2026-02-01', '2027-01-31', @usuario);


COMMIT;


-- ---------------------------------------------------------------------
-- Comprobación
-- ---------------------------------------------------------------------
SELECT * FROM Estado;
SELECT * FROM Ciudad;
SELECT * FROM Equipo;
SELECT * FROM Jugador;
SELECT * FROM Manager;
SELECT * FROM Posicion;
SELECT * FROM Estadio;
SELECT * FROM Temporada;
SELECT * FROM Partido;
SELECT * FROM Alineacion;
SELECT * FROM Carrera;
SELECT * FROM Patrocinador;
SELECT * FROM Contrato;
