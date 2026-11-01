package com.pelomacio.integracao;

import java.sql.Connection;
import java.sql.DriverManager;

/** Condição dos testes de integração: só rodam se houver um PostgreSQL acessível (no CI, sempre). */
public final class Banco {

    private Banco() {}

    public static boolean disponivel() {
        if (System.getenv("CI") != null) return true; // no CI, banco ausente deve falhar o build, não pular
        String url = System.getenv().getOrDefault("TEST_DB_URL", "jdbc:postgresql://localhost:5433/pelo_macio_test");
        String user = System.getenv().getOrDefault("TEST_DB_USER", "postgres");
        String senha = System.getenv().getOrDefault("TEST_DB_PASSWORD", "postgres");
        try (Connection c = DriverManager.getConnection(url, user, senha)) {
            return c.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }
}
