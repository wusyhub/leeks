package utils;

import com.intellij.ide.util.PropertiesComponent;
import org.apache.commons.lang.StringUtils;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.TableModel;

public class DragDropRowTableUI extends BasicTableUI {

    private boolean draggingRow = false;

    private int startDragPoint;

    private int dyOffset;

    @Override
    protected MouseInputListener createMouseInputListener() {
        return new DragDropRowMouseInputHandler();
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);
        if (draggingRow) {
            g.setColor(table.getParent().getBackground());
            Rectangle cellRect = table.getCellRect(table.getSelectedRow(), 0, false);
            g.copyArea(cellRect.x, cellRect.y, table.getWidth(), table.getRowHeight(), cellRect.x, dyOffset);
            if (dyOffset < 0) {
                g.fillRect(cellRect.x, cellRect.y + (table.getRowHeight() + dyOffset), table.getWidth(), (dyOffset * -1));
            } else {
                g.fillRect(cellRect.x, cellRect.y, table.getWidth(), dyOffset);
            }
        }
    }

    class DragDropRowMouseInputHandler extends MouseInputHandler {

        @Override
        public void mousePressed(MouseEvent e) {
            super.mousePressed(e);
            startDragPoint = (int) e.getPoint().getY();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            int fromRow = table.getSelectedRow();
            if (fromRow >= 0) {
                draggingRow = true;
                int rowHeight = table.getRowHeight();
                int middleOfSelectedRow = (rowHeight * fromRow) + (rowHeight / 2);
                int toRow = -1;
                int yMousePoint = (int) e.getPoint().getY();
                if (yMousePoint < (middleOfSelectedRow - rowHeight)) {
                    // Move row up
                    toRow = fromRow - 1;
                } else if (yMousePoint > (middleOfSelectedRow + rowHeight)) {
                    // Move row down
                    toRow = fromRow + 1;
                }

                if (toRow >= 0 && toRow < table.getRowCount()) {
                    TableModel model = table.getModel();

                    for (int i = 0; i < model.getColumnCount(); i++) {
                        String columnName = model.getColumnName(i);
                        Object fromValue = model.getValueAt(fromRow, i);
                        Object toValue = model.getValueAt(toRow, i);
                        if ("编码".equals(columnName) || "bian_ma".equals(columnName)){
                            moveCode(fromValue.toString(),toValue.toString(),"key_stocks");
                        }

                        model.setValueAt(toValue, fromRow, i);
                        model.setValueAt(fromValue, toRow, i);
                    }
                    table.setRowSelectionInterval(toRow, toRow);
                    startDragPoint = yMousePoint;
                }
                dyOffset = (startDragPoint - yMousePoint) * -1;
                table.repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            super.mouseReleased(e);
            draggingRow = false;
            table.repaint();
        }

        public void moveCode(String moveCode,String destCode,String key){
            String value = PropertiesComponent.getInstance().getValue(key);
            if (StringUtils.isEmpty(value)) {
                return;
            }

            Set<String> set = new LinkedHashSet<>();
            String[] codes = null;
            if (value.contains(";")) {//包含分号
                codes = value.split("[;]");
            } else {
                codes = value.split("[,，]");
            }

            for (String code : codes) {
                if (!code.isEmpty()) {
                    if (destCode.trim().equals(code.trim())) {
                        set.add(moveCode.trim());
                    }
                    if (!moveCode.trim().equals(code.trim())){
                        set.add(code.trim());
                    }

                }
            }

            //移动后顺序转换为字符串
            String newConfig = String.join(",", set);
            //移动后的数据放到配置上
            PropertiesComponent.getInstance().setValue(key,newConfig);
        }

    }

}



