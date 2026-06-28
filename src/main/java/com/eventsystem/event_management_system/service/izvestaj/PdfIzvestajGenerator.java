package com.eventsystem.event_management_system.service.izvestaj;

import com.eventsystem.event_management_system.dto.IzvestajPodaciDto;
import com.eventsystem.event_management_system.utils.enums.FormatIzvestaja;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Component
public class PdfIzvestajGenerator {

    private static final DecimalFormat MONEY_FMT = moneyFormat();

    public byte[] generate(IzvestajPodaciDto podaci, FormatIzvestaja format) {
        if (format != FormatIzvestaja.PDF) {
            throw new IllegalArgumentException("PDF generator podržava samo PDF format.");
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            document.add(new Paragraph(podaci.getNaslov(), titleFont));
            document.add(new Paragraph(podaci.getPodnaslov(), headerFont));
            if (podaci.getIzvorOznaka() != null) {
                document.add(new Paragraph(podaci.getIzvorOznaka(), normalFont));
            }
            document.add(new Paragraph(" "));

            if (podaci.getUkupanPrihod() != null) {
                addLine(document, "Ukupan prihod:", formatMoney(podaci.getUkupanPrihod()), normalFont);
                addLine(document, "Ukupan trošak:", formatMoney(podaci.getUkupanTrosak()), normalFont);
                addLine(document, "Neto:", formatMoney(podaci.getNeto()), normalFont);
                if (podaci.getMarza() != null) {
                    addLine(document, "Marža:", podaci.getMarza().toPlainString(), normalFont);
                }
                if (podaci.getRezultatOcene() != null) {
                    addLine(document, "Rezultat ocene:", podaci.getRezultatOcene().name(), normalFont);
                }
            }

            if (podaci.getSumaFakturisano() != null) {
                addLine(document, "Suma fakturisano:", formatMoney(podaci.getSumaFakturisano()), normalFont);
                addLine(document, "Suma naplaćeno:", formatMoney(podaci.getSumaNaplaceno()), normalFont);
                addLine(document, "Otvoreno potraživanje:", formatMoney(podaci.getOtvorenoPotrazivanje()), normalFont);
            }

            if (podaci.getSumaPlaceno() != null) {
                addLine(document, "Suma plaćeno:", formatMoney(podaci.getSumaPlaceno()), normalFont);
            }

            if (podaci.getBrojDogadjaja() != null) {
                addLine(document, "Broj događaja:", String.valueOf(podaci.getBrojDogadjaja()), normalFont);
            }

            if (podaci.getFakture() != null && !podaci.getFakture().isEmpty()) {
                document.add(new Paragraph("Fakture", headerFont));
                document.add(buildFakturaTable(podaci));
                document.add(new Paragraph(" "));
            }

            if (podaci.getUgovori() != null && !podaci.getUgovori().isEmpty()) {
                document.add(new Paragraph("Ugovori", headerFont));
                document.add(buildUgovorTable(podaci));
            }

            document.close();
            return out.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Greška pri generisanju PDF izveštaja.", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Greška pri generisanju PDF izveštaja.", ex);
        }
    }

    private PdfPTable buildFakturaTable(IzvestajPodaciDto podaci) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.addCell(headerCell("Broj"));
        table.addCell(headerCell("Datum"));
        table.addCell(headerCell("Ukupan iznos"));
        table.addCell(headerCell("Plaćeni iznos"));
        for (var red : podaci.getFakture()) {
            table.addCell(cell(red.getBrojFakture()));
            table.addCell(cell(red.getDatum()));
            table.addCell(cell(formatMoney(red.getUkupanIznos())));
            table.addCell(cell(formatMoney(red.getPlaceniIznos())));
        }
        return table;
    }

    private PdfPTable buildUgovorTable(IzvestajPodaciDto podaci) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.addCell(headerCell("Broj"));
        table.addCell(headerCell("Predmet"));
        table.addCell(headerCell("Status"));
        table.addCell(headerCell("Vrednost"));
        table.addCell(headerCell("Važi do"));
        for (var red : podaci.getUgovori()) {
            table.addCell(cell(red.getBrojUgovora()));
            table.addCell(cell(red.getPredmet()));
            table.addCell(cell(red.getStatus()));
            table.addCell(cell(formatMoney(red.getVrednost())));
            table.addCell(cell(red.getVaziDo()));
        }
        return table;
    }

    private void addLine(Document doc, String label, String value, Font font) throws DocumentException {
        doc.add(new Paragraph(label + " " + value, font));
    }

    private PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private PdfPCell cell(String text) {
        return new PdfPCell(new Phrase(text != null ? text : "—", FontFactory.getFont(FontFactory.HELVETICA, 10)));
    }

    static String formatMoney(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        return MONEY_FMT.format(value) + " RSD";
    }

    private static DecimalFormat moneyFormat() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("sr-RS"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        df.setGroupingUsed(true);
        return df;
    }
}
