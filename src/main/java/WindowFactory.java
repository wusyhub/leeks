import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import utils.HttpClientPool;
import utils.LogUtil;

public class WindowFactory implements ToolWindowFactory {
    /**
     * 基金窗口
     */
    private FundWindow fundWindow = new FundWindow();
    /**
     * 股票窗口
     */
    private StockWindow stockWindow = new StockWindow();
    /**
     * 查找窗口
     */
    private FindWindow findWindow = new FindWindow();
    /**
     * 虚拟货币窗口
     */
    private CoinWindow coinWindow = new CoinWindow();

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        //先加载代理
        loadProxySetting();
        ContentManager contentManager = toolWindow.getContentManager();
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        //股票
        Content content_stock = contentFactory.createContent(stockWindow.getmPanel(), "Stock", false);
        contentManager.addContent(content_stock);
        //查找窗口
        Content content_find = contentFactory.createContent(findWindow.getmPanel(), "Find", false);
        contentManager.addContent(content_find);
        //基金
        Content content_fund = contentFactory.createContent(fundWindow.getmPanel(), "Fund", false);
        contentManager.addContent(content_fund);
        //虚拟货币
        Content content_coin = contentFactory.createContent(coinWindow.getmPanel(), "Coin", false);
        contentManager.addContent(content_coin);
        if (StringUtils.isEmpty(PropertiesComponent.getInstance().getValue("key_funds"))) {
            // 没有配置基金数据，选择展示股票
            contentManager.setSelectedContent(content_stock);
        }
        LogUtil.setProject(project);
    }

    /**
     * 加载代理配置
     */
    private void loadProxySetting() {
        String proxyStr = PropertiesComponent.getInstance().getValue("key_proxy");
        HttpClientPool.getHttpClient().buildHttpClient(proxyStr);
    }

}
