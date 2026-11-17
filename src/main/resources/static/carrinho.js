layout('carrinho');
let all = [], quote = null, lastCep = '';
const lines = () => Object.entries(getCart()).map(([id, q]) => ({ p: all.find(x => x.id == id), q, id })).filter(l => l.p);
const subtotal = () => lines().reduce((s, l) => s + l.p.price * l.q, 0);

function render() {
  const ls = lines();
  fill($('#items'), 'tplCartLine', ls.map(({ p, q, id }) => ({ id, qty: q, name: p.name, price: brl(p.price), href: 'index.html?q=' + encodeURIComponent(p.name) })));
  $('#empty').hidden = ls.length > 0;
  const sub = subtotal(), ship = quote ? Number(quote.price) : null;
  $('#sub').textContent = brl(sub);
  $('#ship').textContent = ship === null ? 'Informe o CEP' : ship === 0 ? 'Grátis' : brl(ship);
  $('#total').textContent = brl(sub + (ship || 0));
  $('#buy').disabled = !ls.length;
}
async function calc() {
  const msg = $('#cepMsg'); msg.className = ''; msg.textContent = 'Calculando frete...';
  try {
    quote = await api(`/shipping?cep=${encodeURIComponent(lastCep)}&subtotal=${subtotal()}`);
    msg.textContent = `${quote.city}/${quote.uf} · entrega em até ${quote.days} dias úteis`;
  } catch (e) { quote = null; msg.className = 'err'; msg.textContent = e.message; }
  render();
}
bindMask('cep', maskCep);
$('#cepForm').addEventListener('submit', e => { e.preventDefault(); lastCep = $('#cep').value; calc(); });
$('#items').addEventListener('click', e => {
  const t = e.target.dataset, id = t.inc || t.dec; if (!id) return;
  const cart = getCart(), p = all.find(x => x.id == id), next = (cart[id] || 0) + (t.inc ? 1 : -1);
  if (next <= 0) delete cart[id]; else if (next <= p.stock) cart[id] = next; else $('#status').textContent = 'Quantidade máxima em estoque atingida.';
  setCart(cart);
  quote && lastCep ? calc() : render(); // o frete grátis depende do subtotal
});
$('#buy').addEventListener('click', async () => {
  const msg = $('#msg'); msg.className = 'err';
  if (!session) { msg.innerHTML = 'Para comprar, <a href="conta.html">entre</a> ou <a href="cadastro.html">crie sua conta</a>.'; return; }
  if (!quote) { msg.textContent = 'Calcule o frete informando seu CEP.'; return; }
  $('#buy').disabled = true; msg.className = ''; msg.textContent = 'Finalizando pedido...';
  try {
    const items = lines().map(l => ({ productId: +l.id, quantity: l.q }));
    const o = await api('/orders', { method: 'POST', body: { items, cep: lastCep } });
    setCart({});
    $('#conteudo > .cols').hidden = true; $('#done').hidden = false;
    $('#doneText').textContent = `Pedido #${o.id} · Total ${brl(o.total)} (frete ${brl(o.shipping)}). O pagamento ainda está pendente: esta é uma versão de demonstração.`;
  } catch (e) { msg.className = 'err'; msg.textContent = e.message; $('#buy').disabled = false; }
});
(async () => {
  try { all = await api('/products'); const c = getCart(); Object.keys(c).forEach(id => { if (!all.find(p => p.id == id)) delete c[id]; }); setCart(c); }
  catch (e) { $('#status').textContent = 'Não foi possível carregar o carrinho. ' + e.message; }
  render();
})();
