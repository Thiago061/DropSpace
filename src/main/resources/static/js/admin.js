(function () {
  'use strict';

  const TOKEN_KEY = 'zonageo.admin.token';
  const TIPOS = {
    PONTO_AGUA: 'Ponto de água', DESCANSO_SOMBRA: 'Descanso / sombra',
    ABRIGO_FRIO: 'Abrigo (frio)', MERCADO_PARCEIRO: 'Mercado parceiro'
  };
  const CORES = { PONTO_AGUA: '#287ca5', DESCANSO_SOMBRA: '#2e9e5b', ABRIGO_FRIO: '#7b5ea7', MERCADO_PARCEIRO: '#d99a00' };
  const STATUS = ['Aberto', 'Em análise', 'Validado', 'Em atendimento', 'Resolvido', 'Falso positivo', 'Cancelado'];
  const TITULOS = { painel: 'Painel e mapa', pontos: 'Pontos de apoio', ocorrencias: 'Ocorrências', usuarios: 'Usuários e planos', vouchers: 'Vouchers' };

  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];
  const escapeHtml = v => String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const fmtData = iso => iso ? new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' }) : '—';

  let token = sessionStorage.getItem(TOKEN_KEY);
  let map, camada, marcadorClique;
  let pontosCache = [];
  let editandoId = null;

  // ------------------------------------------------------------------ API
  async function api(url, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) };
    const res = await fetch(url, { ...options, headers });
    if (res.status === 401) { sair(); throw new Error('Sessão expirada. Entre novamente.'); }
    if (res.status === 403) throw new Error('Você não tem permissão para esta ação.');
    if (!res.ok) {
      let msg = 'Erro ' + res.status;
      try { const b = await res.json(); msg = b.detail || b.message || msg; } catch (_) { /* sem corpo */ }
      throw new Error(msg);
    }
    return res.status === 204 ? null : res.json();
  }

  function mostrar(el, texto, ok) {
    el.textContent = texto;
    el.className = 'message ' + (ok ? 'success' : 'error');
  }

  // ---------------------------------------------------------------- login
  $('#form-login').addEventListener('submit', async e => {
    e.preventDefault();
    const msg = $('#msg-login');
    const dados = Object.fromEntries(new FormData(e.currentTarget));
    try {
      // login sem Authorization: um token antigo/expirado causaria 401 mesmo com senha correta
      const res = await fetch('/api/auth/login', {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(dados)
      });
      const corpo = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(corpo.detail || 'Não foi possível entrar.');
      if (corpo.usuario.papel !== 'ADMIN') throw new Error('Esta conta não tem acesso administrativo.');
      token = corpo.token;
      sessionStorage.setItem(TOKEN_KEY, token);
      $('#usuario-logado').textContent = 'Conectado como ' + corpo.usuario.login;
      e.currentTarget.reset();
      msg.textContent = '';
      entrar();
    } catch (err) { mostrar(msg, err.message, false); }
  });

  function sair() {
    token = null;
    sessionStorage.removeItem(TOKEN_KEY);
    $('#tela-painel').hidden = true;
    $('#tela-login').hidden = false;
  }
  $('#btn-sair').addEventListener('click', sair);

  function entrar() {
    $('#tela-login').hidden = true;
    $('#tela-painel').hidden = false;
    iniciarMapa();
    trocarView((location.hash || '#painel').slice(1));
    carregarTudo();
  }

  // ------------------------------------------------------------ navegação
  function trocarView(nome) {
    if (!TITULOS[nome]) nome = 'painel';
    $$('.view').forEach(v => { v.hidden = v.dataset.view !== nome; });
    $$('#menu a').forEach(a => a.classList.toggle('active', a.dataset.view === nome));
    $('#titulo-view').textContent = TITULOS[nome];
    if (nome === 'painel' && map) setTimeout(() => map.invalidateSize(), 50);
  }
  $('#menu').addEventListener('click', e => {
    const a = e.target.closest('a[data-view]');
    if (!a) return;
    e.preventDefault();
    history.replaceState(null, '', '#' + a.dataset.view);
    trocarView(a.dataset.view);
  });

  // ----------------------------------------------------------------- mapa
  function iniciarMapa() {
    if (map) return;
    map = L.map('map').setView([-15.7939, -47.8828], 5);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap contributors' }).addTo(map);
    camada = L.layerGroup().addTo(map);
    map.on('click', e => {
      const lat = Number(e.latlng.lat.toFixed(6)), lon = Number(e.latlng.lng.toFixed(6));
      $$('input[name=latitude]').forEach(i => { i.value = lat; });
      $$('input[name=longitude]').forEach(i => { i.value = lon; });
      if (marcadorClique) marcadorClique.remove();
      marcadorClique = L.circleMarker([lat, lon], { radius: 6, color: '#142b3b', fillOpacity: 0.2 }).addTo(map)
        .bindPopup('Coordenadas copiadas para os formulários').openPopup();
    });
  }

  function desenharMapa(pontos, ocorrencias) {
    camada.clearLayers();
    const limites = [];
    pontos.filter(p => p.ativo).forEach(p => {
      L.circleMarker([p.latitude, p.longitude], { radius: 9, color: CORES[p.tipo], fillColor: CORES[p.tipo], fillOpacity: 0.8 })
        .bindPopup(`<strong>${escapeHtml(p.nome)}</strong><br>${escapeHtml(TIPOS[p.tipo])}${p.voucherDescricao ? '<br>Voucher: ' + escapeHtml(p.voucherDescricao) : ''}`)
        .addTo(camada);
      limites.push([p.latitude, p.longitude]);
    });
    ocorrencias.filter(o => o.latitude != null && o.longitude != null).forEach(o => {
      L.circleMarker([o.latitude, o.longitude], { radius: 8, color: '#c0392b', weight: 2, fillOpacity: 0.25, dashArray: '3' })
        .bindPopup(`<strong>${escapeHtml(o.titulo)}</strong><br>${escapeHtml(o.categoria)} · ${escapeHtml(o.severidade)}<br>Status: ${escapeHtml(o.status)}`)
        .addTo(camada);
      limites.push([o.latitude, o.longitude]);
    });
    if (limites.length) map.fitBounds(limites, { padding: [30, 30], maxZoom: 14 });
  }

  // ---------------------------------------------------------------- dados
  async function carregarTudo() {
    try {
      const [stats, pontos, ocorrencias, usuarios, vouchers] = await Promise.all([
        api('/api/dashboard'), api('/api/admin/pontos'), api('/api/ocorrencias'),
        api('/api/admin/usuarios'), api('/api/admin/vouchers')
      ]);
      $('#aviso-global').textContent = '';
      $('#status-api').innerHTML = '<i></i> API conectada';
      $('#s-pontos').textContent = stats.pontosApoio;
      $('#s-abertas').textContent = stats.ocorrenciasAbertas;
      $('#s-usuarios').textContent = stats.usuarios;
      $('#s-pro').textContent = stats.usuariosPro;
      pontosCache = pontos;
      desenharMapa(pontos, ocorrencias);
      renderPontos(pontos);
      renderOcorrencias(ocorrencias);
      renderUsuarios(usuarios);
      renderVouchers(vouchers);
    } catch (err) {
      $('#status-api').innerHTML = '<i></i> Falha ao carregar';
      mostrar($('#aviso-global'), err.message, false);
    }
  }

  function renderPontos(lista) {
    $('#lista-pontos').innerHTML = lista.length ? lista.map(p => `
      <tr>
        <td>${escapeHtml(p.nome)}</td>
        <td>${escapeHtml(TIPOS[p.tipo] || p.tipo)}</td>
        <td>${escapeHtml(p.voucherDescricao || '—')}</td>
        <td><span class="tag ${p.ativo ? 'resolvido' : 'off'}">${p.ativo ? 'Ativo' : 'Inativo'}</span></td>
        <td class="row-actions">
          <button type="button" class="button-light" data-acao="editar" data-id="${p.id}">Editar</button>
          <button type="button" class="button-light" data-acao="ativo" data-id="${p.id}" data-valor="${!p.ativo}">${p.ativo ? 'Desativar' : 'Ativar'}</button>
        </td>
      </tr>`).join('') : '<tr><td colspan="5">Nenhum ponto cadastrado.</td></tr>';
  }

  function renderOcorrencias(lista) {
    const ordenadas = lista.slice().sort((a, b) => b.id - a.id).slice(0, 50);
    $('#lista-ocorrencias').innerHTML = ordenadas.length ? ordenadas.map(o => `
      <tr>
        <td>${escapeHtml(o.titulo)}<br><small>${escapeHtml(o.categoria)}</small></td>
        <td><span class="tag ${o.severidade === 'Crítico' ? 'critical' : o.severidade === 'Alto' ? 'alto' : ''}">${escapeHtml(o.severidade)}</span></td>
        <td><select data-acao="status" data-id="${o.id}" aria-label="Status de ${escapeHtml(o.titulo)}">
          ${STATUS.map(s => `<option${s === o.status ? ' selected' : ''}>${s}</option>`).join('')}</select></td>
      </tr>`).join('') : '<tr><td colspan="3">Nenhuma ocorrência cadastrada.</td></tr>';
  }

  function renderUsuarios(lista) {
    $('#lista-usuarios').innerHTML = lista.length ? lista.map(u => `
      <tr>
        <td>${escapeHtml(u.nome)}</td>
        <td>${escapeHtml(u.login)}</td>
        <td><span class="tag ${u.plano === 'PRO' ? 'pro' : ''}">${escapeHtml(u.plano)}</span></td>
        <td>${u.plano === 'PRO' ? fmtData(u.planoExpiraEm) : '—'}</td>
        <td class="row-actions">
          <button type="button" class="button-light" data-acao="pro" data-id="${u.id}">Dar Pro</button>
          ${u.plano === 'PRO' ? `<button type="button" class="button-light" data-acao="free" data-id="${u.id}">Voltar ao Free</button>` : ''}
        </td>
      </tr>`).join('') : '<tr><td colspan="5">Nenhum usuário cadastrado ainda.</td></tr>';
  }

  function renderVouchers(lista) {
    $('#lista-vouchers').innerHTML = lista.length ? lista.map(v => `
      <tr>
        <td><code>${escapeHtml(v.codigo)}</code></td>
        <td>${escapeHtml(v.pontoNome)}</td>
        <td>${escapeHtml(v.descricao)}</td>
        <td><span class="tag ${v.status === 'RESGATADO' ? 'resolvido' : v.status === 'EXPIRADO' ? 'off' : ''}">${escapeHtml(v.status)}</span><br><small>${fmtData(v.emitidoEm)}</small></td>
      </tr>`).join('') : '<tr><td colspan="4">Nenhum voucher emitido ainda.</td></tr>';
  }

  // -------------------------------------------------------- pontos (form)
  function vazioParaNulo(v) { return v === '' ? null : v; }

  $('#form-ponto').addEventListener('submit', async e => {
    e.preventDefault();
    const f = e.currentTarget, d = new FormData(f), msg = $('#msg-ponto');
    const corpo = {
      nome: d.get('nome'), tipo: d.get('tipo'),
      latitude: Number(d.get('latitude')), longitude: Number(d.get('longitude')),
      endereco: vazioParaNulo(d.get('endereco')), horario: vazioParaNulo(d.get('horario')),
      descricao: vazioParaNulo(d.get('descricao')), voucherDescricao: vazioParaNulo(d.get('voucherDescricao')),
      gratuito: d.get('gratuito') === 'on', sombra: d.get('sombra') === 'on'
    };
    try {
      await api(editandoId ? '/api/admin/pontos/' + editandoId : '/api/admin/pontos',
        { method: editandoId ? 'PUT' : 'POST', body: JSON.stringify(corpo) });
      mostrar(msg, editandoId ? 'Ponto atualizado.' : 'Ponto cadastrado.', true);
      cancelarEdicao(false);
      await carregarTudo();
    } catch (err) { mostrar(msg, err.message, false); }
  });

  function cancelarEdicao(limparMensagem = true) {
    editandoId = null;
    $('#form-ponto').reset();
    $('#form-ponto [name=gratuito]').checked = true;
    $('#titulo-form-ponto').textContent = 'Novo ponto de apoio';
    $('#cancelar-ponto').hidden = true;
    if (limparMensagem) $('#msg-ponto').textContent = '';
  }
  $('#cancelar-ponto').addEventListener('click', () => cancelarEdicao());

  $('#lista-pontos').addEventListener('click', async e => {
    const b = e.target.closest('button[data-acao]');
    if (!b) return;
    const id = Number(b.dataset.id);
    try {
      if (b.dataset.acao === 'editar') {
        const p = pontosCache.find(x => x.id === id);
        if (!p) return;
        const f = $('#form-ponto');
        editandoId = id;
        f.nome.value = p.nome; f.tipo.value = p.tipo;
        f.latitude.value = p.latitude; f.longitude.value = p.longitude;
        f.endereco.value = p.endereco || ''; f.horario.value = p.horario || '';
        f.descricao.value = p.descricao || ''; f.voucherDescricao.value = p.voucherDescricao || '';
        f.gratuito.checked = p.gratuito; f.sombra.checked = p.sombra;
        $('#titulo-form-ponto').textContent = 'Editando: ' + p.nome;
        $('#cancelar-ponto').hidden = false;
        f.scrollIntoView({ behavior: 'smooth', block: 'start' });
      } else if (b.dataset.acao === 'ativo') {
        await api(`/api/admin/pontos/${id}/ativo?ativo=${b.dataset.valor === 'true'}`, { method: 'PATCH' });
        await carregarTudo();
      }
    } catch (err) { mostrar($('#aviso-global'), err.message, false); }
  });

  // ---------------------------------------------------- ocorrências (form)
  $('#form-ocorrencia').addEventListener('submit', async e => {
    e.preventDefault();
    const f = e.currentTarget, d = Object.fromEntries(new FormData(f)), msg = $('#msg-ocorrencia');
    try {
      await api('/api/ocorrencias', { method: 'POST', body: JSON.stringify({
        ...d, latitude: Number(d.latitude), longitude: Number(d.longitude), status: 'Aberto' }) });
      mostrar(msg, 'Ocorrência registrada.', true);
      f.reset();
      await carregarTudo();
    } catch (err) { mostrar(msg, err.message, false); }
  });

  $('#lista-ocorrencias').addEventListener('change', async e => {
    const s = e.target.closest('select[data-acao=status]');
    if (!s) return;
    try {
      await api(`/api/ocorrencias/${s.dataset.id}/status`, { method: 'PATCH', body: JSON.stringify({ status: s.value }) });
      await carregarTudo();
    } catch (err) { mostrar($('#aviso-global'), err.message, false); await carregarTudo(); }
  });

  // ------------------------------------------------------------- usuários
  $('#lista-usuarios').addEventListener('click', async e => {
    const b = e.target.closest('button[data-acao]');
    if (!b) return;
    const dias = Number($('#dias-pro').value) || 30;
    const corpo = b.dataset.acao === 'pro' ? { plano: 'PRO', dias } : { plano: 'FREE' };
    try {
      await api(`/api/admin/usuarios/${b.dataset.id}/plano`, { method: 'PATCH', body: JSON.stringify(corpo) });
      await carregarTudo();
    } catch (err) { mostrar($('#aviso-global'), err.message, false); }
  });

  // ------------------------------------------------------------- vouchers
  $('#form-voucher').addEventListener('submit', async e => {
    e.preventDefault();
    const f = e.currentTarget, msg = $('#msg-voucher');
    try {
      const v = await api('/api/admin/vouchers/resgatar', { method: 'POST', body: JSON.stringify({ codigo: f.codigo.value }) });
      mostrar(msg, `Resgatado: ${v.descricao} (${v.pontoNome}).`, true);
      f.reset();
      await carregarTudo();
    } catch (err) { mostrar(msg, err.message, false); }
  });

  // ------------------------------------------------------------------ boot
  window.addEventListener('hashchange', () => { if (!$('#tela-painel').hidden) trocarView(location.hash.slice(1)); });
  if (token) {
    api('/api/dashboard').then(() => entrar()).catch(() => sair());
  }
})();
