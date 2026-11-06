// Comunicação com o backend e guarda do token da sessão (fica só na aba do navegador).

const CHAVE = 'zonageo.admin.token';
let token = sessionStorage.getItem(CHAVE);
let aoExpirar = () => {};

export const temToken = () => token !== null;
export const aoSessaoExpirar = funcao => { aoExpirar = funcao; };

export function guardarToken(novo) {
  token = novo;
  sessionStorage.setItem(CHAVE, novo);
}

export function limparToken() {
  token = null;
  sessionStorage.removeItem(CHAVE);
}

const MSG_OFFLINE =
  'Não consegui falar com o servidor. Confirme se o backend está rodando (mvn spring-boot:run) em http://localhost:8080.';

async function enviar(url, opcoes, comToken) {
  const cabecalhos = { 'Content-Type': 'application/json' };
  if (comToken && token) cabecalhos.Authorization = 'Bearer ' + token;
  const controle = new AbortController();
  const limite = setTimeout(() => controle.abort(), 20000);
  try {
    return await fetch(url, { ...opcoes, headers: cabecalhos, signal: controle.signal });
  } catch (_) {
    throw new Error(MSG_OFFLINE);
  } finally {
    clearTimeout(limite);
  }
}

async function lerErro(resposta) {
  try {
    const corpo = await resposta.json();
    return corpo.detail || corpo.message || null;
  } catch (_) {
    return null;
  }
}

/** Chamada autenticada. Em 401 encerra a sessão. */
export async function api(url, opcoes = {}) {
  const resposta = await enviar(url, opcoes, true);
  if (resposta.status === 401) {
    aoExpirar();
    throw new Error('Sessão expirada. Entre novamente.');
  }
  if (resposta.status === 403) throw new Error('Você não tem permissão para esta ação.');
  if (!resposta.ok) throw new Error((await lerErro(resposta)) || 'Erro ' + resposta.status);
  return resposta.status === 204 ? null : resposta.json();
}

/** Login nunca envia token: um token velho faria o servidor responder 401 mesmo com a senha certa. */
export async function login(usuario, senha) {
  const resposta = await enviar('/api/auth/login', {
    method: 'POST', body: JSON.stringify({ login: usuario, senha }),
  }, false);
  if (!resposta.ok) {
    throw new Error((await lerErro(resposta)) || 'Não foi possível entrar (erro ' + resposta.status + ').');
  }
  return resposta.json();
}

export const json = corpo => JSON.stringify(corpo);
