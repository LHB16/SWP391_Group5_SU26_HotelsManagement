package vn.edu.fpt.hotel_management.selenium;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelHelper {

    public static List<Map<String, String>> readTestData(String filePath) throws IOException {
        List<Map<String, String>> testDataList = new ArrayList<>();
        
        File file = new File(filePath);
        if (!file.exists()) {
            return testDataList;
        }

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return testDataList;
            }

            int colCount = headerRow.getLastCellNum();
            List<String> headers = new ArrayList<>();
            for (int j = 0; j < colCount; j++) {
                headers.add(getCellValueAsString(headerRow.getCell(j)));
            }

            int rowCount = sheet.getLastRowNum();
            for (int i = 1; i <= rowCount; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, String> rowData = new HashMap<>();
                boolean isEmptyRow = true;
                for (int j = 0; j < colCount; j++) {
                    String value = getCellValueAsString(row.getCell(j));
                    rowData.put(headers.get(j), value);
                    if (!value.isEmpty()) {
                        isEmptyRow = false;
                    }
                }
                if (!isEmptyRow) {
                    testDataList.add(rowData);
                }
            }
        }
        return testDataList;
    }

    public static void writeTestReport(String filePath, List<Map<String, String>> reportData) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Test Report");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Pass Style
            CellStyle passStyle = workbook.createCellStyle();
            Font passFont = workbook.createFont();
            passFont.setColor(IndexedColors.GREEN.getIndex());
            passFont.setBold(true);
            passStyle.setFont(passFont);

            // Fail Style
            CellStyle failStyle = workbook.createCellStyle();
            Font failFont = workbook.createFont();
            failFont.setColor(IndexedColors.RED.getIndex());
            failFont.setBold(true);
            failStyle.setFont(failFont);

            // Headers
            String[] headers = {"Test Case ID", "Username", "Hotel Name", "Room Type", "Description", "Status", "Execution Time (ms)", "Note"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (Map<String, String> rowData : reportData) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rowData.getOrDefault("TestCaseID", "TC_" + (rowNum - 1)));
                row.createCell(1).setCellValue(rowData.getOrDefault("Username", ""));
                row.createCell(2).setCellValue(rowData.getOrDefault("HotelName", ""));
                row.createCell(3).setCellValue(rowData.getOrDefault("RoomType", ""));
                row.createCell(4).setCellValue(rowData.getOrDefault("Description", ""));
                
                Cell statusCell = row.createCell(5);
                String status = rowData.getOrDefault("Status", "FAIL");
                statusCell.setCellValue(status);
                if ("PASS".equalsIgnoreCase(status)) {
                    statusCell.setCellStyle(passStyle);
                } else {
                    statusCell.setCellStyle(failStyle);
                }

                row.createCell(6).setCellValue(rowData.getOrDefault("ExecutionTime", "0"));
                row.createCell(7).setCellValue(rowData.getOrDefault("Note", ""));
            }

            // Auto-fit columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        }
    }


    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.format("%d", (long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
