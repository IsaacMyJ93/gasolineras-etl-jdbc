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

public class LocalidadDaoImp {

    private Connection connection;

    private PreparedStatement ps;

    public LocalidadDaoImp() {
        connection = DBconnection.getConnection();
    }

    private static final int FILA_INICIO_DATOS = 1;
    private static final int COL_PROVINCIA = 0;
    private static final int COL_MUNICIPIO = 1;
    private static final int COL_LOCALIDAD = 2;

    /** Lee los Excel y carga cada terna (provincia, municipio, localidad) una sola vez. */
    public void importarExcel(Map<String, Integer> idsMunicipio, String... rutas) {
        // Clave "municipio|localidadad" para que el Set distinga homónimos de municipios distintos
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
                    String loc = ExcelCleaner.getCleanString(row, COL_LOCALIDAD);
                    if (prov == null || prov.isBlank() || mun == null || mun.isBlank()
                            || loc == null || loc.isBlank())
                        continue;

                    unicos.add(prov.trim().toUpperCase() + "|" + mun.trim() + "|" + loc.trim());               }
            } catch (IOException e) {
                throw new RuntimeException("Error al leer " + ruta + ": " + e.getMessage(), e);
            }
        }

        int insertados = 0;
        for (String clave : unicos) {
            String[] partes = clave.split("\\|", 3);                         // 3 partes
            Integer munId = idsMunicipio.get(partes[0] + "|" + partes[1]);   // "PROVINCIA|municipio"
            if (munId == null) {
                System.out.println("Municipio sin id, se omite: " + partes[0] + " / " + partes[1]);
                continue;
            }
            if (insertar(partes[2], munId)) insertados++;                    // la localidad es partes[2]
        }
        System.out.println("Localidades leídas: " + unicos.size() + ", nuevas insertadas: " + insertados);
    }

    public boolean insertar(String nombre, int municipioId) {
        String sql = "INSERT IGNORE INTO localidad (nombre, municipio_id) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setInt(2, municipioId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Integer> obtenerMapaIds() {
        Map<String, Integer> mapa = new HashMap<>();
        String sql = "SELECT l.id, l.nombre AS loc, m.nombre AS mun, p.nombre AS prov "
                + "FROM localidad l "
                + "JOIN municipio m ON m.id = l.municipio_id "
                + "JOIN provincia p ON p.id = m.provincia_id";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                mapa.put(rs.getString("prov") + "|" + rs.getString("mun") + "|" + rs.getString("loc"),
                        rs.getInt("id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return mapa;
    }

}
