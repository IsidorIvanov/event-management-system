package com.eventsystem.event_management_system.utils;

import java.text.Normalizer;

public final class TekstNormalizacija {

    private TekstNormalizacija() {
    }

    /** Uklanja dijakritike radi upoređivanja (npr. bežični ≈ bezicni). */
    public static String normalizuj(String tekst) {
        if (tekst == null) {
            return "";
        }
        return Normalizer.normalize(tekst, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase()
                .trim();
    }

    public static boolean naziviSePodudaraju(String nazivUCenovniku, String trazeniNaziv) {
        String a = normalizuj(nazivUCenovniku);
        String b = normalizuj(trazeniNaziv);
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        return a.equals(b) || a.contains(b) || b.contains(a);
    }
}
