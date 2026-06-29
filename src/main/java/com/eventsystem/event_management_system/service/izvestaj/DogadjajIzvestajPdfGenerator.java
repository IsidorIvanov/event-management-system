package com.eventsystem.event_management_system.service.izvestaj;

import com.eventsystem.event_management_system.dto.DogadjajAnalitikaDto;
import com.eventsystem.event_management_system.dto.DogadjajAnalitikaDto.TackaDto;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Generiše PDF "Reports & Analytics" za jedan događaj: rezime + linijski grafikon
 * stope prisustva i stubičasti grafikon engagement-a po sesiji (crtani direktno na PDF).
 */
@Component
public class DogadjajIzvestajPdfGenerator {

    private static final BaseFont FONT_REGULAR = loadBaseFont("Arial.ttf", "arial.ttf");
    private static final BaseFont FONT_BOLD = loadBaseFont("Arial-Bold.ttf", "arialbd.ttf");

    private static final Color INK = new Color(0x33, 0x33, 0x33);
    private static final Color MUTED = new Color(0x88, 0x88, 0x88);
    private static final Color GRID = new Color(0xDD, 0xDD, 0xD5);
    private static final Color PANEL = new Color(0xEC, 0xEA, 0xE3);
    private static final Color BAR = new Color(0x3A, 0x3A, 0x3A);
    private static final Color LINE = new Color(0x22, 0x22, 0x22);

    private static final float LEFT = 50f;
    private static final float RIGHT = 545f;

    private static final DateTimeFormatter DATUM_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter GEN_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public byte[] generate(DogadjajAnalitikaDto a) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            PdfContentByte cb = writer.getDirectContent();

            // Header
            text(cb, FONT_BOLD, 20, INK, LEFT, 806, "Reports & Analytics");
            text(cb, FONT_REGULAR, 11, MUTED, LEFT, 790, safe(a.getDogadjajNaziv()));
            cb.setColorStroke(GRID);
            cb.setLineWidth(0.8f);
            cb.moveTo(LEFT, 780);
            cb.lineTo(RIGHT, 780);
            cb.stroke();

            // Key metrics (cards)
            float cardGap = 12f;
            float cardW = (RIGHT - LEFT - 2 * cardGap) / 3f;
            float cardY = 724;
            float cardH = 48;
            drawMetricCard(cb, LEFT, cardY, cardW, cardH,
                    fmtPct(a.getStopaPrisustva()), "Stopa prisustva");
            drawMetricCard(cb, LEFT + cardW + cardGap, cardY, cardW, cardH,
                    a.getUkupnoRegistracija() + " / " + nz(a.getMaksKapacitet()), "Potvrđene registracije");
            drawMetricCard(cb, LEFT + 2 * (cardW + cardGap), cardY, cardW, cardH,
                    fmtPct(a.getProsecniEngagement()), "Prosečan engagement");

            // Event Details
            text(cb, FONT_BOLD, 13, INK, LEFT, 700, "Detalji događaja");
            drawEventDetails(cb, a, LEFT, 556, RIGHT - LEFT, 136);

            // Attendance Rate (line chart)
            text(cb, FONT_BOLD, 13, INK, LEFT, 538, "Attendance Rate");
            drawLineChart(cb, a.getStopaPrisustvaSerija(), LEFT, 398, RIGHT - LEFT, 132);

            // Engagement Score / Session (bar chart)
            text(cb, FONT_BOLD, 13, INK, LEFT, 380, "Engagement Score / Session");
            drawBarChart(cb, a.getEngagementPoSesiji(), LEFT, 234, RIGHT - LEFT, 138);

