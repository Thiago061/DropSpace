const params = new URLSearchParams(location.search);
const category = params.get('cat') || '', query = params.get('q') || '';
let products = [], interests = [];
layout('loja').then(() => { $('#q').value = query; });

async function load() {
  $('#status').textContent = 'Carregando produtos...';
  try {
    const [list] = await Promise.all([api(`/products?q=${encodeURIComponent(query)}&category=${encodeURIComponent(category)}`), categoriesReady]);
    products = list;
    if (category) $('#titulo').textContent = CAT[category] || 'Produtos';
    if (session) interests = (await api('/me').catch(() => ({}))).interests || []; // personalização leve: interesses primeiro
    render();
  } catch (e) { $('#status').textContent = 'Não foi possível carregar os produtos. ' + e.message; }
}

function cardData(p) {
  const off = p.oldPrice ? Math.round((1 - p.price / p.oldPrice) * 100) : 0, n = Math.round(p.rating);
  return {
    id: p.id, name: p.name, image: p.imageUrl, noImage: !p.imageUrl, icon: ICON[p.category] || '📦', categoryName: CAT[p.category] || 'Produto',
    off: off ? `${off}% OFF` : '', stars: p.reviews > 0 ? `${'★'.repeat(n)}${'☆'.repeat(5 - n)} (${p.reviews})` : '', ratingLabel: `Nota ${p.rating} de 5`,
    oldPrice: p.oldPrice ? 'De ' + brl(p.oldPrice) : '', price: brl(p.price), installment: `ou 10x de ${brl(p.price / 10)} sem juros`,
    shipInfo: (p.price >= 3000 ? 'Frete grátis' : 'Calcule o frete no carrinho') + (p.stock > 0 && p.stock <= 3 ? ` · Últimas ${p.stock} unidades` : ''),
    soldOut: p.stock < 1, buttonLabel: p.stock < 1 ? 'Indisponível' : 'Adicionar ao carrinho',
  };
}

function render() {
  const s = $('#sort').value, rank = p => interests.includes(p.category) ? 0 : 1;
  const list = [...products].sort((a, b) => s === 'asc' ? a.price - b.price : s === 'desc' ? b.price - a.price : rank(a) - rank(b));
  $('#status').textContent = list.length ? `${list.length} produto(s) encontrado(s)` : 'Nenhum produto encontrado. Tente outra busca.';
  fill($('#grid'), 'tplProduct', list.map(cardData));
}

$('#sort').addEventListener('change', render);
$('#grid').addEventListener('click', e => {
  const id = e.target.dataset.add; if (!id) return;
  const p = products.find(x => x.id == id), cart = getCart();
  if ((cart[id] || 0) >= p.stock) { $('#status').textContent = 'Você já adicionou todo o estoque disponível deste produto.'; return; }
  cart[id] = (cart[id] || 0) + 1; setCart(cart);
  $('#status').textContent = `Adicionado ao carrinho: ${p.name}`;
});
load();
