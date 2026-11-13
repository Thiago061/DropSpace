// Página "Vouchers": cadastro das ofertas dos mercados parceiros, conferência no caixa e histórico.
import { api, json } from '../api.js';
import { dados, aoMudar, recarregarOfertas } from '../dados.js';
import { $, escapeHtml, fmtData, mostrar, vazioParaNulo } from '../util.js';

const formOferta = $('#form-oferta');
const msgOferta = $('#msg-oferta');
const formResgate = $('#form-resgate');
const msgResgate = $('#msg-resgate');
let editandoId = null;
let catalogoProdutos = [];

export function iniciar() {
  aoMudar('dados:pontos', preencherMercados);
  aoMudar('dados:ofertas', renderizarOfertas);
  formOferta.addEventListener('submit', salvarOferta);
  formOferta.elements.produtoCatalogo.addEventListener('change', preencherProduto);
  carregarCatalogoProdutos();
  $('#cancelar-oferta').addEventListener('click', () => cancelarEdicao());
  $('#lista-ofertas').addEventListener('click', aoClicarNasOfertas);
  formResgate.addEventListener('submit', resgatar);
}

async function carregarCatalogoProdutos() {
  try {
    catalogoProdutos = await api('/api/admin/ofertas/produtos');
    const seletor = formOferta.elements.produtoCatalogo;
    seletor.innerHTML = '<option value="">Escolha um produto</option>' + catalogoProdutos
      .map((p, i) => `<option value="${i}">${escapeHtml(p.nome)} — ${escapeHtml(p.unidade)}</option>`).join('');
  } catch (erro) { mostrar(msgOferta, erro.message, false); }
}

function preencherProduto() {
  const produto = catalogoProdutos[Number(formOferta.elements.produtoCatalogo.value)];
  if (!produto) return;
  formOferta.elements.titulo.value = produto.nome;
  formOferta.elements.unidade.value = produto.unidade;
  formOferta.elements.valor.value = Number(produto.valorSugerido).toFixed(2);
  formOferta.elements.descricao.value = produto.descricao;
}

export async function carregarEmitidos() {
  const lista = await api('/api/admin/vouchers');
  $('#lista-emitidos').innerHTML = lista.map(v => `
    <tr>
      <td><code>${escapeHtml(v.codigo)}</code></td>
      <td>${escapeHtml(v.pontoNome)}</td>
      <td>${escapeHtml(v.descricao)}</td>
      <td><span class="tag ${v.status === 'RESGATADO' ? 'resolvido' : v.status === 'EXPIRADO' ? 'off' : ''}">${escapeHtml(v.status)}</span>
        <br><small>${fmtData(v.emitidoEm)}</small></td>
    </tr>`).join('') || '<tr><td colspan="4">Nenhum voucher emitido ainda.</td></tr>';
}

function preencherMercados() {
  const mercados = dados.pontos.filter(p => p.tipo === 'MERCADO_PARCEIRO');
  const atual = formOferta.elements.pontoId.value;
  formOferta.elements.pontoId.innerHTML = mercados.length
    ? mercados.map(m => `<option value="${m.id}">${escapeHtml(m.nome)}${m.ativo ? '' : ' (inativo)'}</option>`).join('')
    : '<option value="">Cadastre antes um ponto do tipo "Mercado parceiro"</option>';
  if (atual) formOferta.elements.pontoId.value = atual;
}

async function salvarOferta(evento) {
  evento.preventDefault();
  if (!formOferta.elements.pontoId.value) { mostrar(msgOferta, 'Escolha um mercado parceiro.', false); return; }
  const corpo = {
    pontoId: Number(formOferta.elements.pontoId.value),
    titulo: formOferta.elements.titulo.value,
    descricao: vazioParaNulo(formOferta.elements.descricao.value),
    unidade: formOferta.elements.unidade.value,
    valor: Number(formOferta.elements.valor.value),
  };
  try {
    const url = editandoId ? '/api/admin/ofertas/' + editandoId : '/api/admin/ofertas';
    await api(url, { method: editandoId ? 'PUT' : 'POST', body: json(corpo) });
    const texto = editandoId ? 'Oferta atualizada.' : 'Oferta cadastrada.';
    cancelarEdicao(false);
    mostrar(msgOferta, texto, true);
    await recarregarOfertas();
  } catch (erro) { mostrar(msgOferta, erro.message, false); }
}

function cancelarEdicao(limparMensagem = true) {
  editandoId = null;
  formOferta.reset();
  $('#titulo-form-oferta').textContent = 'Nova oferta de voucher';
  $('#cancelar-oferta').hidden = true;
  if (limparMensagem) mostrar(msgOferta, '', true);
}

function renderizarOfertas() {
  $('#lista-ofertas').innerHTML = dados.ofertas.map(o => `
    <tr>
      <td>${escapeHtml(o.pontoNome)}</td>
      <td>${escapeHtml(o.titulo)}${o.descricao ? `<br><small>${escapeHtml(o.descricao)}</small>` : ''}</td>
      <td>${escapeHtml(o.unidade || '—')}</td>
      <td>${o.valor == null ? '—' : Number(o.valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}</td>
      <td><span class="tag ${o.ativo ? 'resolvido' : 'off'}">${o.ativo ? 'Ativa' : 'Inativa'}</span></td>
      <td class="row-actions">
        <button type="button" class="button-light" data-acao="editar" data-id="${o.id}">Editar</button>
        <button type="button" class="button-light" data-acao="ativo" data-id="${o.id}" data-valor="${!o.ativo}">${o.ativo ? 'Desativar' : 'Ativar'}</button>
      </td>
    </tr>`).join('') || '<tr><td colspan="6">Nenhuma oferta cadastrada.</td></tr>';
}

async function aoClicarNasOfertas(evento) {
  const botao = evento.target.closest('button[data-acao]');
  if (!botao) return;
  const id = Number(botao.dataset.id);
  try {
    if (botao.dataset.acao === 'editar') {
      const o = dados.ofertas.find(x => x.id === id);
      if (!o) return;
      editandoId = id;
      formOferta.elements.pontoId.value = String(o.pontoId);
      const produtoIndex = catalogoProdutos.findIndex(p => p.nome === o.titulo);
      formOferta.elements.produtoCatalogo.value = produtoIndex < 0 ? '' : String(produtoIndex);
      formOferta.elements.titulo.value = o.titulo;
      formOferta.elements.descricao.value = o.descricao || '';
      formOferta.elements.unidade.value = o.unidade || '';
      formOferta.elements.valor.value = o.valor == null ? '' : Number(o.valor).toFixed(2);
      $('#titulo-form-oferta').textContent = 'Editando oferta';
      $('#cancelar-oferta').hidden = false;
      formOferta.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } else {
      await api(`/api/admin/ofertas/${id}/ativo?ativo=${botao.dataset.valor === 'true'}`, { method: 'PATCH' });
      await recarregarOfertas();
    }
  } catch (erro) { mostrar(msgOferta, erro.message, false); }
}

async function resgatar(evento) {
  evento.preventDefault();
  try {
    const v = await api('/api/admin/vouchers/resgatar', { method: 'POST', body: json({ codigo: formResgate.elements.codigo.value }) });
    mostrar(msgResgate, `Resgatado: ${v.descricao} (${v.pontoNome}).`, true);
    formResgate.reset();
    await carregarEmitidos();
  } catch (erro) { mostrar(msgResgate, erro.message, false); }
}
