# Cartão 6.2: Modelagem de Classes (texto de apoio)

**Entregável:** Diagrama de Classes (`MVP_6-2_Diagrama-de-Classes_v2.png`) com este texto curto de apoio para o documento no Overleaf (atividade 6.10).

## Visão geral

O diagrama mostra o modelo de domínio do back-end do MinhaVidaPolitica.com: as entidades que o sistema guarda, seus atributos e como se relacionam. Ele foi derivado dos requisitos RF01 a RF13 e das regras de negócio RN01 a RN07 do Documento de Requisitos. Cada classe do diagrama corresponde a uma entidade JPA do projeto (`backend/src/main/java/.../domain`) e a uma tabela do banco (cartão 6.5).

## Entidades

| Classe | Responsabilidade | Requisitos |
|---|---|---|
| `Politico` | Pessoa em exercício no mandato atual, com cargo, estado (quando estadual), partido, resumo, vigência do mandato e a fonte que sustenta o registro. | RF01, RF03, RF04, RF07, RN01 |
| `Cargo` | Cargo eletivo exibido pelo sistema (Presidente, Governador etc.), com esfera NACIONAL ou ESTADUAL. É uma tabela, e não uma lista fixa no código, porque os cargos de "demais representantes" ainda dependem de definição do cliente. | RF04, risco R04 |
| `UnidadeFederativa` | As 26 UFs e o Distrito Federal, usadas no seletor de estado. | RF02, RN02 |
| `Proposta` | Proposta de um político no mandato atual. | RF05 |
| `AcaoConcretizada` | Ação já realizada no mandato atual. Exige fonte e data. | RF06 |
| `FonteDados` | Fonte oficial ou confiável, com o registro de aprovação do cliente (quem, quando) e a periodicidade de atualização. É a classe central das regras de transparência. | RF08, RF13, RN05 |
| `LogAtualizacao` | Histórico de cada importação de dados: resultado, contagens e motivos de rejeição. | RF13 |
| `Candidatura` | Quem concorreu a presidente ou governador numa eleição (ano, número, nome, partido e situação final: eleito, não eleito, segundo turno ou sem resultado). Não tem mandato, votos nem ranking. **Extensão pedida pela equipe, fora do escopo validado (RN01), que depende de decisão do cliente.** | Pedido da equipe |
| `UsuarioPainel` | Pessoa da equipe mantenedora com acesso ao painel, com perfil ADMIN ou EDITOR. A senha é guardada somente como hash BCrypt. | Perfil "Equipe mantenedora" |

As enumerações `Esfera`, `TipoFonte`, `StatusImportacao`, `SituacaoCandidatura` e `PerfilUsuario` restringem os valores possíveis de cada campo correspondente.

## Relacionamentos

- Um `Politico` exerce um `Cargo` e, quando o cargo é estadual, representa uma `UnidadeFederativa` (relação opcional, pois o Presidente é nacional).
- Um `Politico` tem várias `Proposta` e várias `AcaoConcretizada`. A relação é de composição: excluir o político exclui também esses registros.
- `Politico`, `Proposta` e `AcaoConcretizada` apontam, cada um, para a `FonteDados` que sustenta o dado. Isso atende ao RF08, porque cada bloco de informação mostra sua própria fonte, o link e a data de atualização.
- `Candidatura` disputa um `Cargo` (presidente ou governador), pode ter uma `UnidadeFederativa` (só governador) e aponta para a `FonteDados` que a sustenta.
- `LogAtualizacao` aponta para a `FonteDados` importada.
- `UsuarioPainel` não se liga às demais classes. O nome de quem executou uma importação é guardado como texto no log.

## Decisões de modelagem

1. **A fonte decide o que o cidadão vê (RN05).** Um dado só é exibido se a sua `FonteDados` estiver aprovada pelo cliente e ativa (`liberaExibicao()`). Enquanto isso não acontece, o registro fica retido e o sistema responde com a mensagem neutra do RF12.
2. **Só o mandato atual (RN01).** O `Politico` guarda `emExercicio`, `inicioMandato` e `fimMandato`, e só é exibido quando a data de hoje está dentro da vigência. Não existe histórico de mandatos anteriores, que está fora do escopo da versão 1. Por isso a classe `Mandato`, cogitada no rascunho da Etapa 4, foi incorporada ao `Politico`.
3. **Sem juízo de valor (RN04, RF09).** Nenhuma classe tem campo de nota, ranking ou avaliação. As listas são ordenadas por cargo e nome, ou por data, nunca por mérito.
4. **Cargo estadual exige UF; cargo nacional não pode ter UF (RN02).** A regra é validada no serviço, tanto no cadastro manual quanto na importação.
5. **Sem cadastro de cidadão.** O modelo não tem nenhuma entidade para o cidadão nem para sua localização (RN03). Só a equipe mantenedora tem login.
6. **Candidatos separados de quem exerce mandato.** `Candidatura` não se mistura com `Politico`: o cidadão vê quem governa (RN01) e, em consulta à parte, quem concorreu. A lista é sempre alfabética, sem votos nem ranking (RN04).
