layout('conta');
bindPasswordFields();
const show = id => ['loginBox', 'accountBox'].forEach(b => { $('#' + b).hidden = b !== id; });

async function route() {
  if (!session) return show('loginBox');
  try {
    const me = await api('/me');
    show('accountBox'); $('#hello').textContent = `Olá, ${me.name}!`;
    $('#address').textContent = me.street ? `${me.street}, ${me.district} · ${me.city}/${me.uf} · CEP ${me.cep}` : (me.cep ? 'CEP ' + me.cep : '');
    const list = await api('/orders');
    $('#noOrders').hidden = list.length > 0;
    fill($('#orders'), 'tplOrder', list.map(o => ({
      title: `Pedido #${o.id} · ${new Date(o.createdAt).toLocaleDateString('pt-BR')} · ${o.items.length} item(ns) · frete ${brl(o.shipping)} · ${o.status.replaceAll('_', ' ')}`, total: brl(o.total) })));
  } catch (e) { show('loginBox'); $('#loginMsg').textContent = e.message; }
}
$('#loginForm').addEventListener('submit', async e => {
  e.preventDefault(); const msg = $('#loginMsg'); msg.textContent = '';
  if (!$('#login').value.trim() || !$('#password').value) { msg.textContent = 'Informe seu e-mail (ou usuário) e a senha.'; return; }
  try {
    saveSession(await api('/auth/login', { method: 'POST', body: { login: $('#login').value, password: $('#password').value } }));
    go(session.role === 'ADMIN' ? 'admin.html' : 'conta.html');
  } catch (err) { msg.textContent = err.message; }
});
$('#logout').onclick = async () => { try { await api('/auth/logout', { method: 'POST' }); } catch {} saveSession(null); go('index.html'); };
route();
