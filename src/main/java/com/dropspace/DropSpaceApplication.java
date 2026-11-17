package com.dropspace;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class DropSpaceApplication {
    public static void main(String[] args) { SpringApplication.run(DropSpaceApplication.class, args); }

    /** Cria/atualiza o administrador a partir da configuração (config/local.properties ou variáveis de ambiente). */
    @Bean
    CommandLineRunner seedAdmin(Users users, BCryptPasswordEncoder encoder,
                                @Value("${dropspace.admin.username:}") String username,
                                @Value("${dropspace.admin.password:}") String password) {
        return args -> {
            if (username.isBlank() || password.isBlank()) return; // sem credenciais configuradas, não existe admin
            String login = username.toLowerCase().trim();
            User admin = users.findByUsername(login).orElseGet(() -> {
                User u = new User();
                u.username = login; u.firstName = "Administrador"; u.role = "ADMIN";
                return u;
            });
            if (admin.passwordHash == null || !encoder.matches(password, admin.passwordHash)) {
                admin.passwordHash = encoder.encode(password); // trocar a senha na configuração troca a senha no banco
                users.save(admin);
            }
        };
    }

    /** Dados de exemplo na primeira execução. */
    @Bean
    CommandLineRunner seed(Products products, Videos videos, Categories categories) {
        return args -> {
            if (categories.count() == 0) {
                categories.save(new Category("pc", "Computadores", "🖥️", 1));
                categories.save(new Category("notebook", "Notebooks", "💻", 2));
                categories.save(new Category("celular", "Celulares", "📱", 3));
                categories.save(new Category("monitor", "Monitores", "📺", 4));
                categories.save(new Category("periferico", "Periféricos", "🖱️", 5));
                categories.save(new Category("teclado", "Teclados e mouses", "⌨️", 6));
                categories.save(new Category("hardware", "Hardware e componentes", "🧩", 7));
                categories.save(new Category("console", "Games", "🎮", 8));
                categories.save(new Category("acessorio", "Acessórios", "🎧", 9));
            }
            if (videos.count() == 0) {
                videos.save(new Video("hF6_rzZOmho", "Os melhores PCs gamer por faixa de preço", "MW Informática", "Comparativo de PCs gamer em diferentes faixas de preço, para você saber quanto investir.", "pc", 1));
                videos.save(new Video("pRHOPltt8FY", "Qual é o melhor PC de entrada em 2026?", "Reset Hub", "Testes reais comparando um PC montado com peças novas contra um kit Xeon.", "pc", 2));
                videos.save(new Video("pFTgWgb5wiM", "Como escolher as melhores peças de PC", "YouTube", "Guia para entender e escolher as peças certas na hora de comprar ou montar.", "hardware", 3));
            }
            if (products.count() > 0) return;
            products.save(new Product("PC Gamer Ryzen 5 5600, RTX 4060, 16GB DDR4, SSD 500GB", "pc", "DropSpace", "4599.90", "5299.90", 8, 4.7, 128));
            products.save(new Product("PC Home Office Core i3, 8GB, SSD 240GB", "pc", "DropSpace", "1999.90", null, 15, 4.4, 56));
            products.save(new Product("PC Workstation Ryzen 9, 64GB, RTX 4070, SSD 2TB", "pc", "DropSpace", "12499.90", "13999.90", 3, 4.9, 21));
            products.save(new Product("Notebook Acer Aspire 5, Core i5, 8GB, SSD 512GB, 15.6\"", "notebook", "Acer", "3299.90", "3699.90", 20, 4.5, 342));
            products.save(new Product("Notebook Gamer Lenovo LOQ, Ryzen 7, RTX 4050, 16GB", "notebook", "Lenovo", "5899.90", null, 9, 4.6, 87));
            products.save(new Product("Notebook Dell Inspiron 15, Core i7, 16GB, SSD 512GB", "notebook", "Dell", "4799.90", "5399.90", 11, 4.3, 64));
            products.save(new Product("Smartphone Samsung Galaxy A55, 256GB, 5G", "celular", "Samsung", "2299.90", "2599.90", 30, 4.6, 910));
            products.save(new Product("Smartphone Motorola Moto G54, 128GB, 5G", "celular", "Motorola", "1099.90", null, 40, 4.2, 455));
            products.save(new Product("Smartphone Xiaomi Redmi Note 13, 256GB", "celular", "Xiaomi", "1599.90", "1799.90", 25, 4.5, 612));
            products.save(new Product("Monitor Gamer LG UltraGear 24\", Full HD, 144Hz", "monitor", "LG", "899.90", "1099.90", 18, 4.7, 265));
            products.save(new Product("Webcam Full HD 1080p com microfone", "periferico", "Logitech", "249.90", null, 28, 4.4, 190));
            products.save(new Product("Teclado Mecânico Redragon Kumara, ABNT2", "teclado", "Redragon", "229.90", null, 50, 4.5, 1200));
            products.save(new Product("Mouse Gamer Logitech G203, 8000 DPI", "teclado", "Logitech", "129.90", "159.90", 60, 4.6, 2100));
            products.save(new Product("Placa de Vídeo GeForce RTX 4060, 8GB", "hardware", "Gigabyte", "2199.90", "2499.90", 10, 4.8, 330));
            products.save(new Product("SSD NVMe 1TB Kingston NV2", "hardware", "Kingston", "449.90", null, 45, 4.7, 870));
            products.save(new Product("Console PlayStation 5 Slim, 1TB, com leitor de disco", "console", "Sony", "3699.90", "3999.90", 12, 4.9, 1530));
            products.save(new Product("Console Xbox Series S, 512GB", "console", "Microsoft", "2399.90", null, 14, 4.7, 740));
            products.save(new Product("Console Nintendo Switch OLED, 64GB", "console", "Nintendo", "2499.90", "2799.90", 10, 4.8, 980));
            products.save(new Product("Headset Gamer Sem Fio HyperX Cloud", "acessorio", "HyperX", "499.90", "599.90", 35, 4.6, 321));
        };
    }
}
