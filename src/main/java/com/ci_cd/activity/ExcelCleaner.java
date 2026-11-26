package com.ci_cd.activity;

import org.apache.poi.ss.usermodel.*;
import java.math.BigDecimal;

public class ExcelCleaner {

        private static final DataFormatter formatter = new DataFormatter();

        // Lee cualquier celda como String limpio (o null si está vacía o es basura)
        public static String getCleanString(Row row, int colIndex) {
            Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell == null) return null;

            String valor = formatter.formatCellValue(cell).trim();
            if (valor.isEmpty() || valor.equalsIgnoreCase("NO HAY")
                    || valor.equalsIgnoreCase("NINGUNO") || valor.equalsIgnoreCase("POR RELLENAR")) {
                return null;
            }
            return valor;
        }

        // Lee precios o coordenadas manejando tanto formato numérico como texto con coma
        public static BigDecimal getCleanDecimal(Row row, int colIndex) {
            Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell == null) return null;

            try {
                if (cell.getCellType() == CellType.NUMERIC) {
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                }
                String texto = formatter.formatCellValue(cell).trim().replace(",", ".");
                return texto.isEmpty() ? null : new BigDecimal(texto);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // Normaliza el texto largo de "Tipo venta" a su sigla oficial (P, R, M)
        public static String normalizarTipoVenta(String raw) {
            if (raw == null) return null;
            String lower = raw.toLowerCase();
            if (lower.contains("pesqueros")) return "P";
            if (lower.contains("deportivos") || lower.contains("recreo")) return "R";
            if (lower.contains("mixto") || lower.contains("mixta")) return "M";
            return raw;
        }
}

