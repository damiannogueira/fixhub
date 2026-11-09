package fixhub.comun.exception;

import jakarta.persistence.EntityExistsException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Respaldo común para las excepciones que los handlers de cada módulo no cubren.
 * Garantiza que toda respuesta de error tenga un ProblemDetail con un
 * mensaje en español en {@code detail}, que es lo que muestra el frontend.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final String MENSAJE_ACCESO_DENEGADO_GENERICO = "Access Denied";

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail manejarAccesoDenegado(
        AccessDeniedException exception,
        HttpServletRequest request
    ) {
        // @PreAuthorize lanza "Access Denied"; los servicios lanzan mensajes propios en español.
        String mensaje = exception.getMessage() == null
            || exception.getMessage().isBlank()
            || MENSAJE_ACCESO_DENEGADO_GENERICO.equalsIgnoreCase(exception.getMessage())
            ? "No tiene permisos para realizar esta acción"
            : exception.getMessage();

        return crear(HttpStatus.FORBIDDEN, "Acceso denegado", mensaje, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail manejarArgumentoInvalido(
        IllegalArgumentException exception,
        HttpServletRequest request
    ) {
        return crear(HttpStatus.BAD_REQUEST, "Solicitud inválida", exception.getMessage(), request);
    }

    @ExceptionHandler({IllegalStateException.class, EntityExistsException.class})
    public ProblemDetail manejarConflicto(
        RuntimeException exception,
        HttpServletRequest request
    ) {
        return crear(HttpStatus.CONFLICT, "Operación no permitida", exception.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail manejarCuerpoIlegible(
        HttpMessageNotReadableException exception,
        HttpServletRequest request
    ) {
        return crear(
            HttpStatus.BAD_REQUEST,
            "Formato inválido",
            "El formato de los datos enviados no es válido",
            request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail manejarTipoInvalido(
        MethodArgumentTypeMismatchException exception,
        HttpServletRequest request
    ) {
        return crear(
            HttpStatus.BAD_REQUEST,
            "Parámetro inválido",
            "El valor de '" + exception.getName() + "' no es válido",
            request
        );
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

        ProblemDetail problem = crear(
            HttpStatus.BAD_REQUEST,
            "Error de validación",
            "Los datos enviados no son válidos",
            request
        );
        problem.setProperty("errores", errores);
        return problem;
    }

    private ProblemDetail crear(
        HttpStatus status,
        String titulo,
        String detalle,
        HttpServletRequest request
    ) {
        String mensaje = detalle == null || detalle.isBlank()
            ? "No se pudo completar la operación"
            : detalle;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, mensaje);
        problem.setTitle(titulo);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
