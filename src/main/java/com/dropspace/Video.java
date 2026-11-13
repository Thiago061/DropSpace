package com.dropspace;

import jakarta.persistence.*;
import java.util.regex.Pattern;

/** Vídeo educativo exibido na seção "Aprenda". O vídeo continua hospedado no YouTube (apenas incorporado). */
@Entity
public class Video {
    private static final Pattern YOUTUBE_ID = Pattern.compile("^[A-Za-z0-9_-]{11}$");

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String youtubeId;
    public String title;
    public String channel;       // crédito ao criador
    @Column(length = 500) public String summary;
    public String shopCategory;  // categoria da loja relacionada (liga conteúdo a vendas)
    public int position;         // ordem de exibição

    public Video() {}
    public Video(String youtubeId, String title, String channel, String summary, String shopCategory, int position) {
        if (!YOUTUBE_ID.matcher(youtubeId).matches()) throw new IllegalArgumentException("ID do YouTube inválido: " + youtubeId);
        this.youtubeId = youtubeId; this.title = title; this.channel = channel;
        this.summary = summary; this.shopCategory = shopCategory; this.position = position;
    }
}
