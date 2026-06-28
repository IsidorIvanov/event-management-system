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
