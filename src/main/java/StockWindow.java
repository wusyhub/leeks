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
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import handler.SinaStockHandler;
import handler.StockRefreshHandler;
import handler.TencentStockHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.HttpClientPool;
import utils.LogUtil;
import utils.PopupsUiUtil;
import utils.WindowUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StockWindow {
    private JPanel mPanel;

    static StockRefreshHandler handler;

    static JBTable table;
    static JLabel refreshTimeLabel;

    public JPanel getmPanel() {
        return mPanel;
    }

    static {
        refreshTimeLabel = new JLabel();
        refreshTimeLabel.setToolTipText("最后刷新时间");
        refreshTimeLabel.setBorder(new EmptyBorder(0, 0, 0, 5));
        table = new JBTable();
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
                    JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<PopupsUiUtil.StockShowType>("",
                            PopupsUiUtil.StockShowType.values()) {
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
                                    handler.handle(FundWindow.getTopDataList(code, "key_stocks"));
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
                                    handler.handle(FundWindow.deleteData(code, "key_stocks"));
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
                .addExtraAction(new AnActionButton("搜索", AllIcons.Actions.Search) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        // 获取 JBPopupFactory
                        JBPopupFactory instance = JBPopupFactory.getInstance();
                        JPanel panel = new JPanel();
                        //设置面板大小
                        panel.setSize(600, 20);
                        JTextField searchTextField = new JTextField();
                        panel.add(searchTextField);
                        Object[] columns = {"字段一", "字段二", "字段三", "字段四"};//字段
                        Object[][] data = null;//需要展示的数据，一般是二维数组
                        DefaultTableModel model = new DefaultTableModel(data, columns);
                        JBTable table = new JBTable();
                        table.setModel(model);
                        panel.add(table);
                        searchTextField.addKeyListener(new KeyAdapter() {
                            @Override
                            public void keyReleased(KeyEvent e) {
                                handler.clearRow();
                                String value = searchTextField.getText().trim();
                                List<String> list = searchData(value);
                                if (handler != null) {
                                    handler.refreshColorful(true);
                                    handler.handle(list);
                                }
                            }
                        });

                        instance.createComponentPopupBuilder(panel, new JBLabel())
                                .setTitle("搜索")
                                .setCancelOnClickOutside(true)
                                .setMovable(true)
                                //设置可拖动大小
                                .setResizable(true)
                                .setNormalWindowLevel(false)
                                .setMinSize(new Dimension(600, 300))
                                .createPopup()
                                .showInFocusCenter();
                    }
                })
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

    /**
     * 搜索数据信息
     *
     * @param value 搜索值
     */
    private List<String> searchData(String value) {
        List<String> list = new ArrayList<>();
        try {
            String searchUrl = "http://smartbox.gtimg.cn/s3/?t=all&q=" + value;
            String result = HttpClientPool.getHttpClient().get(searchUrl);
            result = result.substring(8, result.length() - 1);
            String[] strings = result.split("\\^");
            for (String string : strings) {
                String[] data = string.split("~");
                if (Objects.equals(data[0], "sh") || Objects.equals(data[0], "sz")) {
                    String code = data[0] + data[1];
                    list.add(code);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
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
            handler.handle(loadStocks());
        }
    }

    public static void refresh() {
        if (handler != null) {
            boolean colorful = PropertiesComponent.getInstance().getBoolean("key_colorful");
            handler.refreshColorful(colorful);
            handler.handle(loadStocks());
        }
    }

    private static List<String> loadStocks() {
        return FundWindow.getConfigList("key_stocks");
    }

}
