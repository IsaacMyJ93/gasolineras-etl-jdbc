package dao;

import com.ci_cd.activity.ExcelCleaner;
import database.DBconnection;
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

public class MunicipioDaoImp {

    private Connection connection;

    private PreparedStatement preparedStatement;

    public MunicipioDaoImp() {
        connection = DBconnection.getConnection();
    }

        private static final int FILA_INICIO_DATOS = 1;
        private static final int COL_PROVINCIA = 0;
        private static final int COL_MUNICIPIO = 1;   // índice 1 en ambos ficheros

        /** Lee los Excel y carga cada pareja (provincia, municipio) una sola vez. */
        public void importarExcel(Map<String, Integer> idsProvincia, String... rutas) {
            // Clave "provincia|municipio" para que el Set distinga homónimos de provincias distintas
            Set<String> unicos = new LinkedHashSet<>();

            for (String ruta : rutas) {
                try (FileInputStream fis = new FileInputStream(ruta);
                     Workbook workbook = new XSSFWorkbook(fis)) {

                    Sheet sheet = workbook.getSheetAt(0);
                    for (int i = FILA_INICIO_DATOS; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row == null) continue;

                        String prov = ExcelCleaner.getCleanString(row, COL_PROVINCIA);
                        String mun = ExcelCleaner.getCleanString(row, COL_MUNICIPIO);
                        if (prov == null || prov.isBlank() || mun == null || mun.isBlank()) continue;

                        unicos.add(prov.trim().toUpperCase() + "|" + mun.trim());
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error al leer " + ruta + ": " + e.getMessage(), e);
                }
            }

            int insertados = 0;
            for (String clave : unicos) {
                String[] partes = clave.split("\\|", 2);
                Integer provId = idsProvincia.get(partes[0]);
                if (provId == null) {
                    System.out.println("Provincia sin id, se omite: " + partes[0]);
                    continue;
                }
                if (insertar(partes[1], provId)) insertados++;
            }
            System.out.println("Municipios leídos: " + unicos.size() + ", nuevos insertados: " + insertados);
        }

        public boolean insertar(String nombre, int provinciaId) {
            String sql = "INSERT IGNORE INTO municipio (nombre, provincia_id) VALUES (?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, nombre);
                ps.setInt(2, provinciaId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public Map<String, Integer> obtenerMapaIds() {
            Map<String, Integer> mapa = new HashMap<>();
            String sql = "SELECT m.id, m.nombre AS mun, p.nombre AS prov "
                    + "FROM municipio m JOIN provincia p ON p.id = m.provincia_id";
            try (PreparedStatement ps = connection.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mapa.put(rs.getString("prov") + "|" + rs.getString("mun"), rs.getInt("id"));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return mapa;
        }

}
