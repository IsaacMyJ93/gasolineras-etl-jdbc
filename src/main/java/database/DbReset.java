package database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DbReset {

    // Orden: primero las tablas hijas, por claridad (con FK desactivadas da igual)
    private static final String[] TABLAS = {
            "precio", "estacion", "localidad", "municipio",
            "empresa", "combustible", "provincia"
    };

    public static void vaciarTodo() {
        Connection c = DBconnection.getConnection();
        try (Statement st = c.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 0");
            for (String t : TABLAS) {
                st.execute("TRUNCATE TABLE " + t);   // vacía y reinicia AUTO_INCREMENT a 1
            }
            st.execute("SET FOREIGN_KEY_CHECKS = 1");
            System.out.println("Base de datos vaciada.");
        } catch (SQLException e) {
            throw new RuntimeException("Error al vaciar la BD", e);
        }
    }
}