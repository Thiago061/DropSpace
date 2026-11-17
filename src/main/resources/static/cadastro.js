layout('conta');
bindPasswordFields();
bindMask('cpf', maskCpf); bindMask('cep', maskCep);
let addressOk = true;

/* Regras de cada campo: devolvem a mensagem de erro (ou vazio). O servidor repete as mesmas validações. */
const rules = {
  firstName: v => v.trim().length < 2 ? 'Informe seu nome.' : '',
  lastName: v => v.trim().length < 2 ? 'Informe seu sobrenome.' : '',
  cpf: v => !v ? 'Informe seu CPF.' : isCpf(v) ? '' : 'CPF inválido. Confira os números.',
  cep: v => digits(v).length === 8 ? '' : 'Informe um CEP com 8 números.',
  email: v => !v ? 'Informe seu e-mail.' : isEmail(v.trim()) ? '' : 'E-mail inválido. Exemplo: nome@email.com',
  password: v => v.length < 6 || v.length > 8 ? 'A senha deve ter de 6 a 8 caracteres.' : '',
};
Object.keys(rules).forEach(id => $('#' + id).addEventListener('blur', () => setError(id, rules[id]($('#' + id).value))));

async function lookupCep() {
  const out = $('#addr'); out.textContent = '';
  if (rules.cep($('#cep').value)) return;
  out.textContent = 'Buscando endereço...';
  try {
    const a = await api('/address/' + digits($('#cep').value));
    out.textContent = [a.street, a.district, `${a.city}/${a.uf}`].filter(Boolean).join(' · '); setError('cep', ''); addressOk = true;
  } catch (e) {
    out.textContent = '';
    if (e.status === 502) { addressOk = true; out.textContent = 'Não foi possível buscar o endereço agora, mas você pode continuar.'; }
    else { addressOk = false; setError('cep', e.message); }
  }
}
$('#cep').addEventListener('input', () => { if (digits($('#cep').value).length === 8) lookupCep(); else $('#addr').textContent = ''; });

$('#regForm').addEventListener('submit', async e => {
  e.preventDefault(); $('#formErr').textContent = '';
  const values = Object.fromEntries(Object.keys(rules).map(id => [id, $('#' + id).value]));
  const ok = Object.keys(rules).map(id => setError(id, rules[id](values[id]))).every(Boolean);
  if (!ok || !addressOk) { $('#' + (Object.keys(rules).find(id => rules[id](values[id])) || 'cep')).focus(); return; }
  $('#regSubmit').disabled = true;
  try {
    saveSession(await api('/auth/register', { method: 'POST', body: { ...values, email: values.email.trim() } }));
    go('interesses.html');
  } catch (err) { $('#formErr').textContent = err.message; $('#regSubmit').disabled = false; }
});
