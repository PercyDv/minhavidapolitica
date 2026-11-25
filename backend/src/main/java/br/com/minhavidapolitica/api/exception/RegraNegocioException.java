package br.com.minhavidapolitica.api.exception;

import org.springframework.http.HttpStatus;

/** Violação de regra de negócio. Por padrão HTTP 422; conflitos de estado usam 409. */
public class RegraNegocioException extends RuntimeException {

    private final HttpStatus status;

    public RegraNegocioException(String mensagem) {
        this(mensagem, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public RegraNegocioException(String mensagem, HttpStatus status) {
        super(mensagem);
        this.status = status;
    }

    public static RegraNegocioException conflito(String mensagem) {
        return new RegraNegocioException(mensagem, HttpStatus.CONFLICT);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
