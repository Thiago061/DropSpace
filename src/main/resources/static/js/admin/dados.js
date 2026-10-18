// Dados compartilhadas entre páginas. Quando algo muda, avisamos as páginas por eventos do documento.
import { api } from './api.js';

export const dados = { pontos: [], ocorrencias: [], ofertas: [] };

function carregarEAvisar(chave, url, evento) {
  return api(url).then(lista => {
    dados[chave] = lista;
    document.dispatchEvent(new CustomEvent(evento));
    return lista;
  });
}

export const recarregarPontos = () => carregarEAvisar('pontos', '/api/admin/pontos', 'dados:pontos');
export const recarregarOcorrencias = () => carregarEAvisar('ocorrencias', '/api/ocorrencias', 'dados:ocorrencias');
export const recarregarOfertas = () => carregarEAvisar('ofertas', '/api/admin/ofertas', 'dados:ofertas');

export const aoMudar = (evento, funcao) => document.addEventListener(evento, funcao);
