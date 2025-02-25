import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.table.JBTable;
import handler.SinaStockHandler;
import handler.StockRefreshHandler;
import handler.TencentStockHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.DragDropRowTableUI;
import utils.LogUtil;
import utils.PopupsUiUtil;
import utils.WindowUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;

public class StockWindow extends AbstractWindow {

    private JPanel mPanel;

    static StockRefreshHandler handler;

    static JBTable table;

    static JLabel refreshTimeLabel;

    public JPanel getmPanel() {
        return mPanel;
    }

    public static StockRefreshHandler getHandler() {
        return handler;
    }

    static {
        refreshTimeLabel = new JLabel();
        refreshTimeLabel.setToolTipText("最后刷新时间");
        refreshTimeLabel.setBorder(new EmptyBorder(0, 0, 0, 5));
        table = new JBTable();
        table.setUI(new DragDropRowTableUI());
        //记录列名的变化
        table.getTableHeader().addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                StringBuilder tableHeadChange = new StringBuilder();
                for (int i = 0; i < table.getColumnCount(); i++) {
                    tableHeadChange.append(table.getColumnName(i)).append(",");
                }
                PropertiesComponent instance = PropertiesComponent.getInstance();
                //将列名的修改放入环境中 key:stock_table_header_key
                instance.setValue(WindowUtils.STOCK_TABLE_HEADER_KEY, tableHeadChange
                        .substring(0, tableHeadChange.length() > 0 ? tableHeadChange.length() - 1 : 0));
            }

        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (table.getSelectedRow() < 0) {
                    return;
                }
                String code = String.valueOf(table.getModel().getValueAt(table.convertRowIndexToModel(table.getSelectedRow()), handler.codeColumnIndex));//FIX 移动列导致的BUG
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() > 1) {
                    // 鼠标左键双击
                    try {
                        PopupsUiUtil.showImageByStockCode(code, PopupsUiUtil.StockShowType.min, new Point(e.getXOnScreen(), e.getYOnScreen()));
                    } catch (MalformedURLException ex) {
                        ex.printStackTrace();
                        LogUtil.info(ex.getMessage());
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    //鼠标右键
                    PopupsUiUtil.StockShowType[] values = Arrays.stream(PopupsUiUtil.StockShowType.values()).filter(type -> {
                        if (PopupsUiUtil.StockShowType.add.equals(type)) {
                            return false;
                        }
                        return true;
                    }).toArray(PopupsUiUtil.StockShowType[]::new);
                    JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<PopupsUiUtil.StockShowType>("", values) {
                        @Override
                        public @NotNull String getTextFor(PopupsUiUtil.StockShowType value) {
                            return value.getDesc();
                        }

                        @Override
                        public @Nullable PopupStep onChosen(PopupsUiUtil.StockShowType selectedValue, boolean finalChoice) {
                            //判断是右键是否是置顶
                            if (selectedValue.getType().equals(PopupsUiUtil.StockShowType.top.getType())) {
                                //将数据置顶
                                if (handler != null) {
                                    boolean colorful = PropertiesComponent.getInstance().getBoolean("key_colorful");
                                    handler.stopHandle();
                                    handler.handle(getTopDataList(code, "key_stocks"), true);
                                    handler.refreshColorful(colorful);
                                }
                                //应用数据
                                apply();
                                return PopupStep.FINAL_CHOICE;
                            }
                            //判断是右键是否是删除自选
                            if (selectedValue.getType().equals(PopupsUiUtil.StockShowType.delete.getType())) {
                                //删除自选
                                if (handler != null) {
                                    boolean colorful = PropertiesComponent.getInstance().getBoolean("key_colorful");
                                    handler.refreshColorful(colorful);
                                    handler.stopHandle();
                                    handler.handle(deleteData(code, "key_stocks"), true);
                                }
                                //应用数据
                                apply();
                                return PopupStep.FINAL_CHOICE;
                            }
                            try {
                                PopupsUiUtil.showImageByStockCode(code, selectedValue, new Point(e.getXOnScreen(), e.getYOnScreen()));
                            } catch (MalformedURLException ex) {
                                ex.printStackTrace();
                                LogUtil.info(ex.getMessage());
                            }
                            return super.onChosen(selectedValue, finalChoice);
                        }
                    }).show(RelativePoint.fromScreen(new Point(e.getXOnScreen(), e.getYOnScreen())));
                }
            }
        });
    }

    public StockWindow() {

        //切换接口
        handler = factoryHandler();

        AnActionButton refreshAction = new AnActionButton("停止刷新当前表格数据", AllIcons.Actions.Pause) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                handler.stopHandle();
                this.setEnabled(false);
            }
        };
        ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(table)
                .addExtraAction(new AnActionButton("持续刷新当前表格数据", AllIcons.Actions.Refresh) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        refresh();
                        refreshAction.setEnabled(true);
                    }
                })
                .addExtraAction(refreshAction)
                .setToolbarPosition(ActionToolbarPosition.TOP);
        JPanel toolPanel = toolbarDecorator.createPanel();
        toolbarDecorator.getActionsPanel().add(refreshTimeLabel, BorderLayout.EAST);
        toolPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        mPanel.add(toolPanel, BorderLayout.CENTER);
        // 非主要tab，需要创建，创建时立即应用数据
        apply();
    }

    private static StockRefreshHandler factoryHandler() {
        boolean useSinaApi = PropertiesComponent.getInstance().getBoolean("key_stocks_sina");
        if (useSinaApi) {
            if (handler instanceof SinaStockHandler) {
                return handler;
            }
            if (handler != null) {
                handler.stopHandle();
            }
            return new SinaStockHandler(table, refreshTimeLabel);
        }
        if (handler instanceof TencentStockHandler) {
            return handler;
        }
        if (handler != null) {
            handler.stopHandle();
        }
        return new TencentStockHandler(table, refreshTimeLabel);
    }

    public static void apply() {
        if (handler != null) {
            handler = factoryHandler();
            PropertiesComponent instance = PropertiesComponent.getInstance();
            handler.setStriped(instance.getBoolean("key_table_striped"));
            handler.setThreadSleepTime(instance.getInt("key_stocks_thread_time", handler.getThreadSleepTime()));
            handler.refreshColorful(instance.getBoolean("key_colorful"));
            handler.clearRow();
            handler.setupTable(loadStocks());
            handler.handle(loadStocks(), true);
        }
    }

    public static void refresh() {
        if (handler != null) {
            boolean colorful = PropertiesComponent.getInstance().getBoolean("key_colorful");
            handler.refreshColorful(colorful);
            handler.handle(loadStocks(), true);
        }
    }

    private static List<String> loadStocks() {
        return getConfigList("key_stocks");
    }

}
