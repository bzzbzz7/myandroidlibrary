package com.zz.android.widgets;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 把HTML的Table转换成Android的Table
 * <p>
 * <pre>
 * <b>注意：</b>目前只支持绝对值的转换
 * 目前支持的属性：
 *  <ol>
 *   	<li>table</li>
 *  	<li>tr</li>
 *  	<li>td</li>
 *  	<li>width</li>
 *  	<li>height</li>
 *  	<li>rowspan</li>
 *  	<li>colspan</li>
 *  	<li>cellspacing</li>
 *  	<li>ctype(Component Type)</li>
 *  	<li>dindex</li>
 *  </ol>
 * </pre>
 *
 * @author skg
 */
public class TableTemplateLayout extends GridLayout {
    private static final int TABLE_CELL_TAG_KEY = TableTemplateLayout.class.hashCode();
    private static final String XML_TAG_TABLE = "table";
    private static final String XML_TAG_ROW = "tr";
    private static final String XML_TAG_COLUMN = "td";
    private static final String XML_WIDTH = "width";
    private static final String XML_HEIGHT = "height";
    private static final String XML_ROW_SPAN = "rowspan";
    private static final String XML_COLUMN_SPAN = "colspan";
    private static final String XML_CELLSPACING = "cellspacing";
    //
    private static final String XML_COMPONENT_TYPE = "ctype";
    /**
     * Data Index
     */
    private static final String XML_DATA_INDEX = "dindex";
    private TableTemplate mTableTemplate;
    private Float mDimensScale;
    private boolean mAutoScale;

    public TableTemplateLayout(Context context) {
        super(context);
        setAlignmentMode(ALIGN_BOUNDS);
    }

