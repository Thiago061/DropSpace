// Funções pequenas e constantes usadas por todas as páginas do painel.

export const $ = (seletor, raiz = document) => raiz.querySelector(seletor);
export const $$ = (seletor, raiz = document) => [...raiz.querySelectorAll(seletor)];

const ENTIDADES = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
/** Sempre use ao colocar texto vindo da API dentro de innerHTML. */
export const escapeHtml = valor => String(valor ?? '').replace(/[&<>"']/g, c => ENTIDADES[c]);

export const fmtData = iso =>
  iso ? new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' }) : '—';

export const fmtDia = iso =>
  iso ? new Date(iso + 'T00:00:00').toLocaleDateString('pt-BR') : 'sem prazo';

export const vazioParaNulo = valor => (valor === '' || valor == null ? null : valor);

/** Mostra uma mensagem de sucesso/erro em um elemento <p class="message">. */
export function mostrar(elemento, texto, ok) {
  elemento.textContent = texto;
  elemento.className = 'message ' + (texto ? (ok ? 'success' : 'error') : '');
}

export const TIPOS = {
  PONTO_AGUA: 'Ponto de água',
  DESCANSO_SOMBRA: 'Descanso / sombra',
  ABRIGO_FRIO: 'Abrigo (frio)',
  MERCADO_PARCEIRO: 'Mercado parceiro',
};

export const CORES = {
  PONTO_AGUA: '#287ca5',
  DESCANSO_SOMBRA: '#2e9e5b',
  ABRIGO_FRIO: '#7b5ea7',
  MERCADO_PARCEIRO: '#d99a00',
};

export const STATUS_OCORRENCIA = [
  'Aberto', 'Em análise', 'Validado', 'Em atendimento', 'Resolvido', 'Falso positivo', 'Cancelado',
];
