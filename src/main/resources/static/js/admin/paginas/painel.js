// Página "Painel e mapa": indicadores e mapa geral de pontos e ocorrências.
import { api } from '../api.js';
import { dados, aoMudar } from '../dados.js';
import { $, CORES, TIPOS, escapeHtml } from '../util.js';

let mapa = null;
let camada = null;

export function iniciar() {
  try {
    mapa = L.map('mapa-geral').setView([-15.7939, -47.8828], 5);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19, attribution: '&copy; OpenStreetMap contributors',
    }).addTo(mapa);
    camada = L.layerGroup().addTo(mapa);
  } catch (_) {
    $('#mapa-geral').textContent = 'Mapa indisponível no momento. O restante do painel funciona normalmente.';
  }
  aoMudar('dados:pontos', desenhar);
  aoMudar('dados:ocorrencias', desenhar);
}

export function mostrar() {
  if (mapa) setTimeout(() => mapa.invalidateSize(), 60);
}

export async function carregarIndicadores() {
  const s = await api('/api/dashboard');
  $('#ind-pontos').textContent = s.pontosApoio;
  $('#ind-abertas').textContent = s.ocorrenciasAbertas;
  $('#ind-usuarios').textContent = s.usuarios;
  $('#ind-pro').textContent = s.usuariosPro;
}

function desenhar() {
  if (!mapa) return;
  camada.clearLayers();
  const limites = [];

  dados.pontos.filter(p => p.ativo).forEach(p => {
    L.circleMarker([p.latitude, p.longitude], {
      radius: 9, color: CORES[p.tipo], fillColor: CORES[p.tipo], fillOpacity: 0.8,
    }).bindPopup(`<strong>${escapeHtml(p.nome)}</strong><br>${escapeHtml(TIPOS[p.tipo] || p.tipo)}`).addTo(camada);
    limites.push([p.latitude, p.longitude]);
  });

  dados.ocorrencias.filter(o => o.latitude != null && o.longitude != null).forEach(o => {
    L.circleMarker([o.latitude, o.longitude], {
      radius: 8, color: '#c0392b', weight: 2, fillOpacity: 0.25, dashArray: '3',
    }).bindPopup(`<strong>${escapeHtml(o.titulo)}</strong><br>${escapeHtml(o.categoria)} · ${escapeHtml(o.severidade)}<br>Status: ${escapeHtml(o.status)}`)
      .addTo(camada);
    limites.push([o.latitude, o.longitude]);
  });

  if (limites.length) mapa.fitBounds(limites, { padding: [30, 30], maxZoom: 14 });
}
