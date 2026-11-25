package br.com.minhavidapolitica.api.service;

import br.com.minhavidapolitica.api.dto.CatalogoResponse;
import br.com.minhavidapolitica.api.dto.CatalogoResponse.CargoResumo;
import br.com.minhavidapolitica.api.dto.CatalogoResponse.FonteResumo;
import br.com.minhavidapolitica.api.dto.UfResumo;
import br.com.minhavidapolitica.api.repository.CargoRepository;
import br.com.minhavidapolitica.api.repository.FonteDadosRepository;
import br.com.minhavidapolitica.api.repository.UnidadeFederativaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Listas de apoio aos formulários do painel. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoService {

    private final UnidadeFederativaRepository ufRepository;
    private final CargoRepository cargoRepository;
    private final FonteDadosRepository fonteRepository;

    public CatalogoResponse catalogos() {
        return new CatalogoResponse(
                ufRepository.findAllByOrderBySiglaAsc().stream().map(UfResumo::de).toList(),
                cargoRepository.findAllByAtivoTrueOrderByIdAsc().stream().map(CargoResumo::de).toList(),
                fonteRepository.findAllByOrderByNomeAsc().stream().map(FonteResumo::de).toList());
    }
}
