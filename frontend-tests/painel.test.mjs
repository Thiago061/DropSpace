import { JSDOM } from 'jsdom';
import fs from 'fs';

const html = fs.readFileSync(new URL('../src/main/resources/static/admin/index.html', import.meta.url), 'utf8').replace(/<script[^>]*><\/script>/g, '');
const dom = new JSDOM(html, { url: 'http://localhost:8080/admin/index.html', pretendToBeVisual: true });
const { window } = dom;
for (const k of ['document', 'location', 'history', 'sessionStorage', 'CustomEvent', 'FormData', 'HTMLElement']) {
  Object.defineProperty(globalThis, k, { value: window[k], configurable: true, writable: true });
}
globalThis.window = window;

// --- Leaflet falso (o mapa real não importa para a lógica)
const encadeavel = () => new Proxy(function () {}, { get: (_, p) => (p === 'getLatLng' ? () => ({ lat: 0, lng: 0 }) : encadeavel()), apply: () => encadeavel() });
globalThis.L = { map: () => encadeavel(), tileLayer: () => encadeavel(), layerGroup: () => encadeavel(), circleMarker: () => encadeavel(), marker: () => encadeavel() };

// --- API falsa
const chamadas = [];
const resp = (corpo, status = 200) => ({ ok: status < 400, status, json: async () => corpo });
globalThis.fetch = async (url, op = {}) => {
  chamadas.push(`${op.method || 'GET'} ${url} ${op.headers?.Authorization ? '[auth]' : '[sem auth]'}`);
  const corpo = op.body ? JSON.parse(op.body) : null;
  if (url === '/api/auth/login') return corpo.senha === 'certa' ? resp({ token: 'T', usuario: { login: 'zona1', papel: 'ADMIN' } }) : resp({ detail: 'Usuário ou senha inválidos.' }, 401);
  if (url === '/api/dashboard') return resp({ pontosApoio: 6, ocorrenciasAbertas: 2, usuarios: 3, usuariosPro: 1 });
  if (url === '/api/admin/pontos') return resp([{ id: 4, nome: 'Atacadão <b>', tipo: 'MERCADO_PARCEIRO', latitude: -15.79, longitude: -47.88, ativo: true, gratuito: false, sombra: false }]);
  if (url === '/api/ocorrencias') return resp([{ id: 1, titulo: 'Foco', categoria: 'Queimada', severidade: 'Alto', status: 'Aberto', latitude: -15.8, longitude: -47.9 }]);
  if (url === '/api/admin/ofertas' && !op.method) return resp([{ id: 1, pontoId: 4, pontoNome: 'Atacadão', titulo: '1 água', descricao: null, validoAte: null, ativo: true, vigente: true }]);
  if (url === '/api/admin/ofertas' && op.method === 'POST') return resp({ id: 2 }, 201);
  if (url === '/api/admin/vouchers') return resp([{ codigo: 'ZG-AAAA-BBBB', pontoNome: 'Atacadão', descricao: '1 água', status: 'ATIVO', emitidoEm: '2026-10-18T10:00:00Z' }]);
  if (url === '/api/admin/usuarios/resumo') return resp({ total: 3, novos7Dias: 2, ativos7Dias: 3, pro: 1, desativados: 0 });
  if (url.startsWith('/api/admin/usuarios?')) return resp({ total: 2, pagina: 0, tamanho: 15, itens: [
    { id: 7, nome: 'Maria', login: 'maria@x.com', papel: 'USER', plano: 'PRO', planoExpiraEm: '2026-11-01T00:00:00Z', ativo: true, criadoEm: '2026-10-10T00:00:00Z', ultimoLoginEm: '2026-10-17T12:00:00Z', totalLogins: 5 },
    { id: 1, nome: 'Administrador', login: 'zona1', papel: 'ADMIN', plano: 'FREE', ativo: true, criadoEm: '2026-10-01T00:00:00Z', ultimoLoginEm: null, totalLogins: 0 }] });
  if (url === '/api/admin/acessos') return resp([{ criadoEm: '2026-10-17T12:00:00Z', login: 'maria@x.com', sucesso: false, ip: '1.2.3.4' }]);
  if (url.startsWith('/api/admin/lugares')) return resp({ fonte: 'Google', aviso: null, lugares: [{ nome: 'Sobradinho', endereco: 'Sobradinho, Brasília - DF', latitude: -15.6531, longitude: -47.7903 }] });
  return resp({ detail: 'rota não mockada ' + url }, 404);
};

