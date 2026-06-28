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

    public byte[] generate(DogadjajAnalitikaDto a) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            PdfContentByte cb = writer.getDirectContent();

            // Header
            text(cb, FONT_BOLD, 20, INK, LEFT, 800, "Reports & Analytics");
            text(cb, FONT_REGULAR, 11, MUTED, LEFT, 784, "aggregated · " + safe(a.getDogadjajNaziv()));

            // Summary
            float sy = 758;
            text(cb, FONT_REGULAR, 11, INK, LEFT, sy,
                    "Stopa prisustva: " + a.getStopaPrisustva() + "%   ·   "
                            + "Potvrđene registracije: " + a.getUkupnoRegistracija()
                            + " / " + a.getMaksKapacitet() + "   ·   "
                            + "Prosečan engagement: " + a.getProsecniEngagement() + "%");

            // Attendance Rate (line chart)
            text(cb, FONT_BOLD, 13, INK, LEFT, 728, "Attendance Rate");
            drawLineChart(cb, a.getStopaPrisustvaSerija(), LEFT, 540, RIGHT - LEFT, 175);

            // Engagement Score / Session (bar chart)
            text(cb, FONT_BOLD, 13, INK, LEFT, 500, "Engagement Score / Session");
            drawBarChart(cb, a.getEngagementPoSesiji(), LEFT, 300, RIGHT - LEFT, 185);

            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Greška pri generisanju PDF izveštaja događaja.", ex);
        }
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
