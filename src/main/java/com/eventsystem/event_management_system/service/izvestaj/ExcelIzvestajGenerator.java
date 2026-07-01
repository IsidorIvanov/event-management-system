package com.eventsystem.event_management_system.service.izvestaj;

import com.eventsystem.event_management_system.dto.IzvestajPodaciDto;
import com.eventsystem.event_management_system.utils.enums.FormatIzvestaja;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

@Component
public class ExcelIzvestajGenerator {

    public byte[] generate(IzvestajPodaciDto podaci, FormatIzvestaja format) {
        if (format != FormatIzvestaja.EXCEL) {
            throw new IllegalArgumentException("Excel generator podržava samo EXCEL format.");
        }
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Izveštaj");
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            int rowIdx = 0;
            rowIdx = addRow(sheet, rowIdx, podaci.getNaslov(), headerStyle);
            rowIdx = addRow(sheet, rowIdx, podaci.getPodnaslov(), null);
            if (podaci.getIzvorOznaka() != null) {
                rowIdx = addRow(sheet, rowIdx, podaci.getIzvorOznaka(), null);
            }
            rowIdx++;

            if (podaci.getUkupanPrihod() != null) {
                rowIdx = addPair(sheet, rowIdx, "Ukupan prihod", PdfIzvestajGenerator.formatMoney(podaci.getUkupanPrihod()));
                rowIdx = addPair(sheet, rowIdx, "Ukupan trošak", PdfIzvestajGenerator.formatMoney(podaci.getUkupanTrosak()));
                rowIdx = addPair(sheet, rowIdx, "Neto", PdfIzvestajGenerator.formatMoney(podaci.getNeto()));
                if (podaci.getMarza() != null) {
                    rowIdx = addPair(sheet, rowIdx, "Marža", podaci.getMarza().toPlainString());
                }
                if (podaci.getRezultatOcene() != null) {
                    rowIdx = addPair(sheet, rowIdx, "Rezultat ocene", podaci.getRezultatOcene().name());
                }
            }

            if (podaci.getSumaFakturisano() != null) {
                rowIdx = addPair(sheet, rowIdx, "Suma fakturisano", PdfIzvestajGenerator.formatMoney(podaci.getSumaFakturisano()));
                rowIdx = addPair(sheet, rowIdx, "Suma naplaćeno", PdfIzvestajGenerator.formatMoney(podaci.getSumaNaplaceno()));
                rowIdx = addPair(sheet, rowIdx, "Otvoreno potraživanje", PdfIzvestajGenerator.formatMoney(podaci.getOtvorenoPotrazivanje()));
            }

            if (podaci.getSumaPlaceno() != null) {
                rowIdx = addPair(sheet, rowIdx, "Suma plaćeno", PdfIzvestajGenerator.formatMoney(podaci.getSumaPlaceno()));
            }

            if (podaci.getBrojDogadjaja() != null) {
                rowIdx = addPair(sheet, rowIdx, "Broj događaja", String.valueOf(podaci.getBrojDogadjaja()));
            }

            if (podaci.getProsecnaIskoriscenostSala() != null) {
                rowIdx++;
                rowIdx = addRow(sheet, rowIdx, "Resursi", headerStyle);
                rowIdx = addPair(sheet, rowIdx, "Prosečna iskorišćenost sala",
                        podaci.getProsecnaIskoriscenostSala() + "%");
                if (podaci.getPokrivenostInventarProcenat() != null) {
                    rowIdx = addPair(sheet, rowIdx, "Pokrivenost inventarom",
                            podaci.getPokrivenostInventarProcenat() + "%");
                }
            }

            if (podaci.getPrihodOdKarata() != null) {
                rowIdx++;
                rowIdx = addRow(sheet, rowIdx, "Raspodela prihoda i troškova", headerStyle);
                rowIdx = addPair(sheet, rowIdx, "Prihod od karata",
                        PdfIzvestajGenerator.formatMoney(podaci.getPrihodOdKarata()));
                rowIdx = addPair(sheet, rowIdx, "Prihod od izlaznih faktura",
                        PdfIzvestajGenerator.formatMoney(podaci.getPrihodOdIzlaznihFaktura()));
                rowIdx = addPair(sheet, rowIdx, "Evidentirani trošak",
                        PdfIzvestajGenerator.formatMoney(podaci.getTrosakEvidentiran()));
                rowIdx = addPair(sheet, rowIdx, "Honorari",
                        PdfIzvestajGenerator.formatMoney(podaci.getTrosakHonorari()));
                rowIdx = addPair(sheet, rowIdx, "Commitovana nabavka (informativno)",
                        PdfIzvestajGenerator.formatMoney(podaci.getCommitovanaNabavka()));
            }

            if (podaci.getUpozorenja() != null && !podaci.getUpozorenja().isEmpty()) {
                rowIdx++;
                rowIdx = addRow(sheet, rowIdx, "Upozorenja", headerStyle);
                for (String upozorenje : podaci.getUpozorenja()) {
                    rowIdx = addRow(sheet, rowIdx, "• " + upozorenje, null);
                }
            }

            if (podaci.getStavkeSala() != null && !podaci.getStavkeSala().isEmpty()) {
                rowIdx++;
                rowIdx = addRow(sheet, rowIdx, "Iskorišćenost sala", headerStyle);
                Row header = sheet.createRow(rowIdx++);
                String[] cols = {"Sesija", "Sala", "Datum", "Kapacitet", "Popunjenost", "Iskorišćenost %"};
                for (int i = 0; i < cols.length; i++) {
                    Cell cell = header.createCell(i);
                    cell.setCellValue(cols[i]);
                    cell.setCellStyle(headerStyle);
                }
                for (var s : podaci.getStavkeSala()) {
                    Row row = sheet.createRow(rowIdx++);
                    setCell(row, 0, s.getSesijaNaziv());
                    setCell(row, 1, s.getNazivSale());
                    setCell(row, 2, s.getDatum());
                    setCell(row, 3, s.getKapacitet() != null ? String.valueOf(s.getKapacitet()) : "—");
                    setCell(row, 4, s.getPopunjenost() != null ? String.valueOf(s.getPopunjenost()) : "—");
                    setCell(row, 5, s.getIskoriscenostProcenat() != null
                            ? s.getIskoriscenostProcenat() + "%" : "—");
                }
            }

            if (podaci.getStavkeOpreme() != null && !podaci.getStavkeOpreme().isEmpty()) {
                rowIdx++;
                rowIdx = addRow(sheet, rowIdx, "Oprema", headerStyle);
                Row header = sheet.createRow(rowIdx++);
                String[] cols = {"Resurs", "Potrebno", "Dodeljeno", "Na stanju", "Pokriveno"};
                for (int i = 0; i < cols.length; i++) {
                    Cell cell = header.createCell(i);
                    cell.setCellValue(cols[i]);
                    cell.setCellStyle(headerStyle);
                }
                for (var o : podaci.getStavkeOpreme()) {
                    Row row = sheet.createRow(rowIdx++);
                    setCell(row, 0, o.getNazivResursa());
                    setCell(row, 1, String.valueOf(o.getPotrebnaKolicina()));
                    setCell(row, 2, String.valueOf(o.getDodeljeno()));
                    setCell(row, 3, String.valueOf(o.getDostupnoNaStanju()));
                    setCell(row, 4, o.isPokriveno() ? "Da" : "Ne");
                }
            }

            if (podaci.getStavkeNabavke() != null && !podaci.getStavkeNabavke().isEmpty()) {
                rowIdx++;
                rowIdx = addRow(sheet, rowIdx, "Nabavke", headerStyle);
                Row header = sheet.createRow(rowIdx++);
                String[] cols = {"ID", "Status", "Dobavljač", "Stavki", "Vrednost"};
                for (int i = 0; i < cols.length; i++) {
                    Cell cell = header.createCell(i);
                    cell.setCellValue(cols[i]);
                    cell.setCellStyle(headerStyle);
                }
                for (var n : podaci.getStavkeNabavke()) {
                    Row row = sheet.createRow(rowIdx++);
                    setCell(row, 0, n.getNabavkaId() != null ? String.valueOf(n.getNabavkaId()) : "—");
                    setCell(row, 1, n.getStatus());
                    setCell(row, 2, n.getDobavljacNaziv());
                    setCell(row, 3, String.valueOf(n.getBrojStavki()));
                    setCell(row, 4, PdfIzvestajGenerator.formatMoney(n.getUkupnaVrednost()));
                }
            }

            if (podaci.getStavkeBudzeta() != null && !podaci.getStavkeBudzeta().isEmpty()) {
                rowIdx++;
                rowIdx = addRow(sheet, rowIdx, "Budžet", headerStyle);
                Row header = sheet.createRow(rowIdx++);
                String[] cols = {"Budžet", "Kategorija", "Planirano", "Stvarno", "Iskorišćenost %"};
                for (int i = 0; i < cols.length; i++) {
                    Cell cell = header.createCell(i);
                    cell.setCellValue(cols[i]);
                    cell.setCellStyle(headerStyle);
                }
                for (var b : podaci.getStavkeBudzeta()) {
                    Row row = sheet.createRow(rowIdx++);
                    setCell(row, 0, b.getNazivBudzeta());
                    setCell(row, 1, b.getKategorijaNaziv());
                    setCell(row, 2, PdfIzvestajGenerator.formatMoney(b.getPlanirano()));
                    setCell(row, 3, PdfIzvestajGenerator.formatMoney(b.getStvarno()));
                    setCell(row, 4, b.getIskoriscenostProcenat() != null
                            ? b.getIskoriscenostProcenat().toPlainString() + "%" : "—");
                }
            }

            if (podaci.getFakture() != null && !podaci.getFakture().isEmpty()) {
                rowIdx++;
                rowIdx = addRow(sheet, rowIdx, "Fakture", headerStyle);
                rowIdx = addFakturaHeader(sheet, rowIdx, headerStyle);
                for (var f : podaci.getFakture()) {
                    Row row = sheet.createRow(rowIdx++);
                    setCell(row, 0, f.getBrojFakture());
                    setCell(row, 1, f.getDatum());
                    setCell(row, 2, PdfIzvestajGenerator.formatMoney(f.getUkupanIznos()));
                    setCell(row, 3, PdfIzvestajGenerator.formatMoney(f.getPlaceniIznos()));
                }
            }

            if (podaci.getUgovori() != null && !podaci.getUgovori().isEmpty()) {
                rowIdx++;
                rowIdx = addRow(sheet, rowIdx, "Ugovori", headerStyle);
                Row header = sheet.createRow(rowIdx++);
                String[] cols = {"Broj", "Predmet", "Status", "Vrednost", "Važi do"};
                for (int i = 0; i < cols.length; i++) {
                    Cell cell = header.createCell(i);
                    cell.setCellValue(cols[i]);
                    cell.setCellStyle(headerStyle);
                }
                for (var u : podaci.getUgovori()) {
                    Row row = sheet.createRow(rowIdx++);
                    setCell(row, 0, u.getBrojUgovora());
                    setCell(row, 1, u.getPredmet());
                    setCell(row, 2, u.getStatus());
                    setCell(row, 3, PdfIzvestajGenerator.formatMoney(u.getVrednost()));
                    setCell(row, 4, u.getVaziDo());
                }
            }

            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Greška pri generisanju Excel izveštaja.", ex);
        }
    }

    private int addRow(Sheet sheet, int rowIdx, String value, CellStyle style) {
        Row row = sheet.createRow(rowIdx++);
        Cell cell = row.createCell(0);
        cell.setCellValue(value != null ? value : "");
        if (style != null) {
            cell.setCellStyle(style);
        }
        return rowIdx;
    }

    private int addPair(Sheet sheet, int rowIdx, String label, String value) {
        Row row = sheet.createRow(rowIdx++);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
        return rowIdx;
    }

    private int addFakturaHeader(Sheet sheet, int rowIdx, CellStyle style) {
        Row row = sheet.createRow(rowIdx++);
        String[] cols = {"Broj", "Datum", "Ukupan iznos", "Plaćeni iznos"};
        for (int i = 0; i < cols.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(style);
        }
        return rowIdx;
    }

    private void setCell(Row row, int col, String value) {
        row.createCell(col).setCellValue(value != null ? value : "—");
    }
}