let falhas = 0;
const ok = (c, m) => { console.log((c ? 'ok:     ' : 'FALHOU: ') + m); if (!c) falhas++; };
const esperar = ms => new Promise(r => setTimeout(r, ms));
const $ = s => document.querySelector(s);
const submit = f => f.dispatchEvent(new window.Event('submit', { cancelable: true, bubbles: true }));

await import('../src/main/resources/static/js/admin/main.js');

// 1. senha errada
$('#form-login').elements.login.value = 'Zona1'; $('#form-login').elements.senha.value = 'errada';
submit($('#form-login')); await esperar(50);
ok($('#msg-login').textContent === 'Usuário ou senha inválidos.', 'senha errada mostra mensagem do servidor');
ok(!$('#tela-login').hidden, 'continua na tela de login');
ok(!$('#botao-entrar').disabled, 'botão volta a ficar habilitado');

// 2. senha certa
$('#form-login').elements.senha.value = 'certa';
submit($('#form-login')); await esperar(200);
ok($('#tela-login').hidden && !$('#tela-painel').hidden, 'login correto abre o painel');
ok(window.sessionStorage.getItem('zonageo.admin.token') === 'T', 'token guardado na sessão');
ok(chamadas.some(c => c.startsWith('POST /api/auth/login') && c.endsWith('[sem auth]')), 'login NÃO envia token');
ok($('#ind-pontos').textContent === '6' && $('#ind-pro').textContent === '1', 'indicadores do painel preenchidos');
ok($('#lista-pontos').textContent.includes('Atacadão <b>') && !$('#lista-pontos').innerHTML.includes('<b>'), 'lista de pontos escapa HTML (anti-XSS)');
ok($('#lista-ocorrencias').textContent.includes('Foco'), 'lista de ocorrências renderizada');
ok($('#lista-ofertas').textContent.includes('1 água'), 'lista de ofertas de voucher renderizada');
ok($('#lista-emitidos').textContent.includes('ZG-AAAA-BBBB'), 'vouchers emitidos listados');
ok($('#form-oferta').elements.pontoId.innerHTML.includes('Atacadão'), 'select de mercados parceiros preenchido');

// 3. página de usuários
ok($('#res-total').textContent === '3' && $('#res-novos').textContent === '2', 'resumo de usuários');
ok($('#lista-usuarios').textContent.includes('Maria') && $('#lista-usuarios').textContent.includes('5 acesso(s)'), 'usuários com número de acessos');
ok($('#lista-usuarios').querySelectorAll('button[data-acao="pro"]').length === 1, 'admin não tem botões de plano');
ok($('#lista-acessos').textContent.includes('Falhou') && $('#lista-acessos').textContent.includes('1.2.3.4'), 'acessos recentes mostram falha e IP');
ok($('#info-pagina').textContent.includes('2 usuário'), 'paginação informa o total');

// 4. navegação
document.querySelector('#menu a[data-view="usuarios"]').dispatchEvent(new window.MouseEvent('click', { bubbles: true, cancelable: true }));
ok(!document.querySelector('[data-view="usuarios"].view').hidden && document.querySelector('[data-view="painel"].view').hidden, 'menu troca de página');
ok($('#titulo-view').textContent === 'Usuários', 'título da página muda');

// 5. busca de lugar no cadastro de ponto
const form = $('#form-ponto');
form.querySelector('[data-busca]').value = 'Sobradinho';
form.querySelector('[data-buscar]').click(); await esperar(80);
const resultado = form.querySelector('[data-resultados] button');
ok(!!resultado && resultado.textContent.includes('Sobradinho'), 'busca de lugar lista resultados');
resultado.click(); await esperar(30);
ok(form.elements.latitude.value === '-15.6531' && form.elements.longitude.value === '-47.7903', 'escolher o lugar preenche latitude/longitude');
ok(form.elements.nome.value === 'Sobradinho' && form.elements.endereco.value.includes('Brasília'), 'preenche nome e endereço vazios');

// 6. cadastrar oferta de voucher
const fo = $('#form-oferta');
fo.elements.titulo.value = '1 água mineral'; fo.elements.pontoId.value = '4';
submit(fo); await esperar(80);
ok(chamadas.some(c => c.startsWith('POST /api/admin/ofertas')), 'cadastro de oferta chama a API');
ok($('#msg-oferta').textContent === 'Oferta cadastrada.', 'mensagem de sucesso da oferta');

// 7. sair
$('#btn-sair').click();
ok(!$('#tela-login').hidden && window.sessionStorage.getItem('zonageo.admin.token') === null, 'sair volta ao login e apaga o token');

console.log(falhas === 0 ? '\nTODOS OS TESTES DO FRONT PASSARAM' : '\nFALHAS: ' + falhas);
process.exit(falhas === 0 ? 0 : 1);
