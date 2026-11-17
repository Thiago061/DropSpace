/* Código compartilhado por todas as páginas: utilitários, API, templates HTML, validações e layout. */
/* Navegação em um só lugar (facilita testar). */
function go(url) { location.href = url; }
const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];
const brl = n => Number(n).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
const digits = s => String(s ?? '').replace(/\D/g, '');

const store = {
  get(k, d) { try { return JSON.parse(localStorage.getItem(k)) ?? d; } catch { return d; } },
  set(k, v) { localStorage.setItem(k, JSON.stringify(v)); },
};
let session = store.get('ds_session', null); // { token, name, role }
function saveSession(s) { session = s; s ? store.set('ds_session', s) : localStorage.removeItem('ds_session'); }

async function api(path, opts = {}) {
  const headers = {};
  if (opts.body && !(opts.body instanceof FormData)) { headers['Content-Type'] = 'application/json'; opts = { ...opts, body: JSON.stringify(opts.body) }; }
  if (session) headers.Authorization = 'Bearer ' + session.token;
  let res;
  try { res = await fetch('/api' + path, { ...opts, headers }); } catch { throw new Error('Sem conexão com o servidor.'); }
  const data = await res.json().catch(() => ({}));
  if (res.status === 401 && session && path !== '/auth/login') saveSession(null);
  if (!res.ok) { const e = new Error(data.message || 'Algo deu errado. Tente novamente.'); e.status = res.status; throw e; }
  return data;
}

/* ---------- Categorias (uma fonte só: o banco) ---------- */
let categories = store.get('ds_cats', []);
const CAT = {}, ICON = {};
const applyCategories = list => { categories = list; list.forEach(c => { CAT[c.slug] = c.name; ICON[c.slug] = c.icon; }); };
applyCategories(categories);
const categoriesReady = api('/categories').then(list => { applyCategories(list); store.set('ds_cats', list); return list; }).catch(() => categories);

/* ---------- Templates HTML: o HTML fica nas páginas, o JS só preenche os dados ---------- */
// data-bind="prop:chave;prop2:chave2". prop: text | hidden (esconde se vazio) | disabled | qualquer atributo.
function fromTemplate(id, data) {
  const node = document.getElementById(id).content.cloneNode(true);
  $$('[data-bind]', node).forEach(el => el.dataset.bind.split(';').forEach(pair => {
    const [prop, key] = pair.split(':').map(s => s.trim()), v = data[key];
    if (prop === 'text') el.textContent = v ?? '';
    else if (prop === 'hidden') el.hidden = !v;
    else if (prop === 'disabled') el.disabled = !!v;
    else if (v == null || v === '' || v === false) el.removeAttribute(prop);
    else el.setAttribute(prop, v);
  }));
  return node;
}
const fill = (container, templateId, list) => { container.replaceChildren(...list.map(d => fromTemplate(templateId, d))); };

/* ---------- Validações (as mesmas regras existem no servidor) ---------- */
const isEmail = v => /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(v);
function isCpf(raw) {
  const d = digits(raw);
  if (d.length !== 11 || /^(\d)\1+$/.test(d)) return false;
  const check = len => { let s = 0; for (let i = 0; i < len; i++) s += +d[i] * (len + 1 - i); const r = (s * 10) % 11; return r === 10 ? 0 : r; };
  return check(9) === +d[9] && check(10) === +d[10];
}
const maskCpf = v => { const d = digits(v).slice(0, 11); return d.replace(/(\d{3})(\d)/, '$1.$2').replace(/(\d{3})(\d)/, '$1.$2').replace(/(\d{3})(\d{1,2})$/, '$1-$2'); };
const maskCep = v => { const d = digits(v).slice(0, 8); return d.length > 5 ? d.slice(0, 5) + '-' + d.slice(5) : d; };
function setError(id, msg) { // mostra a mensagem logo abaixo do campo e avisa leitores de tela
  const input = $('#' + id), out = $(`#${id}-err`);
  if (out) out.textContent = msg || '';
  if (input) { input.setAttribute('aria-invalid', msg ? 'true' : 'false'); input.classList.toggle('invalid', !!msg); }
  return !msg;
}
function bindMask(id, fn) { $('#' + id).addEventListener('input', e => { e.target.value = fn(e.target.value); }); }

/* Campo de senha: botão mostrar/ocultar e, opcionalmente, bloqueio de copiar/colar (data-noclipboard). */
function bindPasswordFields() {
  $$('.pw').forEach(box => {
    const input = $('input', box), btn = $('.pw__toggle', box);
    btn.addEventListener('click', () => {
      const show = input.type === 'password';
      input.type = show ? 'text' : 'password';
      btn.setAttribute('aria-pressed', show); btn.setAttribute('aria-label', show ? 'Ocultar senha' : 'Mostrar senha');
      btn.textContent = show ? '🙈' : '👁';
    });
    if ('noclipboard' in input.dataset)
      ['copy', 'cut', 'paste', 'drop'].forEach(ev => input.addEventListener(ev, e => e.preventDefault()));
  });
}

/* ---------- Carrinho ---------- */
const getCart = () => store.get('ds_cart', {}); // { idProduto: quantidade }
const setCart = c => { store.set('ds_cart', c); updateBadge(); };
const updateBadge = () => { const b = $('#cartCount'); if (b) b.textContent = Object.values(getCart()).reduce((a, n) => a + n, 0); };

/* ---------- Layout: cabeçalho e rodapé vêm de partials/*.html ---------- */
function markNav(key) { // destaca em laranja o item da página/categoria atual
  $$('#app-header [data-nav]').forEach(a => a.dataset.nav === key ? a.setAttribute('aria-current', 'page') : a.removeAttribute('aria-current'));
}
function renderNav(key) {
  fill($('#navCats'), 'tplNavCat', categories.map(c => ({ href: 'index.html?cat=' + encodeURIComponent(c.slug), label: c.name, key: 'cat:' + c.slug })));
  markNav(key);
}
async function layout(active) {
  const [h, f] = await Promise.all(['header', 'footer'].map(n => fetch(`partials/${n}.html`).then(r => r.text())));
  $('#app-header').innerHTML = h; $('#app-footer').innerHTML = f;
  // Na loja, a categoria vem da URL; por isso o destaque sobrevive a atualizar a página.
  const cat = new URLSearchParams(location.search).get('cat');
  const key = active === 'loja' && cat ? 'cat:' + cat : active;
  if (session) $('#accountLabel').textContent = session.name.split(' ')[0];
  $('#adminLink').hidden = !(session && session.role === 'ADMIN');
  $('#searchForm').addEventListener('submit', e => { e.preventDefault(); go('index.html?q=' + encodeURIComponent($('#q').value)); });
  updateBadge(); renderNav(key);
  categoriesReady.then(() => renderNav(key));
}
