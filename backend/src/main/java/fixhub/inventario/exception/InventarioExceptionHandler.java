package fixhub.inventario.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "fixhub.inventario.controller")
public class InventarioExceptionHandler {

    @ExceptionHandler(RepuestoNoEncontradoException.class)
    public ProblemDetail manejarRepuestoNoEncontrado(
        RepuestoNoEncontradoException exception,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            exception.getMessage()
        );

        problem.setTitle("Repuesto no encontrado");
        problem.setInstance(URI.create(request.getRequestURI()));

        return problem;
    }

    @ExceptionHandler(CodigoRepuestoDuplicadoException.class)
    public ProblemDetail manejarCodigoDuplicado(
        CodigoRepuestoDuplicadoException exception,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            exception.getMessage()
        );

        problem.setTitle("Código de repuesto duplicado");
        problem.setInstance(URI.create(request.getRequestURI()));

        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail manejarValidacion(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        Map<String, String> errores = new LinkedHashMap<>();

        exception.getBindingResult().getFieldErrors().forEach(error ->
            errores.put(error.getField(), error.getDefaultMessage())
        );

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Los datos enviados no son válidos"
        );

        problem.setTitle("Error de validación");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errores", errores);

        return problem;
    }
}