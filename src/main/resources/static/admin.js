layout('admin');
let products = [], editing = null;

async function load() {
  try {
    const [p, m, s] = await Promise.all([api('/admin/products'), api('/admin/movements'), api('/admin/summary'), categoriesReady]);
    products = p; $('#status').textContent = ''; $('#adminBox').hidden = false;
    renderStats(s); renderCategories(); renderProducts(); renderMovements(m);
  } catch (e) {
    $('#status').innerHTML = ''; $('#status').append(e.message + ' · ', Object.assign(document.createElement('a'), { href: 'conta.html', textContent: 'Entrar como administrador' }));
  }
}
function renderStats(s) {
  const rows = [['Produtos', s.products], ['Categorias', s.categories], ['Unidades em estoque', s.units], ['Estoque baixo (≤3)', s.lowStock],
    ['Valor em estoque', brl(s.stockValue)], ['Clientes', s.customers], ['Pedidos', s.orders], ['Vendas (total)', brl(s.revenue)]];
  fill($('#stats'), 'tplStat', rows.map(([label, value]) => ({ label, value })));
}
function renderCategories() {
  fill($('#catList'), 'tplCatRow', categories.map(c => ({ slug: c.slug, label: `${c.icon} ${c.name}` })));
  $('#pCat').replaceChildren(...categories.map(c => new Option(c.name, c.slug)));
}
function renderProducts() {
  $('#sProd').replaceChildren(...products.map(p => new Option(`${p.name} (estoque ${p.stock})`, p.id)));
  fill($('#pBody'), 'tplProdRow', products.map(p => ({
    id: p.id, image: p.imageUrl, noImage: !p.imageUrl, name: p.name, meta: `${CAT[p.category] || p.category} · ${p.brand} · ${p.sku || 'sem SKU'}`,
    price: brl(p.price), oldPrice: p.oldPrice ? brl(p.oldPrice) : '', stock: p.stock + (p.stock <= 3 ? ' (baixo)' : ''), stockClass: p.stock <= 3 ? 'err' : '' })));
}
const renderMovements = m => fill($('#mBody'), 'tplMoveRow', m.map(x => ({
  date: new Date(x.createdAt).toLocaleString('pt-BR'), product: x.productName, type: x.type, qty: (x.quantity > 0 ? '+' : '') + x.quantity, balance: x.balanceAfter, note: x.note || '' })));
async function refreshCategories() { categories = await api('/categories'); applyCategories(categories); store.set('ds_cats', categories); renderCategories(); }

function resetForm() {
  editing = null; $('#pForm').reset(); $('#fTitle').textContent = 'Cadastrar produto'; $('#pStockRow').hidden = false; $('#pCancel').hidden = true;
}
$('#pBody').addEventListener('click', async e => {
  const t = e.target.dataset, msg = $('#pMsg');
  if (t.edit) {
    const p = products.find(x => x.id == t.edit); editing = p.id;
    $('#pName').value = p.name; $('#pCat').value = p.category; $('#pBrand').value = p.brand; $('#pSku').value = p.sku || '';
    $('#pPrice').value = p.price; $('#pOld').value = p.oldPrice || '';
    $('#fTitle').textContent = 'Editar produto'; $('#pStockRow').hidden = true; $('#pCancel').hidden = false;
    $('#pName').focus(); msg.className = ''; msg.textContent = 'O estoque é alterado em "Movimentar estoque".';
  }
  if (t.del) {
    const p = products.find(x => x.id == t.del);
    if (!confirm(`Excluir "${p.name}"? Esta ação não pode ser desfeita.`)) return;
    try { await api('/admin/products/' + p.id, { method: 'DELETE' }); msg.className = ''; msg.textContent = 'Produto excluído.'; await load(); }
    catch (err) { msg.className = 'err'; msg.textContent = err.message; }
  }
});
$('#pCancel').onclick = resetForm;
$('#pForm').addEventListener('submit', async e => {
  e.preventDefault(); const msg = $('#pMsg'); msg.className = ''; $('#pSave').disabled = true;
  const body = { name: $('#pName').value, category: $('#pCat').value, brand: $('#pBrand').value, sku: $('#pSku').value,
    price: $('#pPrice').value ? Number($('#pPrice').value) : null, oldPrice: $('#pOld').value ? Number($('#pOld').value) : null, stock: Number($('#pStock').value || 0) };
  try {
    const saved = editing ? await api('/admin/products/' + editing, { method: 'PUT', body }) : await api('/admin/products', { method: 'POST', body });
    const file = $('#pImg').files[0];
    if (file) { const fd = new FormData(); fd.append('file', file); await api(`/admin/products/${saved.id}/image`, { method: 'POST', body: fd }); }
    resetForm(); msg.textContent = 'Produto salvo com sucesso.'; await load();
  } catch (err) { msg.className = 'err'; msg.textContent = err.message; }
  $('#pSave').disabled = false;
});
$('#sForm').addEventListener('submit', async e => {
  e.preventDefault(); const msg = $('#sMsg'); msg.className = '';
  try {
    await api(`/admin/products/${$('#sProd').value}/stock`, { method: 'POST', body: { type: $('#sType').value, quantity: Number($('#sQty').value), note: $('#sNote').value } });
    $('#sForm').reset(); msg.textContent = 'Movimentação registrada.'; await load();
  } catch (err) { msg.className = 'err'; msg.textContent = err.message; }
});
$('#cForm').addEventListener('submit', async e => {
  e.preventDefault(); const msg = $('#cMsg'); msg.className = '';
  try { await api('/admin/categories', { method: 'POST', body: { name: $('#cName').value, icon: $('#cIcon').value } }); $('#cForm').reset(); msg.textContent = 'Categoria criada.'; await refreshCategories(); }
  catch (err) { msg.className = 'err'; msg.textContent = err.message; }
});
$('#catList').addEventListener('click', async e => {
  const t = e.target.dataset, msg = $('#cMsg'); msg.className = '';
  try {
    if (t.catDel) { if (!confirm('Excluir esta categoria?')) return; await api('/admin/categories/' + t.catDel, { method: 'DELETE' }); msg.textContent = 'Categoria excluída.'; }
    if (t.catEdit) {
      const c = categories.find(x => x.slug === t.catEdit), name = prompt('Novo nome da categoria:', c.name); if (!name) return;
      await api('/admin/categories/' + c.slug, { method: 'PUT', body: { name, icon: c.icon } }); msg.textContent = 'Categoria atualizada.';
    }
    if (t.catDel || t.catEdit) await refreshCategories();
  } catch (err) { msg.className = 'err'; msg.textContent = err.message; }
});
load();
