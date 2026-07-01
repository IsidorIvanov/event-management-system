package com.eventsystem.event_management_system.service.izvestaj;

import com.eventsystem.event_management_system.dto.IzvestajPodaciDto;
import com.eventsystem.event_management_system.utils.enums.FormatIzvestaja;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Component
public class PdfIzvestajGenerator {

    private static final DecimalFormat MONEY_FMT = moneyFormat();
    private static final BaseFont FONT_REGULAR = loadBaseFont("Arial.ttf", "arial.ttf");
    private static final BaseFont FONT_BOLD = loadBaseFont("Arial-Bold.ttf", "arialbd.ttf");

    public byte[] generate(IzvestajPodaciDto podaci, FormatIzvestaja format) {
        if (format != FormatIzvestaja.PDF) {
            throw new IllegalArgumentException("PDF generator podržava samo PDF format.");
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = pdfFont(16, true);
            Font headerFont = pdfFont(12, true);
            Font normalFont = pdfFont(11, false);

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

            if (podaci.getProsecnaIskoriscenostSala() != null) {
                document.add(new Paragraph("Resursi", headerFont));
                addLine(document, "Prosečna iskorišćenost sala:",
                        podaci.getProsecnaIskoriscenostSala() + "%", normalFont);
                if (podaci.getPokrivenostInventarProcenat() != null) {
                    addLine(document, "Pokrivenost inventarom:",
                            podaci.getPokrivenostInventarProcenat() + "%", normalFont);
                }
                document.add(new Paragraph(" "));
            }

            if (podaci.getPrihodOdKarata() != null) {
                document.add(new Paragraph("Raspodela prihoda i troškova", headerFont));
                addLine(document, "Prihod od karata:", formatMoney(podaci.getPrihodOdKarata()), normalFont);
                addLine(document, "Prihod od izlaznih faktura:",
                        formatMoney(podaci.getPrihodOdIzlaznihFaktura()), normalFont);
                addLine(document, "Evidentirani trošak:", formatMoney(podaci.getTrosakEvidentiran()), normalFont);
                addLine(document, "Honorari:", formatMoney(podaci.getTrosakHonorari()), normalFont);
                addLine(document, "Commitovana nabavka (informativno):",
                        formatMoney(podaci.getCommitovanaNabavka()), normalFont);
                document.add(new Paragraph(" "));
            }

            if (podaci.getUpozorenja() != null && !podaci.getUpozorenja().isEmpty()) {
                document.add(new Paragraph("Upozorenja", headerFont));
                for (String upozorenje : podaci.getUpozorenja()) {
                    document.add(new Paragraph("• " + upozorenje, normalFont));
                }
                document.add(new Paragraph(" "));
            }

            if (podaci.getStavkeSala() != null && !podaci.getStavkeSala().isEmpty()) {
                document.add(new Paragraph("Iskorišćenost sala", headerFont));
                document.add(buildSalaTable(podaci));
                document.add(new Paragraph(" "));
            }

            if (podaci.getStavkeOpreme() != null && !podaci.getStavkeOpreme().isEmpty()) {
                document.add(new Paragraph("Oprema", headerFont));
                document.add(buildOpremeTable(podaci));
                document.add(new Paragraph(" "));
            }

            if (podaci.getStavkeNabavke() != null && !podaci.getStavkeNabavke().isEmpty()) {
                document.add(new Paragraph("Nabavke", headerFont));
                document.add(buildNabavkaTable(podaci));
                document.add(new Paragraph(" "));
            }

            if (podaci.getStavkeBudzeta() != null && !podaci.getStavkeBudzeta().isEmpty()) {
                document.add(new Paragraph("Budžet", headerFont));
                document.add(buildBudzetTable(podaci));
                document.add(new Paragraph(" "));
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

    private PdfPTable buildSalaTable(IzvestajPodaciDto podaci) throws DocumentException {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.addCell(headerCell("Sesija"));
        table.addCell(headerCell("Sala"));
        table.addCell(headerCell("Datum"));
        table.addCell(headerCell("Kapacitet"));
        table.addCell(headerCell("Popunjenost"));
        table.addCell(headerCell("Iskorišćenost"));
        for (var red : podaci.getStavkeSala()) {
            table.addCell(cell(red.getSesijaNaziv()));
            table.addCell(cell(red.getNazivSale()));
            table.addCell(cell(red.getDatum()));
            table.addCell(cell(red.getKapacitet() != null ? String.valueOf(red.getKapacitet()) : null));
            table.addCell(cell(red.getPopunjenost() != null ? String.valueOf(red.getPopunjenost()) : null));
            table.addCell(cell(red.getIskoriscenostProcenat() != null
                    ? red.getIskoriscenostProcenat() + "%" : null));
        }
        return table;
    }

    private PdfPTable buildOpremeTable(IzvestajPodaciDto podaci) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.addCell(headerCell("Resurs"));
        table.addCell(headerCell("Potrebno"));
        table.addCell(headerCell("Dodeljeno"));
        table.addCell(headerCell("Na stanju"));
        table.addCell(headerCell("Pokriveno"));
        for (var red : podaci.getStavkeOpreme()) {
            table.addCell(cell(red.getNazivResursa()));
            table.addCell(cell(String.valueOf(red.getPotrebnaKolicina())));
            table.addCell(cell(String.valueOf(red.getDodeljeno())));
            table.addCell(cell(String.valueOf(red.getDostupnoNaStanju())));
            table.addCell(cell(red.isPokriveno() ? "Da" : "Ne"));
        }
        return table;
    }

    private PdfPTable buildNabavkaTable(IzvestajPodaciDto podaci) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.addCell(headerCell("ID"));
        table.addCell(headerCell("Status"));
        table.addCell(headerCell("Dobavljač"));
        table.addCell(headerCell("Stavki"));
        table.addCell(headerCell("Vrednost"));
        for (var red : podaci.getStavkeNabavke()) {
            table.addCell(cell(red.getNabavkaId() != null ? String.valueOf(red.getNabavkaId()) : null));
            table.addCell(cell(red.getStatus()));
            table.addCell(cell(red.getDobavljacNaziv()));
            table.addCell(cell(String.valueOf(red.getBrojStavki())));
            table.addCell(cell(formatMoney(red.getUkupnaVrednost())));
        }
        return table;
    }

    private PdfPTable buildBudzetTable(IzvestajPodaciDto podaci) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.addCell(headerCell("Budžet"));
        table.addCell(headerCell("Kategorija"));
        table.addCell(headerCell("Planirano"));
        table.addCell(headerCell("Stvarno"));
        table.addCell(headerCell("Iskorišćenost"));
        for (var red : podaci.getStavkeBudzeta()) {
            table.addCell(cell(red.getNazivBudzeta()));
            table.addCell(cell(red.getKategorijaNaziv()));
            table.addCell(cell(formatMoney(red.getPlanirano())));
            table.addCell(cell(formatMoney(red.getStvarno())));
            table.addCell(cell(red.getIskoriscenostProcenat() != null
                    ? red.getIskoriscenostProcenat().toPlainString() + "%" : null));
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
        PdfPCell cell = new PdfPCell(new Phrase(text, pdfFont(10, true)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private PdfPCell cell(String text) {
        return new PdfPCell(new Phrase(text != null ? text : "—", pdfFont(10, false)));
    }

    private static Font pdfFont(float size, boolean bold) {
        return new Font(bold ? FONT_BOLD : FONT_REGULAR, size);
    }

    private static BaseFont loadBaseFont(String classpathFile, String windowsFileName) {
        try (InputStream in = PdfIzvestajGenerator.class.getResourceAsStream("/fonts/" + classpathFile)) {
            if (in != null) {
                byte[] bytes = in.readAllBytes();
                return BaseFont.createFont(
                        classpathFile,
                        BaseFont.IDENTITY_H,
                        BaseFont.EMBEDDED,
                        true,
                        bytes,
                        null
                );
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

        throw new IllegalStateException(
                "Unicode font za PDF nije pronađen. Očekivan classpath:/fonts/" + classpathFile
        );
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
