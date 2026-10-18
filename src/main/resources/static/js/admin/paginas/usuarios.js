// Página "Usuários": resumo, lista com busca e paginação, planos, ativação e histórico de acessos.
import { api, json } from '../api.js';
import { $, $$, escapeHtml, fmtData, mostrar } from '../util.js';

const TAMANHO = 15;
let pagina = 0;
let termo = '';
let temporizador = null;

export function iniciar() {
  $('#busca-usuarios').addEventListener('input', e => {
    clearTimeout(temporizador); // espera o admin parar de digitar
    temporizador = setTimeout(() => { termo = e.target.value.trim(); pagina = 0; carregarLista(); }, 350);
  });
  $('#pagina-anterior').addEventListener('click', () => { pagina = Math.max(0, pagina - 1); carregarLista(); });
  $('#pagina-seguinte').addEventListener('click', () => { pagina += 1; carregarLista(); });
  $('#lista-usuarios').addEventListener('click', aoClicarNaLista);
  $$('[data-aba-usuarios]').forEach(b => b.addEventListener('click', () => trocarAba(b.dataset.abaUsuarios)));
}

export async function carregar() {
  await Promise.all([carregarResumo(), carregarLista(), carregarAcessos()]);
}

function trocarAba(nome) {
  $$('[data-aba-usuarios]').forEach(b => b.classList.toggle('ativa', b.dataset.abaUsuarios === nome));
  $('#aba-lista').hidden = nome !== 'lista';
  $('#aba-acessos').hidden = nome !== 'acessos';
}

async function carregarResumo() {
  const r = await api('/api/admin/usuarios/resumo');
  $('#res-total').textContent = r.total;
  $('#res-novos').textContent = r.novos7Dias;
  $('#res-ativos').textContent = r.ativos7Dias;
  $('#res-pro').textContent = r.pro;
}

async function carregarLista() {
  try {
    const p = await api(`/api/admin/usuarios?q=${encodeURIComponent(termo)}&pagina=${pagina}&tamanho=${TAMANHO}`);
    $('#lista-usuarios').innerHTML = p.itens.map(linha).join('')
      || '<tr><td colspan="7">Nenhum usuário encontrado.</td></tr>';
    const totalPaginas = Math.max(1, Math.ceil(p.total / TAMANHO));
    $('#info-pagina').textContent = `${p.total} usuário(s) · página ${p.pagina + 1} de ${totalPaginas}`;
    $('#pagina-anterior').disabled = p.pagina <= 0;
    $('#pagina-seguinte').disabled = p.pagina + 1 >= totalPaginas;
  } catch (erro) { mostrar($('#msg-usuarios'), erro.message, false); }
}

function linha(u) {
  const admin = u.papel === 'ADMIN';
  return `
    <tr>
      <td>${escapeHtml(u.nome)}${admin ? ' <span class="tag">Admin</span>' : ''}</td>
      <td>${escapeHtml(u.login)}</td>
      <td><span class="tag ${u.plano === 'PRO' ? 'pro' : ''}">${escapeHtml(u.plano)}</span>${u.plano === 'PRO' && u.planoExpiraEm ? `<br><small>até ${fmtData(u.planoExpiraEm)}</small>` : ''}</td>
      <td>${fmtData(u.criadoEm)}</td>
      <td>${fmtData(u.ultimoLoginEm)}<br><small>${u.totalLogins} acesso(s)</small></td>
      <td><span class="tag ${u.ativo ? 'resolvido' : 'off'}">${u.ativo ? 'Ativo' : 'Desativado'}</span></td>
      <td class="row-actions">${admin ? '' : `
        <button type="button" class="button-light" data-acao="pro" data-id="${u.id}">Dar Pro</button>
        ${u.plano === 'PRO' ? `<button type="button" class="button-light" data-acao="free" data-id="${u.id}">Tirar Pro</button>` : ''}
        <button type="button" class="button-light" data-acao="ativo" data-id="${u.id}" data-valor="${!u.ativo}">${u.ativo ? 'Desativar' : 'Reativar'}</button>`}
      </td>
    </tr>`;
}

async function aoClicarNaLista(evento) {
  const botao = evento.target.closest('button[data-acao]');
  if (!botao) return;
  const id = botao.dataset.id;
  const msg = $('#msg-usuarios');
  try {
    if (botao.dataset.acao === 'ativo') {
      await api(`/api/admin/usuarios/${id}/ativo?ativo=${botao.dataset.valor === 'true'}`, { method: 'PATCH' });
    } else {
      const dias = Number($('#dias-pro').value) || 30;
      const corpo = botao.dataset.acao === 'pro' ? { plano: 'PRO', dias } : { plano: 'FREE' };
      await api(`/api/admin/usuarios/${id}/plano`, { method: 'PATCH', body: json(corpo) });
    }
    mostrar(msg, 'Alteração salva.', true);
    await Promise.all([carregarResumo(), carregarLista()]);
  } catch (erro) { mostrar(msg, erro.message, false); }
}

async function carregarAcessos() {
  try {
    const lista = await api('/api/admin/acessos');
    $('#lista-acessos').innerHTML = lista.map(a => `
      <tr>
        <td>${fmtData(a.criadoEm)}</td>
        <td>${escapeHtml(a.login)}</td>
        <td><span class="tag ${a.sucesso ? 'resolvido' : 'critical'}">${a.sucesso ? 'Entrou' : 'Falhou'}</span></td>
        <td>${escapeHtml(a.ip || '—')}</td>
      </tr>`).join('') || '<tr><td colspan="4">Nenhum acesso registrado ainda.</td></tr>';
  } catch (erro) { mostrar($('#msg-usuarios'), erro.message, false); }
}
