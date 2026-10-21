package fixhub.autenticacion.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AutenticacionExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail manejarErrorAutenticacion(
        AuthenticationException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            "El identificador o la contraseña son incorrectos"
        );

        problem.setTitle("Credenciales inválidas");

        return problem;
    }

    @ExceptionHandler(ClaveActualIncorrectaException.class)
    public ProblemDetail manejarClaveActualIncorrecta(
        ClaveActualIncorrectaException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            exception.getMessage()
        );

        problem.setTitle("Clave actual incorrecta");

        return problem;
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ProblemDetail manejarUsuarioNoEncontrado(
        UsuarioNoEncontradoException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            exception.getMessage()
        );

        problem.setTitle("Usuario no encontrado");

        return problem;
    }

    @ExceptionHandler(UsuarioSinRolesException.class)
    public ProblemDetail manejarUsuarioSinRoles(
        UsuarioSinRolesException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            exception.getMessage()
        );

        problem.setTitle("El usuario debe conservar al menos un rol");

        return problem;
    }
}
