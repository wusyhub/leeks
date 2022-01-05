import com.intellij.ide.util.PropertiesComponent;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author wusy
 * Company: 福建亿鑫海信息科技有限公司
 * Createtime : 2022/1/5 下午2:32
 * Description :
 * 注意：本内容仅限于福建亿鑫海信息科技有限公司内部传阅，禁止外泄以及用于其他的商业目的
 */
public abstract class AbstractWindow {

    /**
     * 获取配置
     *
     * @param key
     * @param split
     * @return
     */
    public static List<String> getConfigList(String key, String split) {
        String value = PropertiesComponent.getInstance().getValue(key);
        if (StringUtils.isEmpty(value)) {
            return new ArrayList<>();
        }
        Set<String> set = new LinkedHashSet<>();
        String[] codes = value.split(split);
        for (String code : codes) {
            if (!code.isEmpty()) {
                set.add(code.trim());
            }
        }
        return new ArrayList<>(set);
    }

    /**
     * 获取配置
     *
     * @param key
     * @return
     */
    public static List<String> getConfigList(String key) {
        String value = PropertiesComponent.getInstance().getValue(key);
        if (StringUtils.isEmpty(value)) {
            return new ArrayList<>();
        }
        Set<String> set = new LinkedHashSet<>();
        String[] codes;
        //包含分号
        if (value.contains(";")) {
            codes = value.split("[;]");
        } else {
            codes = value.split("[,，]");
        }
        for (String code : codes) {
            if (!code.isEmpty()) {
                set.add(code.trim());
            }
        }
        return new ArrayList<>(set);
    }

    /**
     * 数据置顶
     *
     * @param topCode
     * @param key
     * @return
     */
    public static List<String> getTopDataList(String topCode, String key) {
        List<String> list = getConfigList(key);
        //添加制定后数据
        list.add(0, topCode);
        //置顶后的数据去重后转String
        list = list.stream().distinct().collect(Collectors.toList());
        //置顶后的数据放到配置上
        PropertiesComponent.getInstance().setValue(key, String.join(";", list));
        return list;
    }

    /**
     * 删除自选
     *
     * @param deleteCode
     * @param key
     * @return
     */
    public static List<String> deleteData(String deleteCode, String key) {
        List<String> list = getConfigList(key);
        //删除后顺序转换为字符串
        list = list.stream().filter(code -> !code.isEmpty() && !code.contains(deleteCode)).collect(Collectors.toList());
        //删除后的数据放到配置上
        PropertiesComponent.getInstance().setValue(key, String.join(";", list));
        return list;
    }
}
