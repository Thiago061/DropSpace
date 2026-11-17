layout('videos');
(async () => {
  try {
    const [list] = await Promise.all([api('/videos'), categoriesReady]);
    $('#status').textContent = list.length ? '' : 'Em breve, novos vídeos.';
    fill($('#videos'), 'tplVideo', list.map(v => ({
      ...v, thumb: `background-image:url(https://i.ytimg.com/vi/${encodeURIComponent(v.youtubeId)}/hqdefault.jpg)`,
      playLabel: 'Reproduzir vídeo: ' + v.title, channel: 'Canal: ' + v.channel,
      shopHref: 'index.html?cat=' + encodeURIComponent(v.shopCategory), shopLabel: 'Ver ' + (CAT[v.shopCategory] || 'produtos') + ' na loja',
    })));
  } catch (e) { $('#status').textContent = 'Não foi possível carregar os vídeos agora.'; }
})();
// O player só carrega no clique: página mais rápida e menos rastreamento (youtube-nocookie).
$('#videos').addEventListener('click', e => {
  const b = e.target.closest('[data-yt]'); if (!b) return;
  const f = document.createElement('iframe');
  f.src = `https://www.youtube-nocookie.com/embed/${encodeURIComponent(b.dataset.yt)}?autoplay=1&rel=0`;
  f.title = b.dataset.title; f.allow = 'autoplay; encrypted-media; picture-in-picture'; f.allowFullscreen = true; f.referrerPolicy = 'strict-origin-when-cross-origin';
  b.parentElement.replaceChildren(f);
});
