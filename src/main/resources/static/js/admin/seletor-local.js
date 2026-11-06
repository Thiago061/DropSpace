// Seletor de local: busca por nome (Google/OpenStreetMap, via backend) + mapa para ajustar o ponto exato.
// Preenche os campos "latitude" e "longitude" do formulário em que está.
import { api } from './api.js';
import { $ } from './util.js';

const CENTRO_PADRAO = [-15.7939, -47.8828];

export function criarSeletor(raiz, { aoEscolherLugar } = {}) {
  const form = raiz.closest('form');
  const campoBusca = $('[data-busca]', raiz);
  const botao = $('[data-buscar]', raiz);
  const aviso = $('[data-aviso]', raiz);
  const lista = $('[data-resultados]', raiz);
  const divMapa = $('[data-mapa]', raiz);
  const campoLat = form.elements.latitude;
  const campoLon = form.elements.longitude;

  let mapa = null;
  let marcador = null;

  function garantirMapa() {
    if (mapa) return;
    mapa = L.map(divMapa).setView(CENTRO_PADRAO, 5);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19, attribution: '&copy; OpenStreetMap contributors',
    }).addTo(mapa);
    mapa.on('click', e => definir(e.latlng.lat, e.latlng.lng, false));
    sincronizarComCampos();
  }

  /** Coloca (ou move) o marcador e grava nos campos. */
  function definir(lat, lon, centralizar = true) {
    const la = Number(lat.toFixed(6));
    const lo = Number(lon.toFixed(6));
    campoLat.value = la;
    campoLon.value = lo;
    if (!mapa) return;
    if (!marcador) {
      marcador = L.marker([la, lo], { draggable: true }).addTo(mapa);
      marcador.on('dragend', () => { const p = marcador.getLatLng(); definir(p.lat, p.lng, false); });
    } else {
      marcador.setLatLng([la, lo]);
    }
    if (centralizar) mapa.setView([la, lo], 17);
  }

  /** Se o formulário já tem coordenadas (edição), mostra no mapa. */
  function sincronizarComCampos() {
    const lat = parseFloat(campoLat.value);
    const lon = parseFloat(campoLon.value);
    if (Number.isFinite(lat) && Number.isFinite(lon)) definir(lat, lon, true);
  }

  async function buscar() {
    const consulta = campoBusca.value.trim();
    lista.replaceChildren();
    if (consulta.length < 3) { aviso.textContent = 'Digite ao menos 3 letras.'; aviso.className = 'message error'; return; }
    botao.disabled = true;
    aviso.textContent = 'Buscando…';
    aviso.className = 'message';
    try {
      const r = await api('/api/admin/lugares?q=' + encodeURIComponent(consulta));
      const partes = [];
      if (r.aviso) partes.push(r.aviso);
      partes.push(r.lugares.length ? `Resultados: ${r.fonte}. Clique para escolher.` : 'Nada encontrado. Tente outro nome ou marque no mapa.');
      aviso.textContent = partes.join(' ');
      aviso.className = 'message ' + (r.lugares.length ? 'success' : 'error');
      r.lugares.forEach(lugar => {
        const li = document.createElement('li');
        const b = document.createElement('button');
        b.type = 'button';
        const nome = document.createElement('strong');
        nome.textContent = lugar.nome || '(sem nome)';
        const end = document.createElement('small');
        end.textContent = lugar.endereco;
        b.append(nome, end);
        b.addEventListener('click', () => {
          garantirMapa();
          definir(lugar.latitude, lugar.longitude, true);
          if (aoEscolherLugar) aoEscolherLugar(lugar);
          lista.replaceChildren();
          aviso.textContent = 'Local escolhido. Arraste o marcador se quiser ajustar o ponto exato.';
          aviso.className = 'message success';
        });
        li.appendChild(b);
        lista.appendChild(li);
      });
    } catch (erro) {
      aviso.textContent = erro.message;
      aviso.className = 'message error';
    } finally {
      botao.disabled = false;
    }
  }

  botao.addEventListener('click', buscar);
  campoBusca.addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); buscar(); } });
  // quem digita as coordenadas à mão também vê o marcador
  [campoLat, campoLon].forEach(c => c.addEventListener('change', () => { if (mapa) sincronizarComCampos(); }));

  return {
    /** Chame quando a página ficar visível (o mapa precisa de tamanho para desenhar). */
    mostrar() {
      try {
        garantirMapa();
        setTimeout(() => { mapa.invalidateSize(); sincronizarComCampos(); }, 60);
      } catch (_) {
        divMapa.textContent = 'Mapa indisponível. Digite latitude e longitude manualmente.';
      }
    },
    /** Limpa busca, resultados e marcador (após salvar/cancelar). */
    limpar() {
      campoBusca.value = '';
      lista.replaceChildren();
      aviso.textContent = '';
      aviso.className = 'message';
      if (marcador) { marcador.remove(); marcador = null; }
    },
    /** Usado na edição: leva o mapa até as coordenadas já preenchidas. */
    atualizar: sincronizarComCampos,
  };
}
