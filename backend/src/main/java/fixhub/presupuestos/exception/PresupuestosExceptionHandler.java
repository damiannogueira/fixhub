package fixhub.presupuestos.exception;

import fixhub.inventario.exception.RepuestoNoEncontradoException;
import fixhub.ordenes.exception.OrdenTrabajoNoEncontradaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "fixhub.presupuestos.controller")
public class PresupuestosExceptionHandler {

    @ExceptionHandler({
        PresupuestoNoEncontradoException.class,
        RepuestoNoEncontradoException.class,
        OrdenTrabajoNoEncontradaException.class
    })
    public ProblemDetail manejarRecursoNoEncontrado(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, exception.getMessage()
        );
        problem.setTitle("Recurso no encontrado");
        return problem;
    }
}
