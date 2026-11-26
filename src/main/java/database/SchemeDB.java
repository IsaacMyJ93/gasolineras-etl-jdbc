package database;

import java.time.LocalDateTime;

public interface SchemeDB {

    String DB_NAME = "gasolinera_db";

    // Registros comunes
    String COL_NAME = "nombre";

    // Tabla provincia
    String TAB_PRO = "provincia";

    // Tabla municipio
    String TAB_MUN = "municipio";

    // Tabla localidad
    String TAB_LOC = "localidad";

    // Tabla estacion
    String TAB_EST = "estacion";
    String COL_TIPO = "tipo";
    String COL_LOCALIDAD_ID = "localidad_id";
    String COL_EMPRESA_ID = "empresa_id";
    String COL_CODIGO_POSTAL = "codigo_postal";
    String COL_DIRECCION = "direccion";
    String COL_MARGEN = "margen";
    String COL_LATITUD = "latitud";
    String COL_LONGITUD = "longitud";
    String COL_FECHA_ACTUALIZACION = "fecha_actualizacion";
    String COL_HORARIO = "horario";
    String COL_TIPO_VENTA = "tipo_venta";
    String COL_REMISION = "remision";
    String COL_TIPO_SERVICIO = "tipo_servicio";

    // Tabla empresa
    String TAB_EMP = "empresa";

    // Tabla combustible
    String TAB_COM = "combustible";

    // Tabla precio
    String TAB_PRE = "precio";
    String COL_VALOR = "valor";
    String COL_ESTACION_ID = "estacion_id";
    String COL_COMBUSTIBLE_ID = "combustible_id";

}
