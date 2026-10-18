// Tela de entrada do painel.
import { guardarToken, login } from './api.js';
import { $, mostrar } from './util.js';

/**
 * @param aoEntrar chamada com o usuário depois de um login válido de administrador
 */
export function iniciarLogin(aoEntrar) {
  const form = $('#form-login');
  const botao = $('#botao-entrar');
  const msg = $('#msg-login');

  $('#mostrar-senha').addEventListener('change', e => {
    form.elements.senha.type = e.target.checked ? 'text' : 'password';
  });

  form.addEventListener('submit', async evento => {
    evento.preventDefault();
    if (botao.disabled) return; // evita clique duplo enquanto a senha é verificada (leva ~1 s)
    botao.disabled = true;
    botao.textContent = 'Entrando…';
    mostrar(msg, '', true);
    try {
      const resposta = await login(form.elements.login.value.trim(), form.elements.senha.value);
      if (resposta.usuario.papel !== 'ADMIN') throw new Error('Esta conta não tem acesso administrativo.');
      guardarToken(resposta.token);
      form.elements.senha.value = '';
      await aoEntrar(resposta.usuario);
    } catch (erro) {
      mostrar(msg, erro.message, false);
    } finally {
      botao.disabled = false;
      botao.textContent = 'Entrar';
    }
  });
}
