package dao;

import com.ci_cd.activity.ExcelCleaner;
import database.DBconnection;
import database.SchemeDB;
import model.Provincia;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ProvinciaDaoImp implements GenericoDAO<Provincia> {

    private Connection connection;

    private PreparedStatement preparedStatement;

    public ProvinciaDaoImp() {
        connection = DBconnection.getConnection();
    }

        private static final int FILA_INICIO_DATOS = 1;   // filas sin cabecera
        private static final int COL_PROVINCIA = 0;       // es la columna 0 en ambos ficheros

        public void importarExcel(String... rutas) {
            Set<String> unicas = new LinkedHashSet<>();   // el Set descarta repetidas

            for (String ruta : rutas) {
                try (FileInputStream fis = new FileInputStream(ruta);
                     Workbook workbook = new XSSFWorkbook(fis)) {

                    Sheet sheet = workbook.getSheetAt(0);
                    for (int i = FILA_INICIO_DATOS; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row == null) continue;

                        String nombre = ExcelCleaner.getCleanString(row, COL_PROVINCIA);
                        if (nombre == null || nombre.isBlank()) continue;

                        unicas.add(nombre.trim().toUpperCase());
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error al leer " + ruta + ": " + e.getMessage(), e);
                }
            }

            for (String nombre : unicas) {
                Provincia p = new Provincia();
                p.setNombre(nombre);
                insertar(p);
            }
            System.out.println("Provincias cargadas: " + unicas.size());   // debe salir 52
        }

        @Override
        public boolean insertar(Provincia dato) {
                String sql = String.format("INSERT IGNORE INTO %s (%s) VALUES (?)",
                        SchemeDB.TAB_PRO, SchemeDB.COL_NAME);

                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, dato.getNombre());
                    return ps.executeUpdate() > 0;   // 1 si insertó, 0 si ya existía
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

        }

    public Map<String, Integer> obtenerMapaIds() {
        Map<String, Integer> mapa = new HashMap<>();
        String sql = "SELECT id, nombre FROM " + SchemeDB.TAB_PRO;
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                mapa.put(rs.getString("nombre"), rs.getInt("id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return mapa;
    }
}

