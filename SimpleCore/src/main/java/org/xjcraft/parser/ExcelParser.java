package org.xjcraft.parser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.compress.utils.Lists;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.*;
import org.xjcraft.adapter.Pluggable;
import org.xjcraft.annotation.Comment;
import org.xjcraft.annotation.*;
import org.xjcraft.api.SimpleConfigurationSerializable;
import org.xjcraft.utils.Logger;
import org.xjcraft.utils.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class ExcelParser {

    public static final String ID_IDENTIFIER = "id";
    public static final String CLASS_IDENTIFIER = "==";
    public static final String KEY_IDENTIFIER = "key";
    public static final String VALUE_IDENTIFIER = "value";
    public static final String CONSTS_SHEET = "Hidden";

    public static void loadExcel(Pluggable plugin, Class clazz, InputStream file) {
        try {
            ExcelFile excelFile = new ExcelFile(plugin, clazz);
            excelFile.read(file);
        } catch (Exception e) {
            plugin.getLoggerApi().warning("excel配置加载错误！");
            e.printStackTrace();
        }
    }


    public static void saveExcel(Pluggable plugin, Class clazz, OutputStream file) {
        try {
//            plugin.getLogger().info("start saving:" + file);
            Field field = clazz.getDeclaredField("config");
            field.setAccessible(true);
            Object o = clazz.cast(field.get(null));
            field.setAccessible(false);
            ExcelFile excelFile = new ExcelFile(plugin, clazz);
            excelFile.write(file, o);
        } catch (Exception e) {
            Logger.getLogger().warning("excel配置保存错误！" + file);
            e.printStackTrace();
        }

    }

    public static class ExcelFile {
        Map<String, Sheet> map = new LinkedHashMap<String, Sheet>() {{
        }};
        LinkedHashMap<String, Constrat> consts = new LinkedHashMap<>();
        private Pluggable plugin;
        private Class clazz;

        public ExcelFile(Pluggable plugin, Class clazz) {
            this.plugin = plugin;
            this.clazz = clazz;
            Sheet main = new Sheet("main");
            map.put("main", main);
            parserHeader(clazz, "main", "");
        }

        private String parserHeader(Class clazz, String sheetName, String parent) {
            if (sheetName.length() > 30) {
                sheetName = String.format("H%s", sheetName.hashCode());
            }
            String finalSheetName = sheetName;
            Sheet sheet = map.computeIfAbsent(sheetName, k -> new Sheet(finalSheetName));
            if (String.class.isAssignableFrom(clazz) || isBasicTypes(clazz) || clazz.isEnum()) {
                Header header = sheet.headers.computeIfAbsent(parent, k -> new Header());
                header.comment = parent;
                header.name = parent;
                header.type = clazz.getName();
                if (clazz.isEnum()) {
                    try {
                        consts.putIfAbsent(clazz.getName(), new Constrat(getEnumStrings(clazz)));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return sheetName;
            }
            if (clazz.isInterface()) {
                String name = join(".", parent, CLASS_IDENTIFIER);
                Header header = sheet.headers.computeIfAbsent(name, k -> new Header());
                header.comment = "数据类型";
                header.name = name;
                header.type = "Class";
                List<Class<?>> classes = plugin.getClasses( clazz.getPackage().getName().replace(plugin.getClass().getPackage().getName() + ".", ""));
                for (Class<?> aClass : classes) {
                    if (aClass != clazz && clazz.isAssignableFrom(aClass)) {
                        parseFields(sheetName, parent, sheet, aClass.getDeclaredFields(), new Header.Condition(name, "%content%5=\"" + aClass.getName() + "\""));
                    }
                }
                consts.putIfAbsent(clazz.getName(), new Constrat(classes.stream().filter(c -> c != clazz && clazz.isAssignableFrom(c)).map(Class::getName).collect(Collectors.toList()).toArray(new String[]{})));

            } else {
                parseFields(sheetName, parent, sheet, clazz.getDeclaredFields(), null);

            }

            return sheetName;
        }

        private void parseFields(String sheetName, String parent, Sheet sheet, Field[] fields, Header.Condition condition) {
            for (Field field : fields) {
                if (field.isAnnotationPresent(Ignore.class)
                        || field.isAnnotationPresent(Excel.class)
                        || field.isAnnotationPresent(Folder.class)
                        || field.isAnnotationPresent(Instance.class)
                        || java.lang.reflect.Modifier.isStatic(field.getModifiers())
                ) continue;
                String name = StringUtil.isEmpty(parent) ? field.getName() : (parent + "." + field.getName());
                String comment = field.isAnnotationPresent(Comment.class) ? field.getAnnotation(Comment.class).value() : "";
                Type genericType = field.getGenericType();
                Class<?> type = field.getType();

                Header header = sheet.headers.computeIfAbsent(name, k -> new Header());
                header.comment = comment;
                header.name = name;
                if (List.class.isAssignableFrom(type)) {
                    if (genericType instanceof ParameterizedType) {
                        Class typeArgument = (Class) ((ParameterizedType) genericType).getActualTypeArguments()[0];
                        if (isBasicTypes(typeArgument)) {
                            header.type = String.format("List<%s>", typeArgument.getSimpleName());

                        } else {
                            header.type = String.format("%s<%s>", type.getName(), typeArgument.getName());
                            header.sheet = parserHeader(typeArgument, join(".", sheetName, name), VALUE_IDENTIFIER);
                        }
                    }
                } else if (Map.class.isAssignableFrom(type)) {
//                    Header header = sheet.headers.computeIfAbsent(name, k -> new Header());
//                    header.comment = comment;
//                    header.name = name;
                    header.type = type.getName();
                    if (genericType instanceof ParameterizedType) {
                        Class typeArgument0 = (Class) ((ParameterizedType) genericType).getActualTypeArguments()[0];
                        Class typeArgument1 = (Class) ((ParameterizedType) genericType).getActualTypeArguments()[1];
                        if (!(String.class.isAssignableFrom(typeArgument0) || typeArgument0.isEnum())) {
                            throw new RuntimeException("不允许使用String或枚举类以外的字段做Map的Key！" + join(".", sheetName, name));
                        }
                        header.type += String.format("<%s,%s>", typeArgument1.getName(), typeArgument1.getName());
                        header.sheet = parserHeader(typeArgument0, join(".", sheetName, name), KEY_IDENTIFIER);
                        header.sheet = parserHeader(typeArgument1, join(".", sheetName, name), VALUE_IDENTIFIER);


                    }
                } else if (SimpleConfigurationSerializable.class.isAssignableFrom(type)) {
//                    Header header = sheet.headers.computeIfAbsent(name, k -> new Header());
//                    header.comment = comment;
//                    header.name = name;
                    header.type = type.getName();
                    parserHeader(type, sheetName, name);

                } else if (String.class.isAssignableFrom(type)) {
//                    Header header = sheet.headers.computeIfAbsent(name, k -> new Header());
//                    header.comment = comment;
//                    header.name = name;
                    header.type = type.getName();
                } else if (isBasicTypes(type)) {
//                    Header header = sheet.headers.computeIfAbsent(name, k -> new Header());
//                    header.comment = comment;
//                    header.name = name;
                    header.type = type.getName();
                } else if (type.isEnum()) {
//                    Header header = sheet.headers.computeIfAbsent(name, k -> new Header());
//                    header.comment = comment;
//                    header.name = name;
                    header.type = type.getName();

                    try {
                        consts.putIfAbsent(type.getName(), new Constrat(getEnumStrings(type)));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    header.type = "type not supported:" + type;
                    System.out.println("type not supported:" + type);
                }
                if (condition != null)
                    header.condition.add(condition);


            }
        }


        public void write(OutputStream file, Object o) throws IOException, IllegalAccessException {
            XSSFWorkbook workbook = new XSSFWorkbook();
            writeConst(workbook);
            for (Map.Entry<String, Sheet> entry : map.entrySet()) {
                XSSFSheet sheet = workbook.getSheet(entry.getKey());
                if (sheet == null)
                    sheet = workbook.createSheet(entry.getKey());
                Sheet value = entry.getValue();
                int i = 0;
                writeCell(sheet, getOrDefaultCell(sheet, 0, 0), "注释");
                writeCell(sheet, getOrDefaultCell(sheet, 1, 0), "字段名");
                writeCell(sheet, getOrDefaultCell(sheet, 2, 0), "字段类型");
                writeCell(sheet, getOrDefaultCell(sheet, 3, 0), "关联表");
                for (Map.Entry<String, Header> headerEntry : value.headers.entrySet()) {
                    i++;
                    writeCell(sheet, getOrDefaultCell(sheet, 0, i), headerEntry.getValue().comment);
                    writeCell(sheet, getOrDefaultCell(sheet, 1, i), headerEntry.getValue().name);
                    writeCell(sheet, getOrDefaultCell(sheet, 2, i), headerEntry.getValue().type);
                    writeCell(sheet, getOrDefaultCell(sheet, 3, i), headerEntry.getValue().sheet);
                    sheet.setColumnWidth(i, headerEntry.getValue().type.equals("Class") ? 30 * 256 : 20 * 256);
                    headerEntry.getValue().index = i;
                    if (headerEntry.getValue().condition.size() > 0) {
                        String s = CellReference.convertNumToColString(i);
                        CellRangeAddress[] regions = new CellRangeAddress[]{CellRangeAddress.valueOf(String.format("$%s$5:$%s$25565", s, s))};
                        ArrayList<ConditionalFormattingRule> cfRules = new ArrayList<>();
                        SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();
                        for (Header.Condition conditionEntry : headerEntry.getValue().condition) {
                            ConditionalFormattingRule rule = sheetCF.createConditionalFormattingRule(StringUtil.applyPlaceHolder(conditionEntry.formatting, new HashMap<String, String>() {{
                                put("content", CellReference.convertNumToColString(value.headers.get(conditionEntry.name).index));
                            }}));
                            //                        Method getCTCfRule = XSSFConditionalFormattingRule.class.getDeclaredMethod("getCTCfRule");
                            //                        getCTCfRule.setAccessible(true);
                            //                        CTCfRule ctCfRule = (CTCfRule) getCTCfRule.invoke(rule);
                            //                        ctCfRule.setStopIfTrue(true);
                            PatternFormatting fill = rule.createPatternFormatting();
                            fill.setFillBackgroundColor(IndexedColors.ORANGE.index);
                            fill.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
                            cfRules.add(rule);

                        }
                        sheetCF.addConditionalFormatting(regions, cfRules.toArray(new ConditionalFormattingRule[]{}));
                    }


                }
            }
            workbook.setActiveSheet(workbook.getSheetIndex(workbook.getSheet("main")));
            writeCell(workbook.getSheet("main"), map.get("main"), "main", new ArrayList<>(map.get("main").headers.keySet()), ID_IDENTIFIER, IndexedColors.GREY_25_PERCENT.getIndex());
            writeData(workbook, map.get("main"), o, clazz, "", "main", true);

            try {
                workbook.write(file);
                file.flush();
            } finally {
                file.close();

            }

        }

        public void writeConst(XSSFWorkbook workbook) {
            if (consts.size() > 0) {
                XSSFSheet sheet = workbook.createSheet(CONSTS_SHEET);
                workbook.setSheetHidden(workbook.getSheetIndex(sheet), true);
                int i = 0;
                for (Map.Entry<String, Constrat> entry : consts.entrySet()) {
                    XSSFCell cell = getOrDefaultCell(sheet, 0, i);
                    writeCell(sheet, cell, entry.getKey());
                    List<String> strings = Arrays.asList(entry.getValue().values);
                    Collections.sort(strings);
                    for (int j = 0; j < strings.size(); j++) {
                        writeCell(sheet, getOrDefaultCell(sheet, j + 1, i), strings.get(j));
                    }
                    entry.getValue().col = i;
                    entry.getValue().parse = String.format("%s!$%s$%s:$%s$%s", CONSTS_SHEET, CellReference.convertNumToColString(i), 2,
                            CellReference.convertNumToColString(i), entry.getValue().values.length + 1);
                    XSSFName name = workbook.createName();
                    name.setNameName(entry.getKey().replace("$", "."));
                    name.setRefersToFormula(entry.getValue().parse);
                    i++;
                }


            }
        }

        private void writeData(XSSFWorkbook workbook, Sheet sheet, Object o, Class clazz, String parent, String id, boolean endLine) throws IllegalAccessException {
            XSSFSheet workbookSheet = workbook.getSheet(sheet.name);
            if (o == null) {

            } else if (o instanceof Map) {
                ArrayList<String> sheetHeaderKeySets = new ArrayList<>(sheet.headers.keySet());
//                int i = 0;
                for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) o).entrySet()) {
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    String entryId = join("_", id, key + "");
                    writeCell(workbookSheet, sheet, id, sheetHeaderKeySets, ID_IDENTIFIER, IndexedColors.GREY_25_PERCENT.getIndex());
                    writeCell(workbookSheet, sheet, key, sheetHeaderKeySets, KEY_IDENTIFIER);
                    if (isBasicTypes(clazz)) {
                        writeCell(workbookSheet, sheet, value, sheetHeaderKeySets, VALUE_IDENTIFIER);
                    } else {
                        writeData(workbook, sheet, value, clazz, VALUE_IDENTIFIER, entryId, false);
                    }
                    sheet.index++;
//                    i++;
                }

            } else if (o instanceof List) {
                ArrayList<String> sheetHeaderKeySets = new ArrayList<>(sheet.headers.keySet());
                for (int i = 0; i < ((List) o).size(); i++) {
                    Object entry = ((List) o).get(i);
                    String entryId = join("_", id, i + "");
                    writeCell(workbookSheet, sheet, id, sheetHeaderKeySets, ID_IDENTIFIER, IndexedColors.GREY_25_PERCENT.getIndex());
                    if (clazz.isInterface()) {
                        int idIndex = sheetHeaderKeySets.indexOf(join(".", parent, CLASS_IDENTIFIER));
                        if (idIndex < 0) throw new RuntimeException("missing id!" + sheet.name);
                        writeCell(workbookSheet, getOrDefaultCell(workbookSheet, sheet.index, idIndex + 1), entry.getClass().getName());
                    }
                    writeData(workbook, sheet, entry, clazz, VALUE_IDENTIFIER, entryId, true);
                }

            } else {
                ArrayList<String> sheetHeaderKeySets = new ArrayList<>(sheet.headers.keySet());
                if (clazz.isInterface()) {
                    int index = writeCell(workbookSheet, sheet, o.getClass().getName(), sheetHeaderKeySets, join(".", parent, CLASS_IDENTIFIER), IndexedColors.ORANGE.getIndex());
//                    List<Class<?>> classes = plugin.getClasses(plugin, clazz.getPackage().getName().replace(plugin.getClass().getPackage().getName() + ".", ""));
//                    String[] array = classes.stream().filter(c -> c != clazz && clazz.isAssignableFrom(c)).map(Class::getName).collect(Collectors.toList()).toArray(new String[]{});

                    writeEnumValidation(clazz.getName(), workbookSheet, index);
                }

                //write fields
                Object value = null;
                for (Field field : o.getClass().getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                    if (field.isAnnotationPresent(Ignore.class)) continue;
                    String index = join(".", parent, field.getName());
                    Header header = sheet.headers.get(index);
                    field.setAccessible(true);
                    Object fieldObject = field.get(o);
                    field.setAccessible(false);
                    Class<?> type = field.getType();
                    int i = sheetHeaderKeySets.indexOf(index);
                    if (i < 0) {
                        System.out.println("missmatched field:" + index);
                        continue;
                    }
                    //write data by type
                    XSSFCell cell = getOrDefaultCell(workbookSheet, sheet.index, i + 1);
                    if (List.class.isAssignableFrom(type)) {
                        if (StringUtil.isEmpty(header.sheet)) {
                            if (fieldObject == null) {
                                writeCell(workbookSheet, cell, null);
                            } else {
                                value = StringUtil.join(((List) fieldObject).toArray(), ";\n");
                                writeCell(workbookSheet, cell, (String) value);
                            }
//                            float height = cell.getRow().getHeightInPoints();
//                            float v = (((List) fieldObject).size() * 14.25f);
//                            cell.getRow().setHeightInPoints(Math.max(v, height));
                        } else {
                            Type genericType = field.getGenericType();
                            if (genericType instanceof ParameterizedType) {
                                type = (Class) ((ParameterizedType) genericType).getActualTypeArguments()[0];
                            }
                            writeCell(workbookSheet, cell, id);
                            writeData(workbook, this.map.get(header.sheet), fieldObject, type, VALUE_IDENTIFIER, id, true);
                        }

                    } else if (Map.class.isAssignableFrom(type)) {
                        Type genericType = field.getGenericType();
                        if (genericType instanceof ParameterizedType) {
                            type = (Class) ((ParameterizedType) genericType).getActualTypeArguments()[1];
                        }
                        writeCell(workbookSheet, cell, id);
                        writeData(workbook, this.map.get(header.sheet), fieldObject, type, VALUE_IDENTIFIER, id, true);
                    } else if (SimpleConfigurationSerializable.class.isAssignableFrom(type)) {
                        writeData(workbook, sheet, fieldObject, type, index, id, false);
                    } else if (isBasicTypes(type)) {
                        writeCell(workbookSheet, cell, fieldObject);
                    } else if (type.isEnum()) {
                        writeCell(workbookSheet, cell, fieldObject);
                        writeEnumValidation(type, workbookSheet, fieldObject, cell.getColumnIndex());
                    } else {
                        System.out.println("type not supported:" + type);
                        continue;
                    }
                }
                if (endLine)
                    sheet.index++;
            }
        }

        private void writeEnumValidation(Class clazz, XSSFSheet workbookSheet, Object fieldObject, int cell) {
            try {
                String[] array = getEnumStrings(clazz);
                writeEnumValidation(clazz.getName(), workbookSheet, cell);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                System.out.println("fail to write Enum validation for:" + clazz.getName());
                e.printStackTrace();
            }
        }

        private void writeEnumValidation(String name, XSSFSheet workbookSheet, int cell) {
            Constrat constrat = consts.get(name);
            if (constrat == null) return;
            XSSFDataValidationHelper helper = new XSSFDataValidationHelper(workbookSheet);
            DataValidationConstraint constraint = helper.createFormulaListConstraint(name.replace("$", "."));
            CellRangeAddressList addressList = new CellRangeAddressList(4, Short.MAX_VALUE, cell, cell);
            DataValidation validation = helper.createValidation(constraint, addressList);
            validation.setSuppressDropDownArrow(true);
            validation.setShowErrorBox(true);
            workbookSheet.addValidationData(validation);
        }

        private String[] getEnumStrings(Class clazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
            Method values = ((Class<Enum>) clazz).getDeclaredMethod("values");
            Object[] invoke = (Object[]) values.invoke(null);
            ArrayList<String> strings = new ArrayList<>();
            for (Object o : invoke) {
                strings.add(((Enum) o).name());
            }

            return strings.toArray(new String[]{});
        }

        private void writeCell(XSSFSheet workbookSheet, Sheet sheet, Object fieldObject, ArrayList<String> sheetHeaderKeySets, String identifier) {
            writeCell(workbookSheet, sheet, fieldObject, sheetHeaderKeySets, identifier, null);
        }

        private int writeCell(XSSFSheet workbookSheet, Sheet sheet, Object fieldObject, ArrayList<String> sheetHeaderKeySets, String identifier, Short color) {
            int index = sheetHeaderKeySets.indexOf(identifier);
            if (index < 0) {
                Logger.getLogger().warning(sheet.name + " missing field:" + identifier);
                return index;
            }
            XSSFCell cell = getOrDefaultCell(workbookSheet, sheet.index, index + 1);
            writeCell(workbookSheet, cell, fieldObject, color);
            return index + 1;
        }

        private void writeCell(XSSFSheet workbookSheet, XSSFCell cell, Object fieldObject) {
            writeCell(workbookSheet, cell, fieldObject, null);
        }

        private void writeCell(XSSFSheet workbookSheet, XSSFCell cell, Object fieldObject, Short color) {
            XSSFCellStyle style = workbookSheet.getWorkbook().createCellStyle();
            style.setWrapText(true);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            if (color != null) {
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                style.setFillForegroundColor(color);
            }
            if (fieldObject == null) {
                cell.setCellValue(("null"));
            } else {
                Class<?> type = fieldObject.getClass();
                if (type == Boolean.class) {
                    cell.setCellValue(((Boolean) fieldObject));
                } else if (type == String.class) {
                    cell.setCellValue(((String) fieldObject));
                } else if (type == Integer.class) {
                    style.setAlignment(HorizontalAlignment.RIGHT);
                    cell.setCellValue(((Integer) fieldObject));
                } else if (type == Float.class || type == Double.class) {
                    style.setAlignment(HorizontalAlignment.RIGHT);
                    cell.setCellValue(((Number) fieldObject).doubleValue());
                } else if (type.isEnum()) {
                    cell.setCellValue(fieldObject.toString());
                    writeEnumValidation(type, workbookSheet, fieldObject, cell.getColumnIndex());
                }
            }
            cell.setCellStyle(style);
        }

        public void read(InputStream stream) throws Exception {
            try {
                XSSFWorkbook workbook = new XSSFWorkbook(stream);
                verifyHeaders(workbook);
                readIndex(workbook);
                Field config = clazz.getDeclaredField("config");
                config.setAccessible(true);
                Object o = config.get(null);
                readData(workbook, map.get("main"), clazz, o);
                config.setAccessible(false);
            } finally {
                stream.close();
            }
        }

        private void readIndex(XSSFWorkbook workbook) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                XSSFSheet xssfSheet = workbook.getSheetAt(i);
                Sheet sheet = map.get(xssfSheet.getSheetName());
                if (sheet == null) continue;
                XSSFRow xssfRow = xssfSheet.getRow(1);
                for (int j = 1; j < xssfRow.getLastCellNum(); j++) {
                    XSSFCell cell = xssfRow.getCell(j);
                    if (cell == null) continue;
                    String value = cell.getStringCellValue();
                    Header header = sheet.headers.get(value);
                    if (header != null)
                        header.index = j;
                }
            }
        }

        private <T> T readData(XSSFWorkbook workbook, Sheet sheet, Class clazz, T instance) throws Exception {
            return readData(workbook, sheet, clazz, instance, 0);
        }

        private <T> T readData(XSSFWorkbook workbook, Sheet sheet, Class clazz, T instance, int line) throws Exception {
            XSSFSheet xssfSheet = workbook.getSheet(sheet.name);
            String id = null;
            Map<String, Class> impliments = new HashMap<>();
            for (Map.Entry<String, Header> entry : sheet.headers.entrySet()) {
                if (entry.getValue() == null) continue;
                XSSFCell cell = getOrDefaultCell(xssfSheet, sheet.index, line + 1);
//                System.out.println("[ExcelParser]读取主字段 " + entry.getKey() + ":" + cell);
                String[] split = entry.getKey().split("\\.");
                //同级展开读取
                Class tmpClazz = clazz;
                Object tmpInstance = instance;
                Object parentInstance = null;
                for (int j = 0, splitLength = split.length; j < splitLength; j++) {
                    String s = split[j];
                    //排除id或标记
                    if (ID_IDENTIFIER.equals(s)) {
                        id = getCellValue(cell, String.class);
                        continue;
                    } else if (CLASS_IDENTIFIER.equals(s)) {
                        String parentFieldName = split[j - 1];
                        Class<?> name = Class.forName(getCellValue(cell, String.class));
                        Field parentField = parentInstance.getClass().getDeclaredField(parentFieldName);
                        parentField.setAccessible(true);
                        parentField.set(parentInstance, name.newInstance());
                        parentField.setAccessible(false);
                        impliments.put(parentFieldName, name);
                        continue;
                    }
                    try {
                        Field field = tmpClazz.getDeclaredField(s);
                        field.setAccessible(true);
                        if (j == splitLength - 1) {
                            if (StringUtil.isEmpty(entry.getValue().sheet)) {
                                if (SimpleConfigurationSerializable.class.isAssignableFrom(field.getType())) {
                                    if (!field.getType().isInterface()) {
                                        field.set(tmpInstance, field.getType().newInstance());
                                    }

                                } else if (isBasicTypes(field.getType())) {
                                    field.set(tmpInstance, getCellValue(cell, field.getType()));
                                } else if (List.class.isAssignableFrom(field.getType())) {
                                    List<Object> list = getStringListValue(cell, field);
                                    field.set(tmpInstance, list);
                                }
                            } else {
                                System.out.println("[ExcelParser]读取关联表" + entry.getValue().sheet);
                                field.set(tmpInstance, findLinkedObject(workbook.getSheet(entry.getValue().sheet), map.get(entry.getValue().sheet), field.getGenericType(), getCellValue(cell, String.class)));

                            }
                        } else {
                            parentInstance = tmpInstance;
                            tmpInstance = field.get(tmpInstance);
                            if (field.getType().isInterface()) {
                                tmpClazz = tmpInstance.getClass();
                            } else {
                                tmpClazz = field.getType();
                            }

                        }
                        field.setAccessible(false);
                    } catch (NoSuchFieldException e) {
                        Logger.getLogger().info(tmpClazz.getName() + " missing field: " + "s");
//                        e.printStackTrace();
                    }
                }
                line++;
            }
            return instance;


        }

        /**
         * 解析文本列表
         *
         * @param cell
         * @param field
         * @return
         */
        private List<Object> getStringListValue(XSSFCell cell, Field field) {
            List<Object> list = new ArrayList<>();
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType && ((ParameterizedType) genericType).getActualTypeArguments().length > 0) {
                Type type = ((ParameterizedType) genericType).getActualTypeArguments()[0];
                if (type == String.class) {
                    list = (List) getCellValue(cell, field.getType());

                }
            }
            return list;
        }

        /**
         * 解析关联表
         *
         * @param workbookSheet
         * @param sheet
         * @param genericType
         * @param id
         * @return
         * @throws InstantiationException
         * @throws IllegalAccessException
         * @throws ClassNotFoundException
         */
        public Object findLinkedObject(XSSFSheet workbookSheet, Sheet sheet, Type genericType, String id) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
            if (genericType instanceof ParameterizedType) {
                Type rawType = ((ParameterizedType) genericType).getRawType();
                Type[] types = ((ParameterizedType) genericType).getActualTypeArguments();
                Class<?> type0 = (Class) types[0];
                if (List.class.isAssignableFrom((Class<?>) rawType)) {
                    Type type = type0;
                    if (type instanceof Class) {
                        ArrayList<Object> list = new ArrayList<>();
                        for (int i = 4; i < workbookSheet.getPhysicalNumberOfRows(); i++) {
                            String rowId = getCellValue(getOrDefaultCell(workbookSheet, i, 1), String.class);
                            if (id.equals(rowId)) {
                                list.add(readLine(workbookSheet, sheet, (Class) type, i));
                            }
                        }
                        return list;
                    }
                } else if (Map.class.isAssignableFrom((Class<?>) rawType)) {
                    Type type = types[1];
                    Map<Object, Object> map = new LinkedHashMap<>();

                    for (int i = 4; i < workbookSheet.getPhysicalNumberOfRows(); i++) {
                        String rowId = getCellValue(getOrDefaultCell(workbookSheet, i, 1), String.class);
                        if (id.equals(rowId)) {
                            map.put(readCell(workbookSheet, sheet, KEY_IDENTIFIER, i, type0), readLine(workbookSheet, sheet, (Class) type, i));
                        }
                    }
                    return map;
                }
            }
            return null;
        }

        /**
         * 读取格子
         *
         * @param workbookSheet
         * @param sheet
         * @param keyIdentifier
         * @param row
         * @param clazz
         * @param <T>
         * @return
         */
        private <T> T readCell(XSSFSheet workbookSheet, Sheet sheet, String keyIdentifier, int row, Class<T> clazz) {
            ArrayList<String> list = new ArrayList<>(sheet.headers.keySet());
            int i = list.indexOf(keyIdentifier);
            if (i < 0) return null;
            XSSFCell cell = getOrDefaultCell(workbookSheet, row, i + 1);
            return getCellValue(cell, clazz);
        }

        /**
         * 读取行
         *
         * @param workbookSheet
         * @param sheet
         * @param type
         * @param i
         * @return
         * @throws ClassNotFoundException
         * @throws InstantiationException
         * @throws IllegalAccessException
         */
        private Object readLine(XSSFSheet workbookSheet, Sheet sheet, Class type, int i) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
            Object o = new Object();
            int col = 0;
            try {
                if (isBasicTypes(type) || type.isEnum()) {
                    XSSFCell cell = getOrDefaultCell(workbookSheet, i, sheet.headers.get(VALUE_IDENTIFIER).index);
                    return getCellValue(cell, type);
                } else if (!type.isInterface()) {
                    o = type.newInstance();
                }

                for (Map.Entry<String, Header> entry : sheet.headers.entrySet()) {
                    col++;
                    if (entry.getValue() == null) continue;
                    String[] split = entry.getKey().split("\\.");
                    XSSFCell cell = getOrDefaultCell(workbookSheet, i, col);
                    if (split.length == 1 && VALUE_IDENTIFIER.equals(split[0])) {
                        o = getCellValue(cell, type);
                    } else if (split.length > 1 && VALUE_IDENTIFIER.equals(split[0])) {
                        if (CLASS_IDENTIFIER.equals(split[1])) {
                            o = Class.forName(getCellValue(cell, String.class)).newInstance();
                        } else {
                            readFlatValue(workbookSheet, o, entry, split, cell);
                        }
                    }
                }
            } catch (Exception e) {
                Logger.getLogger().info(String.format("读取行失败！sheet:%s,row:%s,col:%s", workbookSheet.getSheetName(), i, col));
                throw e;
            }

            return o;
        }

        /**
         * 读取扁平化数据
         *
         * @param workbookSheet
         * @param o
         * @param entry
         * @param split
         * @param cell
         * @throws InstantiationException
         * @throws IllegalAccessException
         * @throws ClassNotFoundException
         */
        private void readFlatValue(XSSFSheet workbookSheet, Object o, Map.Entry<String, Header> entry, String[] split, XSSFCell cell) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
            Object tempSuperObject = o;
            Object tempObject = o;
            String lastField = "";
            String fieldName = "";
            try {
                for (int j = 1; j < split.length; j++) {
                    fieldName = split[j];
                    if (CLASS_IDENTIFIER.equals(fieldName)) {
                        Object newInstance = Class.forName(getCellValue(cell, String.class)).newInstance();
                        Field declaredField = tempSuperObject.getClass().getDeclaredField(lastField);
                        declaredField.setAccessible(true);
                        declaredField.set(tempSuperObject, newInstance);
                        return;
                    } else {
                        if (tempObject == null) {
                            //遇到没初始化的元素使用变量类型来初始化
                            Field typeFromField = tempSuperObject.getClass().getDeclaredField(lastField);
                            typeFromField.setAccessible(true);
                            tempObject = typeFromField.getType().newInstance();
                            typeFromField.set(tempSuperObject, tempObject);
                            System.out.println(String.format("类型%s中的变量%s没有默认值，可能会在多态解析中出错！", tempSuperObject.getClass(), lastField));
                        }
                        Field field = tempObject.getClass().getDeclaredField(fieldName);
                        field.setAccessible(true);
                        tempSuperObject = tempObject;
                        tempObject = field.get(tempObject);
                    }
                    lastField = fieldName;
                }
                Field field = tempSuperObject.getClass().getDeclaredField(lastField);
                field.setAccessible(true);
                if (isBasicTypes(field.getType()) || field.getType().isEnum()) {
                    field.set(tempSuperObject, getCellValue(cell, field.getType()));
                } else if (List.class.isAssignableFrom(field.getType())) {
                    field.set(tempSuperObject, getStringListValue(cell, field));
                }
                if (!StringUtil.isEmpty(entry.getValue().sheet)) {
                    field.set(tempSuperObject, findLinkedObject(workbookSheet.getWorkbook().getSheet(entry.getValue().sheet), map.get(entry.getValue().sheet), field.getGenericType(), getCellValue(cell, String.class)));
                }

            } catch (NoSuchFieldException e) {
//                plugin.getLogger().info(String.format("ignore mismatched field:%s.%s @ %s", lastField, fieldName, tempSuperObject.getClass()));
            } catch (IllegalArgumentException e) {
                Logger.getLogger().info(String.format("读取行失败！sheet:%s,cell:%s,lastField:%s,field:%s", workbookSheet.getSheetName(), cell.getAddress(), lastField, fieldName));
                e.printStackTrace();
            }
        }

        /**
         * 根据类型获取格子数据
         *
         * @param cell
         * @param type
         * @param <T>
         * @return
         */
        private <T> T getCellValue(XSSFCell cell, Class<T> type) {
            try {
                if (cell == null) return null;
                if (cell.getCellType() == CellType.STRING && "null".equals(cell.getStringCellValue())) return null;
                if (type == String.class) {
                    return (T) cell.getStringCellValue();
                } else if (type == Float.class) {
                    return (T) (Float) (float) cell.getNumericCellValue();
                } else if (type == Double.class) {
                    return (T) (Double) cell.getNumericCellValue();
                } else if (type == Integer.class) {
                    return (T) (Integer) (int) cell.getNumericCellValue();
                } else if (type == Boolean.class) {
                    return (T) (Boolean) cell.getBooleanCellValue();
                } else if (type == List.class) {
                    if (StringUtil.isEmpty(cell.getStringCellValue())) {
                        return (T) Lists.newArrayList();
                    }
                    return (T) Arrays.asList(cell.getStringCellValue().split("[;\\n]+"));
                } else if (type.isEnum()) {
                    return (T) Enum.valueOf((Class<? extends Enum>) type, cell.getStringCellValue());
                }

                return null;
            } catch (Exception e) {
                Logger.getLogger().info(String.format("读取行%s列%s时格式错误,数据内容:%s", cell.getRowIndex(), cell.getColumnIndex(),cell.getStringCellValue()));
                e.printStackTrace();
                throw e;
            }
        }

        /**
         * 解析头
         *
         * @param workbook
         */
        private void verifyHeaders(XSSFWorkbook workbook) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                XSSFSheet xssfSheet = workbook.getSheetAt(i);
                Sheet sheet = map.get(xssfSheet.getSheetName());
                if (sheet == null) continue;
                XSSFRow xssfRow = xssfSheet.getRow(1);
                LinkedHashMap<String, Header> correct = new LinkedHashMap<>();
                for (int j = 1; j < xssfRow.getLastCellNum(); j++) {
                    XSSFCell cell = xssfRow.getCell(j);
                    if (cell == null) continue;
                    String value = cell.getStringCellValue();
                    correct.put(value, sheet.headers.getOrDefault(value, null));
                }
                for (Map.Entry<String, Header> entry : sheet.headers.entrySet()) {
                    if (!correct.containsKey(entry.getKey())) {
                        correct.put(entry.getKey(), entry.getValue());
                    }
                }
                sheet.headers = correct;
            }
        }

        private XSSFCell getOrDefaultCell(XSSFSheet sheet, int row, int col) {
            XSSFRow sheetRow = sheet.getRow(row);
            if (sheetRow == null) sheetRow = sheet.createRow(row);
            XSSFCell cell = sheetRow.getCell(col);
            if (cell == null) cell = sheetRow.createCell(col);
            return cell;
        }
    }

    public static class Sheet {
        LinkedHashMap<String, Header> headers = new LinkedHashMap<String, Header>() {{
            put(ID_IDENTIFIER, new Header("索引字段", "id", "id", null));
        }};
        private String name;
        int index = 4;

        public Sheet(String main) {
            name = main;
        }


    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class Header {
        String comment;
        String name;
        String type;
        String sheet;
        int index;
        List<Condition> condition = new ArrayList<>();

        public Header(String comment, String name, String type, String sheet) {
            this.comment = comment;
            this.name = name;
            this.type = type;
            this.sheet = sheet;
        }

        @Data
        @AllArgsConstructor
        public static class Condition {
            String name;
            String formatting;
        }
    }

    private static String join(String separate, String... args) {
        String res = "";
        for (String arg : args) {
            if (res.endsWith(separate)) {
                res += arg;
            } else if (StringUtil.isEmpty(res)) {
                res += arg;
            } else {
                res += separate;
                res += arg;
            }
        }
        return res;
    }

    private static boolean isBasicTypes(Class<?> type) {
        if (type == Integer.class || type == String.class || type == Double.class || type == Float.class || type == Boolean.class)
            return true;
        return false;
    }

    /**
     * 枚举类
     */
    protected static class Constrat {
        String parse;
        int col;
        String[] values;

        public Constrat(String[] array) {
            values = array;
        }
    }
}
