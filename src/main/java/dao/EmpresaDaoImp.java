package dao;

import com.ci_cd.activity.ExcelCleaner;
import database.DBconnection;
import org.apache.poi.ss.usermodel.Cell;
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

public class EmpresaDaoImp {

    private static final int FILA_CABECERA = 0;
    private static final int FILA_INICIO_DATOS = 1;
    private static final String CABECERA_ROTULO = "Rótulo";

    private final Connection connection;

    public EmpresaDaoImp() {
        connection = DBconnection.getConnection();
    }

    /** Índice de la columna cuya cabecera coincide con el nombre dado. */
    private int indiceColumna(Sheet sheet, String nombre) {
        Row cabecera = sheet.getRow(FILA_CABECERA);
        for (Cell cell : cabecera) {
            if (cell.getColumnIndex() >= 0
                    && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING
                    && cell.getStringCellValue().trim().equals(nombre)) {
                return cell.getColumnIndex();
            }
        }
        throw new IllegalStateException("No se encuentra la columna '" + nombre + "'");
    }

    /** Lee los Excel y carga cada empresa (rótulo) una sola vez. */
    public void importarExcel(String... rutas) {
        Set<String> unicas = new LinkedHashSet<>();

        for (String ruta : rutas) {
            try (FileInputStream fis = new FileInputStream(ruta);
                 Workbook workbook = new XSSFWorkbook(fis)) {

                Sheet sheet = workbook.getSheetAt(0);
                int colRotulo = indiceColumna(sheet, CABECERA_ROTULO);

                for (int i = FILA_INICIO_DATOS; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    String nombre = ExcelCleaner.getCleanString(row, colRotulo);
                    if (nombre == null || nombre.isBlank()) continue;   // 5 marítimas sin rótulo

                    unicas.add(nombre.trim().toUpperCase());
                }
            } catch (IOException e) {
                throw new RuntimeException("Error al leer " + ruta + ": " + e.getMessage(), e);
            }
        }

        int insertadas = 0;
        for (String nombre : unicas) {
            if (insertar(nombre)) insertadas++;
        }
        System.out.println("Empresas leídas: " + unicas.size() + ", nuevas insertadas: " + insertadas);
    }

    public boolean insertar(String nombre) {
        String sql = "INSERT IGNORE INTO empresa (nombre) VALUES (?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombre);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Mapa nombre -> id, para usarlo al insertar estaciones. */
    public Map<String, Integer> obtenerMapaIds() {
        Map<String, Integer> mapa = new HashMap<>();
        String sql = "SELECT id, nombre FROM empresa";
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