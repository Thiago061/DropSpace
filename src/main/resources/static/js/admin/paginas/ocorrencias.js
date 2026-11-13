// Página "Ocorrências": registro com busca de lugar e acompanhamento de status.
import { api, json } from '../api.js';
import { dados, aoMudar, recarregarOcorrencias } from '../dados.js';
import { criarSeletor } from '../seletor-local.js';
import { $, STATUS_OCORRENCIA, escapeHtml, mostrar } from '../util.js';

const form = $('#form-ocorrencia');
const msg = $('#msg-ocorrencia');
let seletor = null;

export function iniciar() {
  seletor = criarSeletor($('[data-seletor]', form));
  aoMudar('dados:ocorrencias', renderizarLista);
  form.addEventListener('submit', salvar);
  $('#lista-ocorrencias').addEventListener('change', aoMudarStatus);
}

export const mostrarPagina = () => seletor.mostrar();

async function salvar(evento) {
  evento.preventDefault();
  const d = Object.fromEntries(new FormData(form));
  try {
    await api('/api/ocorrencias', {
      method: 'POST',
      body: json({ ...d, latitude: Number(d.latitude), longitude: Number(d.longitude), status: 'Aberto' }),
    });
    form.reset();
    seletor.limpar();
    mostrar(msg, 'Ocorrência registrada.', true);
    await recarregarOcorrencias();
  } catch (erro) { mostrar(msg, erro.message, false); }
}

function classeSeveridade(s) {
  return s === 'Crítico' ? 'critical' : s === 'Alto' ? 'alto' : '';
}

function renderizarLista() {
  const recentes = dados.ocorrencias.slice().sort((a, b) => b.id - a.id).slice(0, 50);
  const linhas = recentes.map(o => `
    <tr>
      <td>${escapeHtml(o.titulo)}<br><small>${escapeHtml(o.categoria)}</small></td>
      <td><span class="tag ${classeSeveridade(o.severidade)}">${escapeHtml(o.severidade)}</span></td>
      <td><select data-id="${o.id}" aria-label="Status de ${escapeHtml(o.titulo)}">
        ${STATUS_OCORRENCIA.map(s => `<option${s === o.status ? ' selected' : ''}>${s}</option>`).join('')}
      </select></td>
    </tr>`);
  $('#lista-ocorrencias').innerHTML = linhas.join('') || '<tr><td colspan="3">Nenhuma ocorrência cadastrada.</td></tr>';
}

async function aoMudarStatus(evento) {
  const select = evento.target.closest('select[data-id]');
  if (!select) return;
  try {
    await api(`/api/ocorrencias/${select.dataset.id}/status`, { method: 'PATCH', body: json({ status: select.value }) });
  } catch (erro) { mostrar(msg, erro.message, false); }
  await recarregarOcorrencias();
}
