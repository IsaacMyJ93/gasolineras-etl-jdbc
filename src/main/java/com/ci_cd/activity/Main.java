package com.ci_cd.activity;

import dao.*;
import database.DbReset;

import java.util.Map;


public class Main {

    private static final String RUTA_EESS = "src/datos/preciosEESS_es.xlsx";
    private static final String RUTA_MAR  = "src/datos/embarcacionesPrecios_es.xlsx";

    public static void main(String[] args){

        // Resetear DB
      //  DbReset.vaciarTodo();

        // 1. Provincias
        ProvinciaDaoImp provDao = new ProvinciaDaoImp();
        provDao.importarExcel(RUTA_EESS, RUTA_MAR);
        Map<String, Integer> idsProv = provDao.obtenerMapaIds();

        // 2. Municipios (necesitan los ids de provincia)
        MunicipioDaoImp munDao = new MunicipioDaoImp();
        munDao.importarExcel(idsProv, RUTA_EESS, RUTA_MAR);
        Map<String, Integer> idsMun = munDao.obtenerMapaIds();   // se lee DESPUÉS de cargar

        // 3. Localidades
        LocalidadDaoImp locDao = new LocalidadDaoImp();
        locDao.importarExcel(idsMun, RUTA_EESS, RUTA_MAR);
        Map<String, Integer> idsLoc = locDao.obtenerMapaIds();   // el método que te pasé

        // 4. Empresas
        EmpresaDaoImp empDao = new EmpresaDaoImp();
        empDao.importarExcel(RUTA_EESS, RUTA_MAR);
        Map<String, Integer> idsEmp = empDao.obtenerMapaIds();

        // 5. Combustibles
        CombustibleDaoImp combDao = new CombustibleDaoImp();
        combDao.importarExcel(RUTA_EESS, RUTA_MAR);
        Map<String, Integer> idsComb = combDao.obtenerMapaIds();

        // 6. Estaciones y precios
        EstacionDaoImp estDao = new EstacionDaoImp();
        estDao.vaciar();   // permite relanzar sin duplicar
        int[] t = estDao.importarExcel(RUTA_EESS, "TERRESTRE", idsLoc, idsEmp, idsComb);
        int[] m = estDao.importarExcel(RUTA_MAR, "MARITIMA", idsLoc, idsEmp, idsComb);
        System.out.println("TOTAL estaciones: " + (t[0] + m[0]) + ", precios: " + (t[1] + m[1]));

    }
}

