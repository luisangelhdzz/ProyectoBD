-- =====================================================================
--  LIGA DE BASEBALL MEXICANA - USUARIOS FINALES Y AUDITORIA
--  Luis Angel Eduardo Hernández Hernández - 24051002 - Grupo 3A4
--
--  Ejecutar como root:   mysql -u root -p
-- =====================================================================

USE BD_LIGA_BASEBALL;
SET NAMES utf8mb4;   -- para que los acentos se guarden bien


-- ---------------------------------------------------------------------
-- 1. Tabla de usuarios finales (los que entran a la aplicación)
--    La contraseña se guarda cifrada con SHA2 (64 caracteres).
-- ---------------------------------------------------------------------
CREATE TABLE Usuario (
    ID_Usuario          INT          NOT NULL AUTO_INCREMENT,
    Nombre_Usuario      VARCHAR(45)  NOT NULL,
    Login_Usuario       VARCHAR(20)  NOT NULL,
    Contrasena_Usuario  CHAR(64)     NOT NULL,
    CONSTRAINT PK_Usuario PRIMARY KEY (ID_Usuario),
    CONSTRAINT UQ_Usuario_Login UNIQUE (Login_Usuario)
) ENGINE = InnoDB;

INSERT INTO Usuario (Nombre_Usuario, Login_Usuario, Contrasena_Usuario) VALUES
    ('Administrador',  'admin', SHA2('admin123', 256)),
    ('Luis Hernández', 'luis',  SHA2('luis123', 256));

SELECT ID_Usuario, Nombre_Usuario, Login_Usuario FROM Usuario;


-- ---------------------------------------------------------------------
-- 2. Conectar Usuario con todas las tablas
--    Cada tabla guarda el ID del usuario final que hizo el último
--    movimiento (alta o cambio) en ese registro.
-- ---------------------------------------------------------------------
ALTER TABLE Estado
    ADD COLUMN ID_Usuario INT NULL,
    ADD CONSTRAINT FK_Estado_Usuario
        FOREIGN KEY (ID_Usuario) REFERENCES Usuario (ID_Usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE Ciudad
    ADD COLUMN ID_Usuario INT NULL,
    ADD CONSTRAINT FK_Ciudad_Usuario
        FOREIGN KEY (ID_Usuario) REFERENCES Usuario (ID_Usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE Equipo
    ADD COLUMN ID_Usuario INT NULL,
    ADD CONSTRAINT FK_Equipo_Usuario
        FOREIGN KEY (ID_Usuario) REFERENCES Usuario (ID_Usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE Jugador
    ADD COLUMN ID_Usuario INT NULL,
    ADD CONSTRAINT FK_Jugador_Usuario
        FOREIGN KEY (ID_Usuario) REFERENCES Usuario (ID_Usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE Manager
    ADD COLUMN ID_Usuario INT NULL,
    ADD CONSTRAINT FK_Manager_Usuario
        FOREIGN KEY (ID_Usuario) REFERENCES Usuario (ID_Usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE Posicion
    ADD COLUMN ID_Usuario INT NULL,
    ADD CONSTRAINT FK_Posicion_Usuario
        FOREIGN KEY (ID_Usuario) REFERENCES Usuario (ID_Usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE Estadio
    ADD COLUMN ID_Usuario INT NULL,
    ADD CONSTRAINT FK_Estadio_Usuario
        FOREIGN KEY (ID_Usuario) REFERENCES Usuario (ID_Usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE Temporada
    ADD COLUMN ID_Usuario INT NULL,
    ADD CONSTRAINT FK_Temporada_Usuario
        FOREIGN KEY (ID_Usuario) REFERENCES Usuario (ID_Usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE Partido
    ADD COLUMN ID_Usuario INT NULL,
    ADD CONSTRAINT FK_Partido_Usuario
        FOREIGN KEY (ID_Usuario) REFERENCES Usuario (ID_Usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE Alineacion
    ADD COLUMN ID_Usuario INT NULL,
    ADD CONSTRAINT FK_Alineacion_Usuario
        FOREIGN KEY (ID_Usuario) REFERENCES Usuario (ID_Usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE Carrera
    ADD COLUMN ID_Usuario INT NULL,
    ADD CONSTRAINT FK_Carrera_Usuario
        FOREIGN KEY (ID_Usuario) REFERENCES Usuario (ID_Usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE Patrocinador
    ADD COLUMN ID_Usuario INT NULL,
    ADD CONSTRAINT FK_Patrocinador_Usuario
        FOREIGN KEY (ID_Usuario) REFERENCES Usuario (ID_Usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE Contrato
    ADD COLUMN ID_Usuario INT NULL,
    ADD CONSTRAINT FK_Contrato_Usuario
        FOREIGN KEY (ID_Usuario) REFERENCES Usuario (ID_Usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE;


-- Los registros que ya existían quedan a nombre del Administrador
UPDATE Estado       SET ID_Usuario = 1 WHERE ID_Usuario IS NULL;
UPDATE Posicion     SET ID_Usuario = 1 WHERE ID_Usuario IS NULL;
UPDATE Patrocinador SET ID_Usuario = 1 WHERE ID_Usuario IS NULL;


-- ---------------------------------------------------------------------
-- 3. Usuario de MySQL que usa la aplicación por dentro para conectarse.
--    Los usuarios finales NO son usuarios de MySQL: la aplicación
--    entra con APP_LIGA y revisa el login contra la tabla Usuario.
-- ---------------------------------------------------------------------
CREATE USER 'APP_LIGA'@'localhost' IDENTIFIED BY 'Liga2026';

GRANT SELECT, INSERT, UPDATE, DELETE
    ON BD_LIGA_BASEBALL.*
    TO 'APP_LIGA'@'localhost';

FLUSH PRIVILEGES;

SHOW GRANTS FOR 'APP_LIGA'@'localhost';


-- ---------------------------------------------------------------------
-- 4. Comprobación: quién registró cada estado
-- ---------------------------------------------------------------------
SELECT e.ID_Estado, e.Nombre_Estado, u.Login_Usuario AS Registro
FROM Estado e
LEFT JOIN Usuario u ON u.ID_Usuario = e.ID_Usuario;
