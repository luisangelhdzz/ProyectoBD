-- =====================================================================
--  LIGA DE BASEBALL MEXICANA - USUARIOS FINALES Y AUDITORIA
--  Luis Angel Eduardo Hernández Hernández - 24051002 - Grupo 3A4
--
--  Ejecutar como root:   mysql -u root -p < usuarios_finales.sql
--  Se puede ejecutar varias veces: lo que ya existe no se vuelve a crear.
-- =====================================================================

USE BD_LIGA_BASEBALL;
SET NAMES utf8mb4;   -- para que los acentos se guarden bien


-- ---------------------------------------------------------------------
-- 1. Tabla de usuarios finales (los que entran a la aplicación)
--    La contraseña se guarda cifrada con SHA2 (64 caracteres).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Usuario (
    ID_Usuario          INT          NOT NULL AUTO_INCREMENT,
    Nombre_Usuario      VARCHAR(45)  NOT NULL,
    Login_Usuario       VARCHAR(20)  NOT NULL,
    Contrasena_Usuario  CHAR(64)     NOT NULL,
    CONSTRAINT PK_Usuario PRIMARY KEY (ID_Usuario),
    CONSTRAINT UQ_Usuario_Login UNIQUE (Login_Usuario)
) ENGINE = InnoDB;

-- INSERT IGNORE: si el login ya existe no marca error, solo lo salta
INSERT IGNORE INTO Usuario (Nombre_Usuario, Login_Usuario, Contrasena_Usuario) VALUES
    ('Administrador',  'admin', SHA2('admin123', 256)),
    ('Luis Hernández', 'luis',  SHA2('luis123', 256));

SELECT ID_Usuario, Nombre_Usuario, Login_Usuario FROM Usuario;


-- ---------------------------------------------------------------------
-- 2. Conectar Usuario con todas las tablas
--    Cada tabla guarda el ID del usuario final que hizo el último
--    movimiento (alta o cambio) en ese registro.
--
--    MySQL no tiene "ADD COLUMN IF NOT EXISTS", así que este
--    procedimiento revisa primero si la columna ya existe y solo la
--    agrega (con su llave foránea) cuando falta.
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS agregar_id_usuario;

DELIMITER //
CREATE PROCEDURE agregar_id_usuario(IN tabla VARCHAR(64))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND LOWER(TABLE_NAME) = LOWER(tabla)
                     AND COLUMN_NAME = 'ID_Usuario') THEN
        SET @sql = CONCAT('ALTER TABLE ', tabla,
            ' ADD COLUMN ID_Usuario INT NULL,',
            ' ADD CONSTRAINT FK_', tabla, '_Usuario',
            ' FOREIGN KEY (ID_Usuario) REFERENCES Usuario (ID_Usuario)',
            ' ON DELETE RESTRICT ON UPDATE CASCADE');
        PREPARE sentencia FROM @sql;
        EXECUTE sentencia;
        DEALLOCATE PREPARE sentencia;
    END IF;
END //
DELIMITER ;

CALL agregar_id_usuario('Estado');
CALL agregar_id_usuario('Ciudad');
CALL agregar_id_usuario('Equipo');
CALL agregar_id_usuario('Jugador');
CALL agregar_id_usuario('Manager');
CALL agregar_id_usuario('Posicion');
CALL agregar_id_usuario('Estadio');
CALL agregar_id_usuario('Temporada');
CALL agregar_id_usuario('Partido');
CALL agregar_id_usuario('Alineacion');
CALL agregar_id_usuario('Carrera');
CALL agregar_id_usuario('Patrocinador');
CALL agregar_id_usuario('Contrato');

DROP PROCEDURE agregar_id_usuario;


-- Los registros que ya existían quedan a nombre del Administrador
UPDATE Estado       SET ID_Usuario = 1 WHERE ID_Usuario IS NULL;
UPDATE Posicion     SET ID_Usuario = 1 WHERE ID_Usuario IS NULL;
UPDATE Patrocinador SET ID_Usuario = 1 WHERE ID_Usuario IS NULL;


-- ---------------------------------------------------------------------
-- 3. Bitácora: un renglón por cada movimiento (alta, cambio o baja).
--    Hace falta para las bajas: al eliminar un registro también se
--    borra su ID_Usuario, pero aquí queda quién lo eliminó y cuándo.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Bitacora (
    ID_Bitacora  INT          NOT NULL AUTO_INCREMENT,
    ID_Usuario   INT          NOT NULL,
    Tabla        VARCHAR(30)  NOT NULL,
    Accion       VARCHAR(10)  NOT NULL,
    Detalle      VARCHAR(100) NULL,
    Fecha        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_Bitacora PRIMARY KEY (ID_Bitacora),
    CONSTRAINT FK_Bitacora_Usuario
        FOREIGN KEY (ID_Usuario) REFERENCES Usuario (ID_Usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB;


-- ---------------------------------------------------------------------
-- 4. Usuario de MySQL que usa la aplicación por dentro para conectarse.
--    Los usuarios finales NO son usuarios de MySQL: la aplicación
--    entra con APP_LIGA y revisa el login contra la tabla Usuario.
-- ---------------------------------------------------------------------
CREATE USER IF NOT EXISTS 'APP_LIGA'@'localhost' IDENTIFIED BY 'Liga2026';

GRANT SELECT, INSERT, UPDATE, DELETE
    ON BD_LIGA_BASEBALL.*
    TO 'APP_LIGA'@'localhost';

FLUSH PRIVILEGES;

SHOW GRANTS FOR 'APP_LIGA'@'localhost';


-- ---------------------------------------------------------------------
-- 5. Comprobación
-- ---------------------------------------------------------------------
-- Quién registró o cambió cada estado
SELECT e.ID_Estado, e.Nombre_Estado, u.Login_Usuario AS Registro
FROM Estado e
LEFT JOIN Usuario u ON u.ID_Usuario = e.ID_Usuario;

-- Últimos movimientos (incluye los eliminados)
SELECT b.Fecha, u.Login_Usuario AS Usuario, b.Tabla, b.Accion, b.Detalle
FROM Bitacora b
JOIN Usuario u ON u.ID_Usuario = b.ID_Usuario
ORDER BY b.Fecha DESC
LIMIT 20;
