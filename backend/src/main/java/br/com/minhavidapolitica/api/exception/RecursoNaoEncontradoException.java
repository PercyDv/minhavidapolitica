package br.com.minhavidapolitica.api.exception;

/** Recurso inexistente (ou não disponível para consulta pública). Resulta em HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
