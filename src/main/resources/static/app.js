const $ = s => document.querySelector(s);
const brl = n => Number(n).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
const esc = s => String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const ICON = { pc: '🖥️', notebook: '💻', celular: '📱', console: '🎮', acessorio: '🎧' };
const CAT = { pc: 'PC', notebook: 'Notebook', celular: 'Celular', console: 'Console', acessorio: 'Acessório' };

let products = [], category = '', query = '', register = false;
let cart = JSON.parse(localStorage.getItem('ds_cart') || '{}');   // { idProduto: quantidade }
let session = JSON.parse(localStorage.getItem('ds_session') || 'null'); // { token, name }

async function api(path, opts = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (session) headers.Authorization = 'Bearer ' + session.token;
  const res = await fetch('/api' + path, { ...opts, headers });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.message || 'Algo deu errado. Tente novamente.');
  return data;
}

/* ---------- Vitrine ---------- */
async function loadProducts() {
  $('#status').textContent = 'Carregando produtos...';
  try {
    products = await api(`/products?q=${encodeURIComponent(query)}&category=${category}`);
    renderProducts();
  } catch (e) { $('#status').textContent = 'Não foi possível carregar os produtos. ' + e.message; }
}

function renderProducts() {
  const sort = $('#sort').value;
  const list = [...products].sort((a, b) => sort === 'asc' ? a.price - b.price : sort === 'desc' ? b.price - a.price : 0);
  $('#status').textContent = list.length ? `${list.length} produto(s) encontrado(s)` : 'Nenhum produto encontrado. Tente outra busca.';
  $('#grid').innerHTML = list.map(p => {
    const off = p.oldPrice ? Math.round((1 - p.price / p.oldPrice) * 100) : 0;
    const stars = '★'.repeat(Math.round(p.rating)) + '☆'.repeat(5 - Math.round(p.rating));
    return `<article class="card">
      ${off ? `<span class="card__off">${off}% OFF</span>` : ''}
      <div class="card__img" role="img" aria-label="${esc(CAT[p.category])}: ${esc(p.name)}">${ICON[p.category] || '📦'}</div>
      <h3>${esc(p.name)}</h3>
      <div class="stars" aria-label="Nota ${p.rating} de 5">${stars} <span>(${p.reviews})</span></div>
      <div class="old">${p.oldPrice ? 'De ' + brl(p.oldPrice) : ''}</div>
      <div class="price">${brl(p.price)} <small>à vista no Pix</small></div>
      <div class="inst">ou 10x de ${brl(p.price / 10)} sem juros</div>
      <div class="ship">${p.price >= 3000 ? 'Frete grátis' : 'Calcular frete no carrinho'}</div>
      <button class="btn btn--accent" data-add="${p.id}" ${p.stock < 1 ? 'disabled' : ''}>${p.stock < 1 ? 'Indisponível' : 'Adicionar ao carrinho'}</button>
    </article>`;
  }).join('');
}

/* ---------- Carrinho ---------- */
const saveCart = () => { localStorage.setItem('ds_cart', JSON.stringify(cart)); renderCart(); };
function changeQty(id, d) {
  const p = products.find(x => x.id == id);
  const next = (cart[id] || 0) + d;
  if (next <= 0) delete cart[id]; else if (!p || next <= p.stock) cart[id] = next;
  saveCart();
}
async function renderCart() {
  const ids = Object.keys(cart);
  $('#cartCount').textContent = ids.reduce((s, i) => s + cart[i], 0);
  let total = 0;
  const rows = [];
  for (const id of ids) {
    let p = products.find(x => x.id == id);
    if (!p) { try { p = await api('/products/' + id); } catch { delete cart[id]; continue; } }
    total += p.price * cart[id];
    rows.push(`<div class="line"><span>${esc(p.name)}<br><strong>${brl(p.price)}</strong></span>
      <span><button data-dec="${id}" aria-label="Diminuir">−</button> ${cart[id]} <button data-inc="${id}" aria-label="Aumentar">+</button></span></div>`);
  }
  $('#cartItems').innerHTML = rows.join('') || '<p>Seu carrinho está vazio. Que tal ver as ofertas?</p>';
  $('#cartTotal').textContent = brl(total);
}
async function checkout() {
  const msg = $('#cartMsg'); msg.className = '';
  if (!session) { msg.textContent = 'Entre na sua conta para finalizar a compra.'; $('#cartDlg').close(); openAuth(); return; }
  const items = Object.entries(cart).map(([productId, quantity]) => ({ productId: +productId, quantity }));
  $('#checkoutBtn').disabled = true;
  try {
    const order = await api('/orders', { method: 'POST', body: JSON.stringify({ items }) });
    cart = {}; saveCart(); loadProducts();
    msg.className = 'ok'; msg.textContent = `Pedido #${order.id} criado! Total ${brl(order.total)}.`;
  } catch (e) { msg.textContent = e.message; }
  $('#checkoutBtn').disabled = false;
}

