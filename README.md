# ⛽ Gasolineras España — Pipeline ETL Relacional en Java & MariaDB

![Java](https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?logo=apachemaven)
![MariaDB](https://img.shields.io/badge/MariaDB-3NF-003545?logo=mariadb)
![Apache POI](https://img.shields.io/badge/Apache%20POI-XLSX-green)

Sistema de extracción, transformación y carga (**ETL**) desarrollado en **Java** con **JDBC** y **Apache POI** para procesar los conjuntos de datos oficiales de precios de carburantes del Ministerio para la Transición Ecológica y el Reto Demográfico (`preciosEESS_es.xlsx` y `embarcacionesPrecios_es.xlsx`) sobre una base de datos relacional normalizada en **MariaDB**.

---

## 🏗️ Arquitectura y Tecnologías

* **Lenguaje:** Java (JDK 17+)
* **Gestión de dependencias:** Maven (`pom.xml`)
* **Lectura de datos:** Apache POI (`XSSFWorkbook`)
* **Persistencia:** JDBC nativo con `PreparedStatement`, transacciones por lote y recuperación de claves autogeneradas (`RETURN_GENERATED_KEYS`)
* **Motor de Base de Datos:** MariaDB (`utf8mb4_unicode_ci`)
* **Patrones de diseño:**
  * **DAO (Data Access Object):** Separación limpia de responsabilidades por entidad (`ProvinciaDAO`, `MunicipioDAO`, `LocalidadDAO`, `EmpresaDAO`, `CombustibleDAO`, `EstacionDAO`, `PrecioDAO`).
  * **Contract Interface (`SchemeDB`):** Centralización de nombres de tablas, columnas y claves foráneas para evitar literales mágicos en las consultas SQL.

---

## 🗄️ Modelo Entidad-Relación (3FN)

El esquema está normalizado en **Tercera Forma Normal (3FN)** para eliminar redundancias geográficas y resolver la relación muchos a muchos (`N:M`) entre estaciones de servicio y tipos de carburante:

* **`provincia` → `municipio` → `localidad`:** Jerarquía territorial con restricciones de unicidad compuesta (`UNIQUE(provincia_id, nombre)` y `UNIQUE(municipio_id, nombre)`).
* **`empresa`:** Catálogo único de rótulos/operadores (`UNIQUE(nombre)`).
* **`combustible`:** Catálogo normalizado de carburantes disponibles (Gasolina 95 E5, Gasóleo A, AdBlue, etc.).
* **`estacion`:** Entidad central que unifica estaciones **terrestres** (`TERRESTRE`) y postes **marítimos** (`MARITIMA`), tolerando estaciones portuarias sin operador asignado (`empresa_id NULL`).
* **`precio`:** Tabla asociativa (`PK(estacion_id, combustible_id)`) con precisión `DECIMAL(8,3)` para soportar tanto precios minoristas por litro como suministros marítimos por cada 1.000 litros.

### Optimizaciones de Rendimiento
* **Deduplicación en memoria (`HashSet` / `HashMap`):** Filtrado previo en Java durante el barrido de los ficheros Excel para minimizar el tráfico de red hacia el servidor SQL.
* **Índices compuestos analíticos:**
  * `idx_estacion_tipo_empresa` sobre `(tipo, empresa_id)` para conteos y rankings por operador y modalidad.
  * `idx_precio_comb` sobre `(combustible_id, valor)` para consultas rápidas de precios mínimos, máximos y medias por carburante.

---

## 📂 Estructura del Proyecto

```text
Gasolineras_bd_relacional/
├── src/
│   ├── datos/               # Ficheros fuente .xlsx del Ministerio
│   └── main/java/
│       ├── database/        # Gestión de conexión JDBC y contrato SchemeDB
│       ├── dao/             # Implementaciones DAO por entidad
│       ├── model/           # Clases POJO del dominio
│       └── Main.java        # Orquestador del flujo ETL
├── .gitignore
├── pom.xml
└── README.md
