package dao;

import adt.ListInterface;
import entity.RedemptionItem;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import utility.DataFileLocator;

/**
 * RedemptionItemDao.java - 负责把 data/redemption_items.txt 读进来,组成
 * RedemptionItem 物件,加进传进来的 redemptionItemList。
 *
 * @author 某某
 *
 * 说明:
 * - 只做"读文件、组物件、塞进容器"这件事,不做任何业务判断(不属于Entity/Boundary/Control)
 * - txt格式: itemName,pointsRequired(跟 RedemptionItem 构造函数的参数顺序一样)
 */
public class RedemptionItemDao {

    private static final String FILE_PATH = "data/redemption_items.txt";

    /**
     * 读取 redemption_items.txt,把每一行组成一个 RedemptionItem,加进 redemptionItemList。
     * @param redemptionItemList 要把读到的RedemptionItem塞进去的容器
     */
    public void loadRedemptionItems(ListInterface<RedemptionItem> redemptionItemList) {
        File file = DataFileLocator.locate(RedemptionItemDao.class, FILE_PATH);
        if (file == null) {
            System.out.println("Failed to load " + FILE_PATH + ": file not found near project root");
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] fields = line.split(",");
                String itemName = fields[0].trim();
                int pointsRequired = Integer.parseInt(fields[1].trim());
                redemptionItemList.add(new RedemptionItem(itemName, pointsRequired));
            }
        } catch (IOException e) {
            System.out.println("Failed to load " + FILE_PATH + ": " + e.getMessage());
        }
    }
}
