/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.framework.util.excel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExcelBuilder {
    private final Class<? extends Workbook> workbookClass;
    private Workbook workbook;
    //private Sheet sheet;
    // private String sheetName;
    //private List<String> headerList;
    //private List<String> columnList;
    //private List<Map<String, Object>> dataList;
    private HSSFColor.HSSFColorPredefined headBgColor;
    private HSSFColor.HSSFColorPredefined headFontColor;
    private HSSFColor.HSSFColorPredefined borderColor;
    private Integer columnWidth;
    private List<SheetBuilder> sheetBuilderList = new ArrayList<>();


    public ExcelBuilder(Class<? extends Workbook> workbookClass) {
        this.workbookClass = workbookClass;
    }

    public ExcelBuilder withWorkbook(Workbook workbook) {
        this.workbook = workbook;
        return this;
    }


    public SheetBuilder addSheet(String sheetName) {
        SheetBuilder sheetBuilder = new SheetBuilder(sheetName);
        sheetBuilderList.add(sheetBuilder);
        return sheetBuilder;
    }

    public SheetBuilder addSheet(SheetBuilder sheetBuilder) {
        sheetBuilderList.add(sheetBuilder);
        return sheetBuilder;
    }

    public ExcelBuilder withColumnWidth(Integer columnWidth) {
        this.columnWidth = columnWidth;
        return this;
    }

   /* public ExcelBuilder withSheetName(String sheetName) {
        this.sheetName = sheetName;
        return this;
    }

    public ExcelBuilder withHeaderList(List<String> headerList) {
        this.headerList = headerList;
        return this;
    }

    public ExcelBuilder withColumnList(List<String> columnList) {
        this.columnList = columnList;
        return this;
    }

    public ExcelBuilder withDataList(List<Map<String, Object>> dataList) {
        this.dataList = dataList;
        return this;
    }*/

    public ExcelBuilder withHeadBgColor(HSSFColor.HSSFColorPredefined color) {
        this.headBgColor = color;
        return this;
    }

    public ExcelBuilder withHeadFontColor(HSSFColor.HSSFColorPredefined color) {
        this.headFontColor = color;
        return this;
    }

    public ExcelBuilder withBorderColor(HSSFColor.HSSFColorPredefined color) {
        this.borderColor = color;
        return this;
    }

    public HSSFColor.HSSFColorPredefined getBorderColor() {
        return this.borderColor;
    }

    private void makeupBody(Cell cell) {
        if (workbook != null) {
            CellStyle style = workbook.createCellStyle();
            if (this.borderColor != null) {
                style.setBottomBorderColor(borderColor.getIndex());
                style.setTopBorderColor(borderColor.getIndex());
                style.setLeftBorderColor(borderColor.getIndex());
                style.setRightBorderColor(borderColor.getIndex());
            }
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            cell.setCellStyle(style);
        }
    }

    private void makeupHeader(Cell cell) {
        if (workbook != null) {
            CellStyle style = workbook.createCellStyle();
            if (this.headBgColor != null) {
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                style.setFillBackgroundColor(headBgColor.getIndex());// 设置背景色
                style.setFillForegroundColor(headBgColor.getIndex());
            }
            if (this.headFontColor != null) {
                Font font = workbook.createFont();
                font.setColor(headFontColor.getIndex());
                style.setFont(font);
            }
            if (this.borderColor != null) {
                style.setBottomBorderColor(borderColor.getIndex());
                style.setTopBorderColor(borderColor.getIndex());
                style.setLeftBorderColor(borderColor.getIndex());
                style.setRightBorderColor(borderColor.getIndex());
            }
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            cell.setCellStyle(style);
        }
    }

    public Workbook build() {
        if (this.workbook == null) {
            try {
                this.workbook = (Workbook) (Class.forName(workbookClass.getName()).newInstance());
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                throw new RuntimeException("无法使用" + workbookClass.getSimpleName() + "创建工作簿：" + e.getMessage(), e);
            }
        }
        if (!this.workbook.getClass().getSimpleName().equals(workbookClass.getSimpleName())) {
            throw new RuntimeException("工作簿类型不正确，定义是:" + workbookClass.getSimpleName() + "，实际是：" + workbook.getClass().getSimpleName());
        }


        if (CollectionUtils.isNotEmpty(this.sheetBuilderList)) {
            for (SheetBuilder sheetBuilder : this.sheetBuilderList) {
                Sheet sheet = null;
                if (StringUtils.isNotBlank(sheetBuilder.getSheetName())) {
                    sheet = this.workbook.createSheet(sheetBuilder.getSheetName());
                } else {
                    sheet = this.workbook.createSheet();
                }
                sheetBuilder.setExcelBuilder(this);
                sheetBuilder.setSheet(sheet);
                sheetBuilder.setWorkbook(this.workbook);
                //设置列宽
                if (this.columnWidth != null && CollectionUtils.isNotEmpty(sheetBuilder.getColumnList())) {
                    for (int i = 0; i < sheetBuilder.getColumnList().size(); i++) {
                        sheet.setColumnWidth(i, this.columnWidth * 256);
                    }
                } else {
                    sheet.setDefaultColumnWidth(15);
                }
                //生成标题行
                if (CollectionUtils.isNotEmpty(sheetBuilder.getHeaderList())) {
                    Row headerRow = sheet.createRow(0);
                    int i = 0;
                    for (String header : sheetBuilder.getHeaderList()) {
                        Cell cell = headerRow.createCell(i);
                        makeupHeader(cell);
                        cell.setCellValue(header);
                        i++;
                    }
                }
                //生成数据行
                if (CollectionUtils.isNotEmpty(sheetBuilder.getColumnList()) && CollectionUtils.isNotEmpty(sheetBuilder.getColumnList()) && CollectionUtils.isNotEmpty(sheetBuilder.getDataList())) {
                    int lastRowNum = sheet.getLastRowNum();
                    for (Map<String, Object> dataMap : sheetBuilder.getDataList()) {
                        lastRowNum++;
                        Row row = sheet.createRow(lastRowNum);
                        int j = 0;
                        for (String column : sheetBuilder.getColumnList()) {
                            Cell cell = row.createCell(j);
                            makeupBody(cell);
                            cell.setCellValue(dataMap.get(column) == null ? null : dataMap.get(column).toString());
                            j++;
                        }
                    }
                }
            }
        }

        return this.workbook;
    }

    /*
    public void addRow(Map<String, Object> dataMap) {
        if (this.workbook == null) {
            this.build();
        }
        if (this.workbook != null && CollectionUtils.isNotEmpty(columnList)) {
            int lastRowNum = sheet.getLastRowNum();
            lastRowNum++;
            Row row = sheet.createRow(lastRowNum);
            int j = 0;
            for (String column : columnList) {
                Cell cell = row.createCell(j);
                makeupBody(cell);
                cell.setCellValue(dataMap.get(column) == null ? null : dataMap.get(column).toString());
                j++;
            }
        }
    }*/

    /*public static void main(String[] a) {
        ExcelBuilder builder = new ExcelBuilder(HSSFWorkbook.class);
        builder.build();
    }*/
}