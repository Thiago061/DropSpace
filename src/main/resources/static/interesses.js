layout('conta');
if (!session) go('conta.html');
const chosen = new Set();
const next = 'index.html';

(async () => {
  const [list, me] = await Promise.all([categoriesReady, api('/me').catch(() => ({ interests: [] }))]);
  (me.interests || []).forEach(s => chosen.add(s)); // permite editar depois
  fill($('#interests'), 'tplInterest', list.map(c => ({ slug: c.slug, icon: c.icon, name: c.name })));
  $$('.interest').forEach(b => b.setAttribute('aria-pressed', chosen.has(b.dataset.slug)));
  $('#continue').disabled = chosen.size === 0;
})();

$('#interests').addEventListener('click', e => {
  const b = e.target.closest('.interest'); if (!b) return;
  const on = !chosen.has(b.dataset.slug);
  on ? chosen.add(b.dataset.slug) : chosen.delete(b.dataset.slug);
  b.setAttribute('aria-pressed', on); $('#continue').disabled = chosen.size === 0;
});
$('#continue').addEventListener('click', async () => {
  $('#continue').disabled = true;
  try { await api('/me/interests', { method: 'PUT', body: { categories: [...chosen] } }); go(next); }
  catch (e) { $('#msg').textContent = e.message; $('#continue').disabled = false; }
});
$('#skip').addEventListener('click', () => { go(next); }); // concluir sem escolher nada