/* ---------- Conta ---------- */
function updateAccount() { $('#accountLabel').textContent = session ? session.name.split(' ')[0] : 'Entrar'; }
function openAuth() {
  register = false; setAuthMode(); $('#authMsg').textContent = ''; $('#authDlg').showModal();
}
function setAuthMode() {
  $('#nameRow').hidden = !register;
  $('#authTitle').textContent = $('#authSubmit').textContent = register ? 'Criar conta' : 'Entrar';
  $('#authToggle').textContent = register ? 'Já tem conta? Entrar' : 'Não tem conta? Cadastre-se';
}
async function openAccount() {
  if (!session) return openAuth();
  $('#ordersList').innerHTML = '<p>Carregando pedidos...</p>'; $('#ordersDlg').showModal();
  try {
    const list = await api('/orders');
    $('#ordersList').innerHTML = `<p>Olá, <strong>${esc(session.name)}</strong>!</p>` + (list.length ? list.map(o =>
      `<div class="line"><span>Pedido #${o.id} · ${new Date(o.createdAt).toLocaleDateString('pt-BR')}<br>${o.items.length} item(ns) · ${o.status.replaceAll('_', ' ')}</span><strong>${brl(o.total)}</strong></div>`).join('')
      : '<p>Você ainda não fez nenhum pedido.</p>');
  } catch (e) { logout(); }
}
function logout() { session = null; localStorage.removeItem('ds_session'); updateAccount(); $('#ordersDlg').close(); }

/* ---------- Eventos ---------- */
$('#searchForm').addEventListener('submit', e => { e.preventDefault(); query = $('#q').value; loadProducts(); });
$('#nav').addEventListener('click', e => {
  if (!e.target.dataset) return;
  category = e.target.dataset.cat ?? category;
  document.querySelectorAll('#nav button').forEach(b => b.classList.toggle('active', b === e.target));
  $('#titulo').textContent = e.target.textContent === 'Tudo' ? 'Todos os produtos' : e.target.textContent;
  loadProducts();
});
$('#sort').addEventListener('change', renderProducts);
$('#grid').addEventListener('click', e => { const id = e.target.dataset.add; if (id) { changeQty(id, 1); $('#cartBtn').focus(); } });
$('#cartItems').addEventListener('click', e => { const t = e.target.dataset; if (t.inc) changeQty(t.inc, 1); if (t.dec) changeQty(t.dec, -1); });
$('#cartBtn').onclick = () => { $('#cartMsg').textContent = ''; renderCart(); $('#cartDlg').showModal(); };
$('#checkoutBtn').onclick = checkout;
$('#accountBtn').onclick = openAccount;
$('#logoutBtn').onclick = logout;
$('#authToggle').onclick = () => { register = !register; setAuthMode(); };
document.querySelectorAll('[data-close]').forEach(b => b.onclick = () => b.closest('dialog').close());
$('#authForm').addEventListener('submit', async e => {
  e.preventDefault();
  const body = { name: $('#name').value, email: $('#email').value, password: $('#password').value };
  try {
    session = await api(register ? '/auth/register' : '/auth/login', { method: 'POST', body: JSON.stringify(body) });
    localStorage.setItem('ds_session', JSON.stringify(session));
    updateAccount(); $('#authDlg').close(); $('#authForm').reset();
  } catch (err) { $('#authMsg').textContent = err.message; }
});

updateAccount(); loadProducts().then(renderCart);

/* ---------- Aprenda (vídeos) ---------- */
async function loadVideos() {
  try {
    const list = await api('/videos');
    $('#videoStatus').textContent = list.length ? '' : 'Em breve, novos vídeos.';
    $('#videos').innerHTML = list.map(v => `<article class="video">
      <div class="video__player">
        <button class="video__play" data-yt="${esc(v.youtubeId)}" data-title="${esc(v.title)}"
          style="background-image:url(https://i.ytimg.com/vi/${encodeURIComponent(v.youtubeId)}/hqdefault.jpg)"
          aria-label="Reproduzir vídeo: ${esc(v.title)}"><span>▶ Assistir</span></button>
      </div>
      <div class="video__body">
        <h3>${esc(v.title)}</h3>
        <p>${esc(v.summary)}</p>
        <p>Canal: ${esc(v.channel)}</p>
        <a class="btn" href="#produtos" data-goto="${esc(v.shopCategory)}">Ver ${esc(CAT[v.shopCategory] || 'produtos')}s na loja</a>
      </div></article>`).join('');
  } catch (e) { $('#videoStatus').textContent = 'Não foi possível carregar os vídeos agora.'; }
}
// O player só é carregado ao clicar: página mais rápida e menos rastreamento (youtube-nocookie).
$('#videos').addEventListener('click', e => {
  const play = e.target.closest('[data-yt]');
  if (play) {
    play.parentElement.innerHTML = `<iframe src="https://www.youtube-nocookie.com/embed/${encodeURIComponent(play.dataset.yt)}?autoplay=1&rel=0"
      title="${play.dataset.title}" allow="accelerometer; autoplay; encrypted-media; picture-in-picture" allowfullscreen referrerpolicy="strict-origin-when-cross-origin"></iframe>`;
    return;
  }
  const go = e.target.closest('[data-goto]');
  if (go) document.querySelector(`#nav [data-cat="${go.dataset.goto}"]`)?.click();
});
loadVideos();
