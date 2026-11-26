package dao;

import database.DBconnection;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
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

public class CombustibleDaoImp {

    private final Connection connection;

    public CombustibleDaoImp() {
        connection = DBconnection.getConnection();
    }

    /** "Precio  amoniaco\t" -> "amoniaco" ; null si la cabecera no es de precio. */
    public static String nombreCombustible(String cabecera) {
        if (cabecera == null || !cabecera.trim().startsWith("Precio")) return null;
        return cabecera.trim().substring("Precio".length()).trim().replaceAll("\\s+", " ");
    }

    public void importarExcel(String... rutas) {
        Set<String> unicos = new LinkedHashSet<>();

        for (String ruta : rutas) {
            try (FileInputStream fis = new FileInputStream(ruta);
                 Workbook workbook = new XSSFWorkbook(fis)) {

                Sheet sheet = workbook.getSheetAt(0);
                Row cabecera = sheet.getRow(0);
                for (Cell cell : cabecera) {
                    if (cell.getCellType() != CellType.STRING) continue;
                    String nombre = nombreCombustible(cell.getStringCellValue());
                    if (nombre != null && !nombre.isBlank()) unicos.add(nombre);
                }
            } catch (IOException e) {
                throw new RuntimeException("Error al leer " + ruta + ": " + e.getMessage(), e);
            }
        }

        int insertados = 0;
        for (String nombre : unicos) {
            if (insertar(nombre)) insertados++;
        }
        System.out.println("Combustibles leídos: " + unicos.size() + ", nuevos insertados: " + insertados);
    }

    public boolean insertar(String nombre) {
        String sql = "INSERT IGNORE INTO combustible (nombre) VALUES (?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombre);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Integer> obtenerMapaIds() {
        Map<String, Integer> mapa = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT id, nombre FROM combustible");
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