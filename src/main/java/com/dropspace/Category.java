package com.dropspace;

import jakarta.persistence.*;

/** Categoria da loja. Também é usada como lista de interesses do cliente (uma única fonte de verdade). */
@Entity
public class Category {
    @Id public String slug;   // ex.: "notebook"
    public String name;       // ex.: "Notebooks"
    public String icon;       // emoji
    public int position;

    public Category() {}
    public Category(String slug, String name, String icon, int position) {
        this.slug = slug; this.name = name; this.icon = icon; this.position = position;
    }
}
