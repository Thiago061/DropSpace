// Página "Pontos de apoio": cadastro com busca de lugar, edição e ativação.
import { api, json } from '../api.js';
import { dados, aoMudar, recarregarPontos } from '../dados.js';
import { criarSeletor } from '../seletor-local.js';
import { $, TIPOS, escapeHtml, mostrar, vazioParaNulo } from '../util.js';

const form = $('#form-ponto');
const msg = $('#msg-ponto');
let editandoId = null;
let seletor = null;

export function iniciar() {
  seletor = criarSeletor($('[data-seletor]', form), {
    // ao escolher um lugar na busca, aproveita nome e endereço se o campo ainda estiver vazio
    aoEscolherLugar(lugar) {
      if (!form.elements.nome.value.trim()) form.elements.nome.value = lugar.nome;
      if (!form.elements.endereco.value.trim()) form.elements.endereco.value = lugar.endereco;
    },
  });
  aoMudar('dados:pontos', renderizarLista);
  form.addEventListener('submit', salvar);
  $('#cancelar-ponto').addEventListener('click', cancelarEdicao);
  $('#lista-pontos').addEventListener('click', aoClicarNaLista);
}

export const mostrarPagina = () => seletor.mostrar();

async function salvar(evento) {
  evento.preventDefault();
  const d = new FormData(form);
  const corpo = {
    nome: d.get('nome'), tipo: d.get('tipo'),
    latitude: Number(d.get('latitude')), longitude: Number(d.get('longitude')),
    endereco: vazioParaNulo(d.get('endereco')), horario: vazioParaNulo(d.get('horario')),
    descricao: vazioParaNulo(d.get('descricao')),
    gratuito: d.get('gratuito') === 'on', sombra: d.get('sombra') === 'on',
  };
  try {
    const url = editandoId ? '/api/admin/pontos/' + editandoId : '/api/admin/pontos';
    await api(url, { method: editandoId ? 'PUT' : 'POST', body: json(corpo) });
    const texto = editandoId ? 'Ponto atualizado.' : 'Ponto cadastrado.';
    cancelarEdicao(false);
    mostrar(msg, texto, true);
    await recarregarPontos();
  } catch (erro) { mostrar(msg, erro.message, false); }
}

function cancelarEdicao(limparMensagem = true) {
  editandoId = null;
  form.reset();
  form.elements.gratuito.checked = true;
  seletor.limpar();
  $('#titulo-form-ponto').textContent = 'Novo ponto de apoio';
  $('#cancelar-ponto').hidden = true;
  if (limparMensagem) mostrar(msg, '', true);
}

function renderizarLista() {
  const linhas = dados.pontos.map(p => `
    <tr>
      <td>${escapeHtml(p.nome)}</td>
      <td>${escapeHtml(TIPOS[p.tipo] || p.tipo)}</td>
      <td><span class="tag ${p.ativo ? 'resolvido' : 'off'}">${p.ativo ? 'Ativo' : 'Inativo'}</span></td>
      <td class="row-actions">
        <button type="button" class="button-light" data-acao="editar" data-id="${p.id}">Editar</button>
        <button type="button" class="button-light" data-acao="ativo" data-id="${p.id}" data-valor="${!p.ativo}">${p.ativo ? 'Desativar' : 'Ativar'}</button>
      </td>
    </tr>`);
  $('#lista-pontos').innerHTML = linhas.join('') || '<tr><td colspan="4">Nenhum ponto cadastrado.</td></tr>';
}

async function aoClicarNaLista(evento) {
  const botao = evento.target.closest('button[data-acao]');
  if (!botao) return;
  const id = Number(botao.dataset.id);
  try {
    if (botao.dataset.acao === 'editar') {
      const p = dados.pontos.find(x => x.id === id);
      if (!p) return;
      editandoId = id;
      form.elements.nome.value = p.nome;
      form.elements.tipo.value = p.tipo;
      form.elements.latitude.value = p.latitude;
      form.elements.longitude.value = p.longitude;
      form.elements.endereco.value = p.endereco || '';
      form.elements.horario.value = p.horario || '';
      form.elements.descricao.value = p.descricao || '';
      form.elements.gratuito.checked = p.gratuito;
      form.elements.sombra.checked = p.sombra;
      $('#titulo-form-ponto').textContent = 'Editando: ' + p.nome;
      $('#cancelar-ponto').hidden = false;
      seletor.atualizar();
      form.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } else {
      await api(`/api/admin/pontos/${id}/ativo?ativo=${botao.dataset.valor === 'true'}`, { method: 'PATCH' });
      await recarregarPontos();
    }
  } catch (erro) { mostrar(msg, erro.message, false); }
}