    public TableTemplateLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAlignmentMode(ALIGN_BOUNDS);
    }

    public static TableTemplate parseTableTemplate(InputStream is) {
        XmlPullParser parser = Xml.newPullParser();
        TableTemplate result = null;
        try {
            parser.setInput(is, null);
            int cellFlowIndex = 0;
            int curRowIndex = -1;
            int curColumnIndex = -1;
            int maxColumnCount = 0;
            TableCell cell;
            while (parser.getEventType() != XmlResourceParser.END_DOCUMENT) {
                String tagName = parser.getName();
                switch (parser.getEventType()) {
                    case XmlResourceParser.START_TAG:
                        if (XML_TAG_TABLE.equalsIgnoreCase(tagName)) {
                            // table
                            result = new TableTemplate();
                            result.designWidth = parseSize(parser, XML_WIDTH, 0);
                            result.designHeight = parseSize(parser, XML_HEIGHT, 0);
                            result.cellspacing = parseSize(parser, XML_CELLSPACING, 0);
                        } else if (XML_TAG_ROW.equalsIgnoreCase(tagName)) {
                            // tr
                            curRowIndex++;
                            curColumnIndex = -1;// reset column on new row
                        } else if (XML_TAG_COLUMN.equalsIgnoreCase(tagName)) {
                            // td
                            cell = new TableCell();
                            cell.rowSpan = parseInt(parser, XML_ROW_SPAN, 1);
                            cell.columnSpan = parseInt(parser, XML_COLUMN_SPAN, 1);
                            curColumnIndex += Math.max(1, cell.columnSpan);
                            //
                            cell.designWidth = parseSize(parser, XML_WIDTH, 0);
                            cell.designHeight = parseSize(parser, XML_HEIGHT, 0);
                            cell.componentId = parser.getAttributeValue(null, XML_COMPONENT_TYPE);
                            cell.flowIndex = cellFlowIndex;
                            cell.rowIndex = curRowIndex;
                            cell.columnIndex = curColumnIndex;// FIXME 当有合并行的时候，计算有误
                            cell.dataIndex = parseInt(parser, XML_DATA_INDEX, -1);
                            cellFlowIndex++;
                            result.cells.add(cell);
                        }
                        break;
                    case XmlResourceParser.END_TAG:
                        if (XML_TAG_ROW.equalsIgnoreCase(tagName)) {
                            maxColumnCount = Math.max(maxColumnCount, curColumnIndex + 1);
                        }
                        break;
                }
                parser.next();
            }
            result.rowCount = curRowIndex + 1;
            result.columnCount = maxColumnCount;
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

    private static int parseInt(XmlPullParser parser, String attrName, int def) {
        try {
            return Integer.parseInt(parser.getAttributeValue(null, attrName));
        } catch (Exception e) {
            return def;
        }
    }

    private static int parseSize(XmlPullParser parser, String attrName, int def) {
        try {
            String value = parser.getAttributeValue(null, attrName);
            return Integer.parseInt(value.replace("px", ""));
        } catch (Exception e) {
            return def;
        }
    }

    public void setTableData(TableTemplate template, TableCellFactory factory) {
        setTableData(template, factory, null);
    }

    public void setTableData(TableTemplate template, TableCellFactory factory, Float dimensScale) {
        mTableTemplate = template;
        mDimensScale = dimensScale;
        mAutoScale = dimensScale == null;
        //
        setRowCount(template.getRowCount());
        setColumnCount(template.getColumnCount());
        // 1.clear all child
        removeAllViews();
        // 2.inner add new child
        List<TableCell> cells = template.cells;
        if (cells == null || cells.isEmpty()) {
            return;
        }
        for (TableCell cell : cells) {
            View child = factory.createCellView(this, cell);
            child.setTag(TABLE_CELL_TAG_KEY, cell);
            ViewGroup.LayoutParams lp = child.getLayoutParams();
            LayoutParams glp = null;
            if (lp == null) {
                glp = new LayoutParams();
            } else if (lp instanceof LayoutParams) {
                glp = (LayoutParams) lp;
            } else {
                // glp = new GridLayout.LayoutParams(lp);
                glp = new LayoutParams();
            }
            glp.rowSpec = spec(UNDEFINED, Math.max(1, cell.rowSpan)/* , 1 */);
            glp.columnSpec = spec(UNDEFINED, Math.max(1, cell.columnSpan)/* , 1 */);
            addViewInLayout(child, -1, glp, true);
        }
        // 3.invalidate
        forceLayout();
        invalidate();
        // 4.before measure make child layout size
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        // 1.get max size
        float scale;
        if (mAutoScale) {
            if (MeasureSpec.getMode(widthSpec) != MeasureSpec.EXACTLY
                    || MeasureSpec.getMode(heightSpec) != MeasureSpec.EXACTLY) {
                throw new IllegalArgumentException(
                        "if you not setup DimensScale,you must use exactly size description for"
                                + getClass().getCanonicalName() + ",for example MATCH_PARENT or 100dp");
            }
            final int maxWidth = MeasureSpec.getSize(widthSpec);
            final int maxHeight = MeasureSpec.getSize(heightSpec);
            final int designWidth = mTableTemplate.designWidth;
            final int designHeight = mTableTemplate.designHeight;
            scale = Math.min((float) maxWidth / designWidth, (float) maxHeight / designHeight);
        } else {
            scale = mDimensScale;
        }
        //
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            // make child layout size
            View child = getChildAt(i);
            LayoutParams glp = (LayoutParams) child.getLayoutParams();
            TableCell cell = (TableCell) child.getTag(TABLE_CELL_TAG_KEY);
            //
            glp.width = (int) (cell.getDesignWidth() * scale);
            glp.height = (int) (cell.getDesignHeight() * scale);
            //
            if (cell.rowIndex > 0) {
                glp.topMargin = (int) (mTableTemplate.cellspacing * scale);
            }
            if (cell.columnIndex > 0) {
                glp.leftMargin = (int) (mTableTemplate.cellspacing * scale);
            }
        }
        super.onMeasure(widthSpec, heightSpec);
    }
    //
    // public static final int TEMPLATE_MATCH_PARENT = -1;
    // public static final int TEMPLATE_WRAP_CONTENT = -2;

    public interface TableCellFactory {
        public View createCellView(TableTemplateLayout parent, TableCell cell);
    }

    public static class TableCell {
        @Deprecated
        int rowIndex;
        @Deprecated
        int columnIndex;
        private int rowSpan;
        private int columnSpan;
        private int designWidth;
        private int designHeight;
        private String componentId;
        private int flowIndex;
        private int dataIndex;

        public int getDesignWidth() {
            return designWidth;
        }

        public int getDesignHeight() {
            return designHeight;
        }

        public String getComponentId() {
            return componentId;
        }

        public int getFlowIndex() {
            return flowIndex;
        }

        public int getDataIndex() {
            return dataIndex;
        }

        @Override
        public String toString() {
            return "TableCell [rowSpan=" + rowSpan + ", columnSpan=" + columnSpan + ", designWidth=" + designWidth
                    + ", designHeight=" + designHeight + ", componentId=" + componentId + ", flowIndex=" + flowIndex
                    + ", dataIndex=" + dataIndex + ", rowIndex=" + rowIndex + ", columnIndex=" + columnIndex + "]";
        }

    }

    public static class TableTemplate {
        private final List<TableCell> cells = new ArrayList<TableCell>();
        private int designWidth;
        private int designHeight;
        private int rowCount;
        private int columnCount;
        private int cellspacing;

        public int getDesignWidth() {
            return designWidth;
        }

        public int getDesignHeight() {
            return designHeight;
        }

        public List<TableCell> getCells() {
            return cells;
        }

        public int getRowCount() {
            return rowCount;
        }

        public int getColumnCount() {
            return columnCount;
        }

        public int getCellspacing() {
            return cellspacing;
        }
    }
}
