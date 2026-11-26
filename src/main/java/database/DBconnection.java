package database;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;


public class DBconnection {

    private static Connection connection;

    public static Connection getConnection() {
        if (connection == null) {
            createConnection();
        }
        return connection;
    }

    private static void createConnection() {
        Properties properties = new Properties();
        try {
            FileInputStream fileInputStream = new FileInputStream("src/main/resources/database.properties");
            if (fileInputStream == null) {
                System.out.println("No puede leer fichero");
            }

            properties.load(fileInputStream);

            String user = properties.getProperty("user");
            String password = properties.getProperty("password");
            String url = properties.getProperty("url");

            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Conexion establecida!");
        } catch (FileNotFoundException e) {
            System.out.println("Error al leer el fichero .properties" + e.getCause());
        } catch (IOException e) {
            System.out.println("No tienes permisos para acceder al fichero."+ e.getCause());
        } catch (SQLException e) {
            System.out.println("Error en la sentencia SQL " + e.getCause());
        }

    }

}
