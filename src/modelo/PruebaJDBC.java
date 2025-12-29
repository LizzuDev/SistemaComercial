package modelo;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PruebaJDBC {
    
    // Configuración centralizada
    private static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=BD_Comercial;encrypt=true;trustServerCertificate=true;";
    private static final String USUARIO = "SuperAdmin";
    private static final String CLAVE = "alejomenso";

    public static Connection getConexion() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, CLAVE);
    }
}
