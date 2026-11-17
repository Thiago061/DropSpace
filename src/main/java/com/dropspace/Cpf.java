package com.dropspace;

/** Validação de CPF (dígitos verificadores). A mesma regra existe no front-end para dar resposta imediata. */
final class Cpf {
    private Cpf() {}

    static String normalize(String s) { return s == null ? "" : s.replaceAll("\\D", ""); }

    static boolean isValid(String raw) {
        String d = normalize(raw);
        if (d.length() != 11 || d.chars().distinct().count() == 1) return false;
        return check(d, 9) == d.charAt(9) - '0' && check(d, 10) == d.charAt(10) - '0';
    }

    private static int check(String d, int len) {
        int sum = 0;
        for (int i = 0; i < len; i++) sum += (d.charAt(i) - '0') * (len + 1 - i);
        int r = (sum * 10) % 11;
        return r == 10 ? 0 : r;
    }
}
