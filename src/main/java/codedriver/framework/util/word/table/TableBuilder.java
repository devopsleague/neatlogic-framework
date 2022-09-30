/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.framework.util.word.table;

import codedriver.framework.util.word.enums.FontFamily;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author longrf
 * @date 2022/9/23 17:21
 */

public class TableBuilder {

    XWPFTable table;

    private Map<Integer, String> tableHeaderMap;

    String fontFamily;

    double spacingBetween;

    int spacingBeforeLines;

    int spacingAfterLines;

    /**
     * 不含表头建表
     *
     * @param table table
     */
    public TableBuilder(XWPFTable table) {
        this.table = table;
        //表风格
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        tblPr.getTblW().setType(STTblWidth.DXA);
        //默认表格宽度
        tblPr.getTblW().setW(new BigInteger("8310"));
    }

    /**
     * 含有表头建表
     *
     * @param table          table
     * @param tableHeaderMap 表头 1->列1 , 2->列2 , 3->列3
     */
    public TableBuilder(XWPFTable table, Map<Integer, String> tableHeaderMap) {
        this.table = table;
        //表风格
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        tblPr.getTblW().setType(STTblWidth.DXA);
        //默认表格宽度
        tblPr.getTblW().setW(new BigInteger("8310"));

        this.tableHeaderMap = tableHeaderMap;
        XWPFTableRow row = table.getRow(0);
        for (int cellNum = 1; cellNum <= tableHeaderMap.size(); cellNum++) {
            XWPFTableCell cell = cellNum == 1 ? row.getCell(0) : row.addNewTableCell();
            setCellText(cell, tableHeaderMap.get(cellNum), table.getWidth() / tableHeaderMap.size());

//            for (XWPFParagraph paragraph : xwpfTableCell.getParagraphs()) {
//                ParagraphBuilder paragraphBuilder = new ParagraphBuilder(paragraph);
//                paragraphBuilder.setTextPosition(2);
//                paragraphBuilder.setColor(FontColor.RED.getValue());
//                xwpfTableCell.setParagraph(paragraphBuilder.builder());
//            }


//for ()
//            XWPFParagraph para = xwpfTableCell.getParagraphs();
//            para.setSpacingBeforeLines(2);
//            para.setSpacingAfterLines(2);
//            for (XWPFRun run : para.getRuns()) {
//                run.setTextPosition(2);
//            }
//            para.setSpacingBetween(2);

////            //单元格风格 、 平分表格宽度
//            CTTcPr tcpr = cell.getCTTc().addNewTcPr();
////            CTTblWidth cellw = tcpr.addNewTcW();
////            cellw.setType(STTblWidth.DXA);
////            cellw.setW(BigInteger.valueOf(2077));
////
//            //垂直居中
//            CTVerticalJc va = tcpr.addNewVAlign();
//            va.setVal(STVerticalJc.CENTER);
//
//
//            //水平居中
//            List<XWPFParagraph> paragraphs = cell.getParagraphs();
//            for (XWPFParagraph paragraph : paragraphs) {
//                paragraph.setAlignment(ParagraphAlignment.CENTER);
//            }
        }
    }

    /**
     * 添加表头
     *
     * @param tableHeaderMap 表头 1->列1 , 2->列2 , 3->列3
     * @return TableBuilder
     */
    public TableBuilder addTableHeaderMap(Map<Integer, String> tableHeaderMap) {
        if (MapUtils.isEmpty(tableHeaderMap)) {
            return this;
        }
        this.tableHeaderMap = tableHeaderMap;

        CTTbl ctTbl = table.getCTTbl();
        CTTblPr tblPr = ctTbl.getTblPr() == null ? ctTbl.addNewTblPr() : ctTbl.getTblPr();
        CTTblWidth tblWidth = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr.addNewTblW();
        tblWidth.setType(STTblWidth.DXA);
        //默认表格宽度
        tblWidth.setW(new BigInteger("8310"));

        XWPFTableRow titleRow = table.getRow(0);
//            titleRow.setHeight(300);
        for (int cellNum = 1; cellNum <= tableHeaderMap.size(); cellNum++) {
            XWPFTableCell cell;
            if (cellNum == 1) {
                cell = titleRow.getCell(0);
            } else {
                cell = titleRow.createCell();
            }
            setCellText(cell, tableHeaderMap.get(cellNum), table.getWidth() / tableHeaderMap.size());
        }

        return this;
    }

