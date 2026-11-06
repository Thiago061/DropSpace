(function () {
  const lista = document.getElementById('planos-lista');
  const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

  function el(tag, className, text) {
    const e = document.createElement(tag);
    if (className) e.className = className;
    if (text !== undefined) e.textContent = text;
    return e;
  }

  function card(plano) {
    const pro = plano.codigo === 'PRO';
    const art = el('article', 'plan' + (pro ? ' plan-pro' : ''));
    if (pro) art.appendChild(el('span', 'plan-badge', 'Mais completo'));
    art.appendChild(el('h3', null, plano.nome));
    const preco = Number(plano.precoMensal);
    const p = el('p', 'plan-price', preco === 0 ? 'Grátis' : moeda.format(preco));
    if (preco > 0) p.appendChild(el('small', null, ' / mês'));
    art.appendChild(p);
    const ul = el('ul');
    plano.recursos.forEach(r => ul.appendChild(el('li', null, r)));
    art.appendChild(ul);
    return art;
  }

  fetch('/api/planos')
    .then(r => { if (!r.ok) throw new Error(); return r.json(); })
    .then(planos => { lista.replaceChildren(...planos.map(card)); })
    .catch(() => { lista.replaceChildren(el('p', 'loading', 'Não foi possível carregar os planos agora.')); });
})();
