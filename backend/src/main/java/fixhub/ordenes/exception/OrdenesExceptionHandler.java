package fixhub.ordenes.exception;

import fixhub.autenticacion.exception.UsuarioNoEsTecnicoException;
import fixhub.clientes.exception.EquipoNoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "fixhub.ordenes.controller")
public class OrdenesExceptionHandler {

    @ExceptionHandler(OrdenTrabajoNoEncontradaException.class)
    public ProblemDetail manejarOrdenNoEncontrada(
        OrdenTrabajoNoEncontradaException exception,
        HttpServletRequest request
    ) {
        return crearProblem(
            HttpStatus.NOT_FOUND,
            "Orden no encontrada",
            exception.getMessage(),
            request
        );
    }

    @ExceptionHandler(EquipoNoEncontradoException.class)
    public ProblemDetail manejarEquipoNoEncontrado(
        EquipoNoEncontradoException exception,
        HttpServletRequest request
    ) {
        return crearProblem(
            HttpStatus.NOT_FOUND,
            "Equipo no encontrado",
            exception.getMessage(),
            request
        );
    }

    @ExceptionHandler(UsuarioNoEsTecnicoException.class)
    public ProblemDetail manejarUsuarioNoTecnico(
        UsuarioNoEsTecnicoException exception,
        HttpServletRequest request
    ) {
        return crearProblem(
            HttpStatus.BAD_REQUEST,
            "Usuario no técnico",
            exception.getMessage(),
            request
        );
    }

    @ExceptionHandler({
        TecnicoRequeridoException.class,
        TransicionEstadoInvalidaException.class,
        OrdenNoCancelableException.class
    })
    public ProblemDetail manejarConflicto(
        RuntimeException exception,
        HttpServletRequest request
    ) {
        return crearProblem(
            HttpStatus.CONFLICT,
            "Operación no permitida",
            exception.getMessage(),
            request
        );
    }

    @ExceptionHandler(EstadoOrdenNoEncontradoException.class)
    public ProblemDetail manejarEstadoFaltante(
        EstadoOrdenNoEncontradoException exception,
        HttpServletRequest request
    ) {
        return crearProblem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Configuración de estados incompleta",
            exception.getMessage(),
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

        ProblemDetail problem = crearProblem(
            HttpStatus.BAD_REQUEST,
            "Error de validación",
            "Los datos enviados no son válidos",
            request
        );
        problem.setProperty("errores", errores);
        return problem;
    }

    private ProblemDetail crearProblem(
        HttpStatus status,
        String title,
        String detail,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            status,
            detail
        );
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
