package com.MedicoService.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
public class EncodingConfig {

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void init() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SET client_encoding TO 'UTF8'");
            System.out.println("✅ client_encoding forzado a UTF8 para la sesión JDBC");
        } catch (SQLException e) {
            System.err.println("⚠️ No se pudo establecer client_encoding UTF8: " + e.getMessage());
        }
    }
}