    /**
     * 获取表头
     *
     * @return tableHeaderMap
     */
    private Map<Integer, String> getTableHeaderMap() {
        if (MapUtils.isEmpty(tableHeaderMap)) {
            XWPFTableRow row = table.getRows().get(0);
            int i = 1;
            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    for (XWPFRun run : paragraph.getRuns()) {
                        tableHeaderMap.put(i, run.toString());
                        i++;
                    }
                }
            }
        }
        return tableHeaderMap;
    }

    /**
     * 添加空行数据
     *
     * @return table
     */
    public TableBuilder addBlankRow() {
        table.createRow();
        return this;
    }

    /**
     * 添加一行数据
     *
     * @param cellMap Map（表头,值）
     * @return TableBuilder
     */
    public TableBuilder addRow(Map<String, String> cellMap) {
        if (MapUtils.isEmpty(cellMap)) {
            return this;
        }
        XWPFTableRow row = table.createRow();
//        row.setHeight(300);
        Map<Integer, String> valueMap = new HashMap<>();
        Map<Integer, String> tableHeaderMap = getTableHeaderMap();
        for (Map.Entry<Integer, String> headerEntry : tableHeaderMap.entrySet()) {
            valueMap.put(headerEntry.getKey(), cellMap.get(headerEntry.getValue()));
        }

        List<XWPFTableCell> tableCells = row.getTableCells();
        for (int cellNum = 1; cellNum <= tableCells.size(); cellNum++) {
            XWPFTableCell cell = tableCells.get(cellNum - 1);
            setCellText(cell, valueMap.get(cellNum), table.getWidth() / tableHeaderMap.size());
        }
        return this;
    }

    /**
     * 设置单元格内容
     *
     * @param cell  cell
     * @param text  文本内容
     * @param width 列宽
     */
    private void setCellText(XWPFTableCell cell, String text, int width) {
        cell.setWidthType(TableWidthType.DXA);

        CTTc cttc = cell.getCTTc();
        CTTblWidth ctTblWidth = cell.getCTTc().addNewTcPr().addNewTcW();
        ctTblWidth.setW(BigInteger.valueOf(width));
        ctTblWidth.isSetW();
        //水平居中
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        CTTcPr ctPr = cttc.addNewTcPr();
        //垂直居中
        ctPr.addNewVAlign().setVal(STVerticalJc.CENTER);
        cttc.getPList().get(0).addNewPPr().addNewJc().setVal(STJc.CENTER);

        CTP ctP = (cell.getCTTc().sizeOfPArray() == 0) ? cell.getCTTc().addNewP() : cell.getCTTc().getPArray(0);
        XWPFParagraph para = cell.getParagraph(ctP);
        cell.setParagraph(para);
        //默认1倍行距
        para.setSpacingBetween(spacingBetween != 0 ? spacingBetween : 1);
        //默认0.5行段前距离
        para.setSpacingBeforeLines(spacingBeforeLines != 0 ? spacingBeforeLines : 50);
        //默认0.5行段后距离
        para.setSpacingAfterLines(spacingAfterLines != 0 ? spacingAfterLines : 50);
        XWPFRun run = para.createRun();
        run.setFontFamily(StringUtils.isNotBlank(this.fontFamily) ? this.fontFamily : FontFamily.BLACK.getValue());
        run.setText(text);
    }

    /**
     * 设置表格宽度 默认 8310
     *
     * @param with 宽度
     * @return tableBuilder
     */
    public TableBuilder setTableWith(String with) {
        CTTbl ctTbl = table.getCTTbl();
        CTTblPr tblPr = ctTbl.getTblPr() == null ? ctTbl.addNewTblPr() : ctTbl.getTblPr();
        CTTblWidth tblWidth = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr.addNewTblW();
        tblWidth.setType(STTblWidth.DXA);
        tblWidth.setW(new BigInteger(with));
        return this;
    }

    /**
     * 添加多行数据
     *
     * @param cellMapList mapList（表头,值）
     * @return TableBuilder
     */
    public TableBuilder addRows(List<Map<String, String>> cellMapList) {
        if (CollectionUtils.isEmpty(cellMapList)) {
            return this;
        }

        for (Map<String, String> cellMap : cellMapList) {
            XWPFTableRow row = table.createRow();
//            row.setHeight(300);
            Map<Integer, String> valueMap = new HashMap<>();
            Map<Integer, String> tableHeaderMap = getTableHeaderMap();
            for (Map.Entry<Integer, String> headerEntry : tableHeaderMap.entrySet()) {
                valueMap.put(headerEntry.getKey(), cellMap.get(headerEntry.getValue()));
            }

            List<XWPFTableCell> tableCells = row.getTableCells();
            for (int cellNum = 1; cellNum <= tableCells.size(); cellNum++) {
                XWPFTableCell cell = tableCells.get(cellNum - 1);
                setCellText(cell, valueMap.get(cellNum), table.getWidth() / tableHeaderMap.size());
            }
        }
        return this;
    }

    /**
     * 设置字体，默认黑体
     *
     * @param fontFamily 字体
     * @return tableBuilder
     */
    public TableBuilder setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
        return this;
    }

    /**
     * 设置行距倍数 默认1倍
     *
     * @param spacing 行距倍数
     * @return tableBuilder
     */
    public TableBuilder setSpacingBetween(double spacing) {
        this.spacingBetween = spacing;
        return this;
    }

    /**
     * 设置段前距离 默认50（0.5行）
     *
     * @param spacingBeforeLines 段前距离
     * @return tableBuilder
     */
    public TableBuilder setSpacingBeforeLines(int spacingBeforeLines) {
        this.spacingBeforeLines = spacingBeforeLines;
        return this;
    }

    /**
     * 设置段后距离 默认50（0.5行）
     *
     * @param spacingAfterLines 段后距离
     * @return tableBuilder
     */
    public TableBuilder setSpacingAfterLines(int spacingAfterLines) {
        this.spacingAfterLines = spacingAfterLines;
        return this;
    }
}
