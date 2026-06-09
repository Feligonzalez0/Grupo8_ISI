package com.is1.proyecto.services;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.is1.proyecto.models.User;

public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    // REGISTRO
    public User registrar(String username, String password) {
        // --- Validación de campos obligatorios ---
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }

        // --- Verificar que el nombre de usuario no esté tomado ---
        User usuarioExistente = User.findFirst("name = ?", username.trim());
        if (usuarioExistente != null) {
            throw new IllegalArgumentException("El nombre de usuario '" + username + "' ya existe.");
        }

        // --- Crear y persistir el nuevo usuario ---
        try {
            User nuevoUsuario = new User();
            nuevoUsuario.set("name", username.trim());
            nuevoUsuario.set("password", BCrypt.hashpw(password, BCrypt.gensalt()));
            nuevoUsuario.set("rol", "UNASSIGNED");
            nuevoUsuario.saveIt();

            logger.info("Usuario registrado correctamente: {}", username);
            return nuevoUsuario;

        } catch (Exception e) {
            logger.error("Error al registrar usuario '{}': {}", username, e.getMessage());
            throw new RuntimeException("Error interno al crear la cuenta. Intente de nuevo.", e);
        }
    }

    // LOGIN
    public User login(String username, String plainTextPassword) {
        // --- Validación de campos obligatorios ---
        if (username == null || username.trim().isEmpty()
                || plainTextPassword == null || plainTextPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario y la contraseña son requeridos.");
        }

        // --- Buscar el usuario en la base de datos ---
        User usuario = User.findFirst("name = ?", username.trim());

        if (usuario == null) {
            logger.warn("Intento de login fallido: usuario '{}' no encontrado.", username);
            // Mensaje genérico: no revelar si el usuario existe o no
            throw new SecurityException("Usuario o contraseña incorrectos.");
        }

        // --- Verificar la contraseña con BCrypt ---
        String hashedPassword = usuario.getString("password");

        boolean passwordCorrecta;
        try {
            passwordCorrecta = BCrypt.checkpw(plainTextPassword, hashedPassword);
        } catch (Exception e) {
            // BCrypt puede lanzar excepción si el hash almacenado está malformado
            logger.error("Error al verificar contraseña para usuario '{}': {}", username, e.getMessage());
            throw new SecurityException("Usuario o contraseña incorrectos.");
        }

        if (!passwordCorrecta) {
            logger.warn("Intento de login fallido: contraseña incorrecta para usuario '{}'.", username);
            throw new SecurityException("Usuario o contraseña incorrectos.");
        }

        logger.info("Login exitoso para usuario: {}", username);
        return usuario;
    }

    // CAMBIO DE CONTRASEÑA
    public void cambiarPassword(User usuario, String passwordActual, String passwordNueva, String passwordConfirm) {

        // --- Validación de campos obligatorios ---
        if (passwordActual == null || passwordActual.isEmpty()
                || passwordNueva == null || passwordNueva.isEmpty()
                || passwordConfirm == null || passwordConfirm.isEmpty()) {
            throw new IllegalArgumentException("Todos los campos son obligatorios.");
        }

        // --- Verificar contraseña actual ---
        try {
            if (!BCrypt.checkpw(passwordActual, usuario.getString("password"))) {
                throw new SecurityException("La contraseña actual es incorrecta.");
            }
        } catch (SecurityException e) {
            throw e; // re-lanzar la excepción de seguridad que acabamos de crear
        } catch (Exception e) {
            logger.error("Error al verificar contraseña actual para usuario '{}': {}",
                    usuario.getString("name"), e.getMessage());
            throw new SecurityException("Error al verificar la contraseña actual.");
        }

        // --- Verificar que las contraseñas nuevas coincidan ---
        if (!passwordNueva.equals(passwordConfirm)) {
            throw new IllegalArgumentException("Las contraseñas nuevas no coinciden.");
        }

        // --- Persistir la nueva contraseña hasheada ---
        try {
            usuario.set("password", BCrypt.hashpw(passwordNueva, BCrypt.gensalt()));
            usuario.saveIt();
            logger.info("Contraseña actualizada para usuario: {}", usuario.getString("name"));
        } catch (Exception e) {
            logger.error("Error al guardar nueva contraseña: {}", e.getMessage());
            throw new RuntimeException("Error al guardar la contraseña.", e);
        }
    }

    // HELPERS INTERNOS
    public boolean existeUsuario(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return User.findFirst("name = ?", username.trim()) != null;
    }
}