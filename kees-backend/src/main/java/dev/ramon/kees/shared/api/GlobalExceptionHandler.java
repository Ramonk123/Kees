package dev.ramon.kees.shared.api;

import dev.ramon.kees.shared.exception.ResourceNotFoundException;
import dev.ramon.kees.source.SourceAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SourceAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleSourceAlreadyExists(
            SourceAlreadyExistsException exception
    ) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
        problem.setTitle("Source already exists");
        return problem;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleResourceNotFound(
            ResourceNotFoundException exception
    ) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );

        problem.setTitle("Resource not found");
        problem.setProperty("resource", exception.getName());
        problem.setProperty("identifier", exception.getIdentifier());

        return problem;
    }
}
