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
}
