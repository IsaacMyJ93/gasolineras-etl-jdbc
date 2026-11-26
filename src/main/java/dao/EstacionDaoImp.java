package dao;

import com.ci_cd.activity.ExcelCleaner;
import database.DBconnection;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EstacionDaoImp {

    private static final int FILA_CABECERA = 0;
    private static final int FILA_INICIO_DATOS = 1;
    private static final int BATCH = 1000;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String SQL_ESTACION =
            "INSERT INTO estacion (tipo, localidad_id, codigo_postal, direccion, margen, latitud, longitud, "
                    + "fecha_actualizacion, horario, empresa_id, tipo_venta, remision, tipo_servicio) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
    private static final String SQL_PRECIO =
            "INSERT INTO precio (estacion_id, combustible_id, valor) VALUES (?,?,?)";

    private final Connection connection;

    public EstacionDaoImp() {
        connection = DBconnection.getConnection();
    }

    /** Vacía precio y estacion para poder relanzar la carga sin duplicados. */
    public void vaciar() {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("DELETE FROM precio");
            st.executeUpdate("DELETE FROM estacion");
            st.executeUpdate("ALTER TABLE estacion AUTO_INCREMENT = 1");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Carga un fichero. tipo = "TERRESTRE" o "MARITIMA".
     * Devuelve {estaciones, precios} insertados.
     */
    public int[] importarExcel(String ruta, String tipo,
                               Map<String, Integer> idsLoc,
                               Map<String, Integer> idsEmp,
                               Map<String, Integer> idsComb) {
        int estaciones = 0, precios = 0, pendientes = 0;

        try (FileInputStream fis = new FileInputStream(ruta);
             Workbook workbook = new XSSFWorkbook(fis);
             PreparedStatement psEst = connection.prepareStatement(SQL_ESTACION, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psPre = connection.prepareStatement(SQL_PRECIO)) {

            connection.setAutoCommit(false);

            Sheet sheet = workbook.getSheetAt(0);

            // cabecera -> índice
            Map<String, Integer> col = new HashMap<>();
            List<int[]> colsPrecio = new ArrayList<>();   // {indiceColumna, combustibleId}
            for (Cell cell : sheet.getRow(FILA_CABECERA)) {
                if (cell.getCellType() != CellType.STRING) continue;
                String cab = cell.getStringCellValue().trim();
                col.put(cab, cell.getColumnIndex());
                String comb = CombustibleDaoImp.nombreCombustible(cab);
                if (comb != null) {
                    Integer combId = idsComb.get(comb);
                    if (combId == null) throw new IllegalStateException("Combustible sin id: " + comb);
                    colsPrecio.add(new int[]{cell.getColumnIndex(), combId});
                }
            }

            for (int i = FILA_INICIO_DATOS; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String prov = txt(row, col, "Provincia");
                String mun = txt(row, col, "Municipio");
                String loc = txt(row, col, "Localidad");
                if (prov == null || mun == null || loc == null) continue;   // fila vacía

                Integer locId = idsLoc.get(prov.toUpperCase() + "|" + mun + "|" + loc);
                if (locId == null) {
                    throw new IllegalStateException("Localidad sin id: " + prov + " / " + mun + " / " + loc);
                }

                String rotulo = txt(row, col, "Rótulo");
                Integer empId = rotulo == null ? null : idsEmp.get(rotulo.toUpperCase());

                psEst.setString(1, tipo);
                psEst.setInt(2, locId);
                setStr(psEst, 3, txt(row, col, "Código postal"));
                setStr(psEst, 4, txt(row, col, "Dirección"));
                setStr(psEst, 5, txt(row, col, "Margen"));              // marítimas: NULL
                setDec(psEst, 6, dec(txt(row, col, "Latitud")));        // 6 marítimas sin coordenadas
                setDec(psEst, 7, dec(txt(row, col, "Longitud")));
                String fecha = txt(row, col, "Toma de datos");          // marítimas: NULL
                if (fecha == null) {
                    psEst.setNull(8, Types.TIMESTAMP);
                } else {
                    psEst.setTimestamp(8, Timestamp.valueOf(LocalDateTime.parse(fecha, FECHA)));
                }
                setStr(psEst, 9, txt(row, col, "Horario"));
                if (empId == null) psEst.setNull(10, Types.INTEGER); else psEst.setInt(10, empId);
                setStr(psEst, 11, txt(row, col, "Tipo venta"));
                setStr(psEst, 12, txt(row, col, "Rem."));
                setStr(psEst, 13, txt(row, col, "Tipo servicio"));      // marítimas: NULL
                psEst.executeUpdate();

                int estId;
                try (ResultSet keys = psEst.getGeneratedKeys()) {
                    keys.next();
                    estId = keys.getInt(1);
                }
                estaciones++;

                for (int[] cp : colsPrecio) {
                    Cell cell = row.getCell(cp[0]);
                    String v = cell == null ? null : ExcelCleaner.getCleanString(row, cp[0]);
                    if (v == null || v.isBlank()) continue;
                    psPre.setInt(1, estId);
                    psPre.setInt(2, cp[1]);
                    psPre.setBigDecimal(3, dec(v));
                    psPre.addBatch();
                    precios++;
                    if (++pendientes % BATCH == 0) psPre.executeBatch();
                }
            }
            psPre.executeBatch();   // último lote
            connection.commit();

        } catch (IOException | SQLException | RuntimeException e) {
            try { connection.rollback(); } catch (SQLException ignored) { }
            throw new RuntimeException("Error cargando " + ruta + ": " + e.getMessage(), e);
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) { }
        }

        System.out.println(ruta + " -> estaciones: " + estaciones + ", precios: " + precios);
        return new int[]{estaciones, precios};
    }

    /** Texto limpio de la columna por nombre de cabecera; null si no existe o está vacía. */
    private String txt(Row row, Map<String, Integer> col, String cabecera) {
        Integer idx = col.get(cabecera);
        if (idx == null) return null;
        String s = ExcelCleaner.getCleanString(row, idx);
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private BigDecimal dec(String s) {
        if (s == null || s.isBlank()) return null;
        return new BigDecimal(s.trim().replace(",", "."));
    }

    private void setStr(PreparedStatement ps, int i, String s) throws SQLException {
        if (s == null) ps.setNull(i, Types.VARCHAR); else ps.setString(i, s);
    }

    private void setDec(PreparedStatement ps, int i, BigDecimal d) throws SQLException {
        if (d == null) ps.setNull(i, Types.DOUBLE); else ps.setDouble(i, d.doubleValue());
    }
}