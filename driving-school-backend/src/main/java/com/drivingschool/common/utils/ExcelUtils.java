package com.drivingschool.common.utils;

import com.drivingschool.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Excel 导出工具类。
 * <p>
 * 提供将数据导出为 Excel（.xlsx）文件并写入 HTTP 响应的功能。
 * 使用 Apache POI 库操作 Excel 文件。
 * </p>
 * <p>
 * 使用方式：
 * 1. 将查询结果封装为 List&lt;Map&lt;String, Object&gt;&gt; 格式
 * 2. 调用 export 方法，Excel 会直接写入 HttpServletResponse 的输出流
 * 3. 浏览器会自动下载生成的 Excel 文件
 * </p>
 * <p>
 * 数据格式要求：
 * - List 中的每个 Map 代表一行数据
 * - Map 的 key 为列名（同时作为表头），value 为单元格值
 * - 所有行的 Map 应保持相同的 key 结构
 * - 支持的值类型：Number、Boolean、LocalDate、LocalDateTime、String
 * </p>
 */
public class ExcelUtils {

    /** 私有构造方法，防止实例化工具类 */
    private ExcelUtils() {}

    /**
     * 将数据导出为 Excel 文件并写入 HTTP 响应。
     * <p>
     * 生成的 Excel 文件包含：
     * - 加粗且灰色背景的表头行
     * - 根据内容自动调整的列宽
     * - UTF-8 编码的文件名（支持中文文件名）
     * </p>
     *
     * @param response HTTP 响应对象，用于输出 Excel 文件流
     * @param sheetName Excel 工作表名称
     * @param data     要导出的数据列表，每个 Map 代表一行，key 为列名
     * @param filename 下载文件名（不含扩展名，会自动添加 .xlsx）
     * @throws BusinessException 当数据为空或导出过程发生 IO 异常时抛出
     */
    public static void export(HttpServletResponse response, String sheetName,
                              List<Map<String, Object>> data, String filename) {
        // 校验数据是否为空
        if (data == null || data.isEmpty()) {
            throw new BusinessException("没有可导出的数据");
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            // 创建工作表
            Sheet sheet = workbook.createSheet(sheetName);

            // 创建表头样式：加粗字体 + 灰色背景
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // 从第一行数据中获取列名，作为 Excel 表头
            Map<String, Object> firstRow = data.get(0);
            String[] headers = firstRow.keySet().toArray(new String[0]);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 写入数据行（从第二行开始，第一行是表头）
            for (int i = 0; i < data.size(); i++) {
                Row row = sheet.createRow(i + 1);
                Map<String, Object> rowData = data.get(i);
                for (int j = 0; j < headers.length; j++) {
                    Cell cell = row.createCell(j);
                    Object value = rowData.get(headers[j]);
                    setCellValue(cell, value);
                }
            }

            // 自动调整每列宽度以适应内容
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 设置 HTTP 响应头，触发浏览器下载
            // 使用 RFC 5987 编码支持中文文件名
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename + ".xlsx");

            // 将工作簿写入响应输出流
            try (OutputStream out = response.getOutputStream()) {
                workbook.write(out);
                out.flush();
            }
        } catch (IOException e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    /**
     * 设置单元格的值，根据值的类型自动选择合适的单元格类型。
     * <p>
     * 支持的类型映射：
     * - null -> 空字符串
     * - Number -> 数值类型（doubleValue）
     * - Boolean -> 布尔类型
     * - LocalDate / LocalDateTime -> 字符串类型（toString）
     * - 其他 -> 字符串类型（toString）
     * </p>
     *
     * @param cell  要设置值的单元格
     * @param value 单元格的值
     */
    private static void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof java.time.LocalDate) {
            cell.setCellValue(value.toString());
        } else if (value instanceof java.time.LocalDateTime) {
            cell.setCellValue(value.toString());
        } else {
            cell.setCellValue(value.toString());
        }
    }
}
