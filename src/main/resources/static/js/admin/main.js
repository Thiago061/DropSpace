// Ponto de entrada do painel: login, navegação entre páginas e carga inicial dos dados.
import { api, aoSessaoExpirar, limparToken, temToken } from './api.js';
import { iniciarLogin } from './auth.js';
import { recarregarOcorrencias, recarregarOfertas, recarregarPontos } from './dados.js';
import * as ocorrencias from './paginas/ocorrencias.js';
import * as painel from './paginas/painel.js';
import * as pontos from './paginas/pontos.js';
import * as usuarios from './paginas/usuarios.js';
import * as vouchers from './paginas/vouchers.js';
import { $, $$, mostrar } from './util.js';

const TITULOS = {
  painel: 'Painel e mapa', pontos: 'Pontos de apoio', ocorrencias: 'Ocorrências',
  vouchers: 'Vouchers', usuarios: 'Usuários',
};

let paginasIniciadas = false;

// ------------------------------------------------------------------ sessão
function mostrarLogin() {
  $('#tela-painel').hidden = true;
  $('#tela-login').hidden = false;
}

function sair() {
  limparToken();
  mostrarLogin();
}

async function entrar(usuario) {
  $('#tela-login').hidden = true;
  $('#tela-painel').hidden = false;
  if (usuario) $('#usuario-logado').textContent = 'Conectado como ' + usuario.login;

  if (!paginasIniciadas) {
    // cada página é iniciada isoladamente: se uma falhar, o resto do painel continua funcionando
    [painel, pontos, ocorrencias, vouchers, usuarios].forEach(pagina => {
      try { pagina.iniciar(); } catch (erro) { console.error('Falha ao iniciar página', erro); }
    });
    paginasIniciadas = true;
  }
  navegar((location.hash || '#painel').slice(1));
  await carregarTudo();
}

// ------------------------------------------------------------- navegação
function navegar(nome) {
  if (!TITULOS[nome]) nome = 'painel';
  $$('.view').forEach(v => { v.hidden = v.dataset.view !== nome; });
  $$('#menu a').forEach(a => a.classList.toggle('active', a.dataset.view === nome));
  $('#titulo-view').textContent = TITULOS[nome];

  if (nome === 'painel') painel.mostrar();
  if (nome === 'pontos') pontos.mostrarPagina();
  if (nome === 'ocorrencias') ocorrencias.mostrarPagina();
  if (nome === 'vouchers') vouchers.carregarEmitidos().catch(() => {});
  if (nome === 'usuarios') usuarios.carregar().catch(() => {});
}

$('#menu').addEventListener('click', evento => {
  const link = evento.target.closest('a[data-view]');
  if (!link) return;
  evento.preventDefault();
  history.replaceState(null, '', '#' + link.dataset.view);
  navegar(link.dataset.view);
});
window.addEventListener('hashchange', () => { if (!$('#tela-painel').hidden) navegar(location.hash.slice(1)); });
$('#btn-sair').addEventListener('click', sair);

// ------------------------------------------------------------ carga inicial
async function carregarTudo() {
  const resultados = await Promise.allSettled([
    painel.carregarIndicadores(), recarregarPontos(), recarregarOcorrencias(), recarregarOfertas(),
    vouchers.carregarEmitidos(), usuarios.carregar(),
  ]);
  const falha = resultados.find(r => r.status === 'rejected');
  const aviso = $('#aviso-global');
  if (falha) {
    $('#status-api').innerHTML = '<i></i> Falha ao carregar';
    mostrar(aviso, falha.reason.message, false);
  } else {
    $('#status-api').innerHTML = '<i></i> API conectada';
    mostrar(aviso, '', true);
  }
}

// ---------------------------------------------------------------- início
aoSessaoExpirar(sair);
iniciarLogin(entrar);
if (temToken()) {
  // há um token da última visita: só entra direto se o servidor ainda o aceitar
  api('/api/dashboard').then(() => entrar(null)).catch(sair);
}
