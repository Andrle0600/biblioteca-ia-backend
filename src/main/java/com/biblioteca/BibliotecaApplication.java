package com.biblioteca;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BibliotecaApplication {

	public static void main(String[] args) {
		cargarEnvLocal();
		SpringApplication.run(BibliotecaApplication.class, args);
	}

	/**
	 * Carga el archivo .env de la raiz del proyecto (si existe) como propiedades
	 * del sistema, para no tener que configurar variables de entorno a mano en
	 * desarrollo. Las variables de entorno reales tienen prioridad sobre el archivo.
	 */
	private static void cargarEnvLocal() {
		Path envFile = Path.of(".env");
		if (!Files.exists(envFile)) {
			return;
		}
		try (var reader = Files.newBufferedReader(envFile, StandardCharsets.UTF_8)) {
			String linea;
			while ((linea = reader.readLine()) != null) {
				linea = linea.trim();
				if (linea.isEmpty() || linea.startsWith("#")) {
					continue;
				}
				int separador = linea.indexOf('=');
				if (separador <= 0) {
					continue;
				}
				String clave = linea.substring(0, separador).trim();
				String valor = linea.substring(separador + 1).trim();
				if (valor.length() >= 2 && valor.startsWith("\"") && valor.endsWith("\"")) {
					valor = valor.substring(1, valor.length() - 1);
				}
				if (System.getenv(clave) == null && System.getProperty(clave) == null) {
					System.setProperty(clave, valor);
				}
			}
		} catch (IOException e) {
			// Archivo .env ilegible: se ignora silenciosamente
		}
	}

}