            // Footer
            String generisano = a.getGenerisanoU() != null
                    ? a.getGenerisanoU().format(GEN_FMT)
                    : "";
            cb.setColorStroke(GRID);
            cb.setLineWidth(0.6f);
            cb.moveTo(LEFT, 40);
            cb.lineTo(RIGHT, 40);
            cb.stroke();
            text(cb, FONT_REGULAR, 8, MUTED, LEFT, 28, "Generisano: " + generisano);
            centerText(cb, FONT_REGULAR, 8, MUTED, (LEFT + RIGHT) / 2, 28, "Event Management System");
            String idLabel = "ID događaja: " + nz(a.getDogadjajId());
            text(cb, FONT_REGULAR, 8, MUTED, RIGHT - FONT_REGULAR.getWidthPoint(idLabel, 8), 28, idLabel);

            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Greška pri generisanju PDF izveštaja događaja.", ex);
        }
    }

    private void drawMetricCard(PdfContentByte cb, float x, float y, float w, float h, String value, String label) {
        panel(cb, x, y, w, h);
        centerText(cb, FONT_BOLD, 18, INK, x + w / 2, y + h - 26, value);
        centerText(cb, FONT_REGULAR, 8, MUTED, x + w / 2, y + 9, label);
    }

    private void drawEventDetails(PdfContentByte cb, DogadjajAnalitikaDto a, float x, float y, float w, float h) {
        panel(cb, x, y, w, h);
        float pad = 14f;
        float colW = (w - 2 * pad) / 2f;
        float leftX = x + pad;
        float rightX = x + pad + colW;
        float labelW = 92f;

        float row1 = y + h - 18;
        float rowGap = 19f;
        // Vrednost levog stuba staje do početka desnog; desni do desne ivice panela.
        float leftValW = rightX - (leftX + labelW) - 8f;
        float rightValW = (x + w - pad) - (rightX + labelW);
        float fullValW = (x + w - pad) - (leftX + labelW);

        kv(cb, leftX, row1, labelW, leftValW, "Lokacija", a.getLokacija());
        kv(cb, leftX, row1 - rowGap, labelW, leftValW, "Period", formatPeriod(a));
        kv(cb, leftX, row1 - 2 * rowGap, labelW, leftValW, "Trajanje", formatTrajanje(a));

        kv(cb, rightX, row1, labelW, rightValW, "Status", a.getStatus());
        kv(cb, rightX, row1 - rowGap, labelW, rightValW, "Maks. kapacitet", String.valueOf(nz(a.getMaksKapacitet())));
        kv(cb, rightX, row1 - 2 * rowGap, labelW, rightValW, "Broj sesija", String.valueOf(a.getBrojSesija()));

        float tagY = row1 - 3 * rowGap;
        String tagovi = (a.getTagovi() == null || a.getTagovi().isEmpty())
                ? "—" : String.join(", ", a.getTagovi());
        kv(cb, leftX, tagY, labelW, fullValW, "Tagovi", tagovi);

        // Opis (prelama se u najviše dva reda preko cele širine panela).
        float opisY = tagY - rowGap;
        text(cb, FONT_REGULAR, 8.5f, MUTED, leftX, opisY, "Opis");
        String opis = (a.getOpis() == null || a.getOpis().isBlank()) ? "—" : a.getOpis().strip();
        wrapText(cb, FONT_REGULAR, 9.5f, INK, leftX + labelW, opisY,
                w - 2 * pad - labelW, opis, 2, 11f);
    }

    private void kv(PdfContentByte cb, float x, float y, float labelW, float valueW, String label, String value) {
        text(cb, FONT_REGULAR, 8.5f, MUTED, x, y, label);
        String v = (value == null || value.isBlank()) ? "—" : value;
        v = skratiNaSirinu(FONT_REGULAR, 9.5f, v, valueW);
        text(cb, FONT_REGULAR, 9.5f, INK, x + labelW, y, v);
    }

    /** Ispisuje tekst prelomljen po rečima u najviše {@code maxLines} redova. */
    private void wrapText(PdfContentByte cb, BaseFont font, float size, Color color,
                          float x, float y, float maxW, String s, int maxLines, float leading) {
        if (s == null) s = "";
        String[] reci = s.split("\\s+");
        StringBuilder linija = new StringBuilder();
        int line = 0;
        for (int i = 0; i < reci.length && line < maxLines; i++) {
            String kandidat = linija.length() == 0 ? reci[i] : linija + " " + reci[i];
            if (font.getWidthPoint(kandidat, size) > maxW && linija.length() > 0) {
                boolean poslednji = line == maxLines - 1;
                String ispis = linija.toString();
                if (poslednji) ispis = skratiNaSirinu(font, size, ispis + " …", maxW);
                text(cb, font, size, color, x, y - line * leading, ispis);
                linija = new StringBuilder(reci[i]);
                line++;
            } else {
                if (linija.length() > 0) linija.append(' ');
                linija.append(reci[i]);
            }
        }
        if (line < maxLines && linija.length() > 0) {
            text(cb, font, size, color, x, y - line * leading, linija.toString());
        }
    }

    private static String skratiNaSirinu(BaseFont font, float size, String s, float maxW) {
        while (s.length() > 1 && font.getWidthPoint(s, size) > maxW) {
            s = s.substring(0, s.length() - 2) + "…";
        }
        return s;
    }

    private static String formatPeriod(DogadjajAnalitikaDto a) {
        if (a.getDatumPocetka() == null || a.getDatumZavrsetka() == null) return "—";
        return a.getDatumPocetka().format(DATUM_FMT) + " – " + a.getDatumZavrsetka().format(DATUM_FMT);
    }

    private static String formatTrajanje(DogadjajAnalitikaDto a) {
        if (a.getDatumPocetka() == null || a.getDatumZavrsetka() == null) return "—";
        long dana = ChronoUnit.DAYS.between(a.getDatumPocetka(), a.getDatumZavrsetka()) + 1;
        return dana + (dana == 1 ? " dan" : " dana");
    }

    private static int nz(Integer v) {
        return v != null ? v : 0;
    }

    private static long nz(Long v) {
        return v != null ? v : 0L;
    }

    private void drawLineChart(PdfContentByte cb, List<TackaDto> tacke, float x, float y, float w, float h) {
        panel(cb, x, y, w, h);
        if (tacke == null || tacke.isEmpty()) {
            text(cb, FONT_REGULAR, 10, MUTED, x + 12, y + h - 20, "Nema podataka o registracijama.");
            return;
        }

        float pad = 14f;
        float plotX = x + pad;
        float plotY = y + pad + 12; // prostor za oznake datuma
        float plotW = w - 2 * pad;
        float plotH = h - 2 * pad - 12;

        double max = Math.max(1.0, tacke.stream().mapToDouble(TackaDto::getVrednost).max().orElse(1.0));

        gridlines(cb, plotX, plotY, plotW, plotH);

        cb.setColorStroke(LINE);
        cb.setLineWidth(1.4f);
        int n = tacke.size();
        for (int i = 0; i < n; i++) {
            float px = n == 1 ? plotX + plotW / 2 : plotX + plotW * i / (n - 1);
            float py = plotY + (float) (plotH * (tacke.get(i).getVrednost() / max));
            if (i == 0) cb.moveTo(px, py);
            else cb.lineTo(px, py);
        }
        cb.stroke();

        // tačke + oznake (proređene da se ne preklapaju)
        int step = Math.max(1, n / 8);
        cb.setColorFill(LINE);
        for (int i = 0; i < n; i++) {
            float px = n == 1 ? plotX + plotW / 2 : plotX + plotW * i / (n - 1);
            float py = plotY + (float) (plotH * (tacke.get(i).getVrednost() / max));
            cb.circle(px, py, 1.6f);
            cb.fill();
            if (i % step == 0 || i == n - 1) {
                centerText(cb, FONT_REGULAR, 7, MUTED, px, y + 5, tacke.get(i).getOznaka());
            }
        }
    }

    private void drawBarChart(PdfContentByte cb, List<TackaDto> tacke, float x, float y, float w, float h) {
        panel(cb, x, y, w, h);
        if (tacke == null || tacke.isEmpty()) {
            text(cb, FONT_REGULAR, 10, MUTED, x + 12, y + h - 20, "Nema sesija za ovaj događaj.");
            return;
        }

        float pad = 14f;
        float plotX = x + pad;
        float plotY = y + pad + 14; // prostor za nazive sesija
        float plotW = w - 2 * pad;
        float plotH = h - 2 * pad - 14;

        double max = Math.max(1.0, tacke.stream().mapToDouble(TackaDto::getVrednost).max().orElse(1.0));
        gridlines(cb, plotX, plotY, plotW, plotH);

        int n = tacke.size();
        float slot = plotW / n;
        float barW = Math.min(48f, slot * 0.6f);
        for (int i = 0; i < n; i++) {
            TackaDto t = tacke.get(i);
            float cx = plotX + slot * i + slot / 2;
            float bh = (float) (plotH * (t.getVrednost() / max));
            cb.setColorFill(BAR);
            cb.rectangle(cx - barW / 2, plotY, barW, bh);
            cb.fill();
            // vrednost iznad stuba
            centerText(cb, FONT_REGULAR, 7, MUTED, cx, plotY + bh + 3, fmtPct(t.getVrednost()));
            // skraćeni naziv sesije ispod
            centerText(cb, FONT_REGULAR, 7, MUTED, cx, y + 5, skrati(t.getOznaka(), 14));
        }
    }

    private void panel(PdfContentByte cb, float x, float y, float w, float h) {
        cb.setColorFill(PANEL);
        cb.rectangle(x, y, w, h);
        cb.fill();
        cb.setColorStroke(GRID);
        cb.setLineWidth(0.7f);
        cb.rectangle(x, y, w, h);
        cb.stroke();
    }

    private void gridlines(PdfContentByte cb, float x, float y, float w, float h) {
        cb.setColorStroke(GRID);
        cb.setLineWidth(0.5f);
        for (int i = 1; i <= 3; i++) {
            float gy = y + h * i / 4;
            cb.moveTo(x, gy);
            cb.lineTo(x + w, gy);
        }
        cb.stroke();
    }

    private void text(PdfContentByte cb, BaseFont font, float size, Color color, float x, float y, String s) {
        cb.beginText();
        cb.setColorFill(color);
        cb.setFontAndSize(font, size);
        cb.setTextMatrix(x, y);
        cb.showText(s != null ? s : "");
        cb.endText();
    }

    private void centerText(PdfContentByte cb, BaseFont font, float size, Color color, float cx, float y, String s) {
        if (s == null) s = "";
        float tw = font.getWidthPoint(s, size);
        text(cb, font, size, color, cx - tw / 2, y, s);
    }

    private static String fmtPct(double v) {
        return (Math.round(v) == v ? String.valueOf((long) v) : String.valueOf(v)) + "%";
    }

    private static String skrati(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }

    private static BaseFont loadBaseFont(String classpathFile, String windowsFileName) {
        try (InputStream in = DogadjajIzvestajPdfGenerator.class.getResourceAsStream("/fonts/" + classpathFile)) {
            if (in != null) {
                byte[] bytes = in.readAllBytes();
                return BaseFont.createFont(classpathFile, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Greška pri učitavanju PDF fonta iz classpath: " + classpathFile, ex);
        }
        try {
            Path windowsFont = Path.of("C:/Windows/Fonts/" + windowsFileName);
            if (Files.isRegularFile(windowsFont)) {
                return BaseFont.createFont(windowsFont.toString(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Greška pri učitavanju PDF fonta sa diska: " + windowsFileName, ex);
        }
        throw new IllegalStateException("Unicode font za PDF nije pronađen. Očekivan classpath:/fonts/" + classpathFile);
    }
}
