package dao;

import adt.ListInterface;
import entity.RedemptionItem;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import utility.DataFileLocator;

/**
 * RedemptionItemDao.java - reads data/redemption_items.txt, builds
 * RedemptionItem objects, and adds them to the given redemptionItemList.
 *
 * Notes:
 * - Only reads the file, builds objects, and stores them in the container -
 *   no business logic here (not part of Entity/Boundary/Control).
 * - txt format: itemName,pointsRequired (same order as the RedemptionItem
 *   constructor).
 *
 * @author All
 */
public class RedemptionItemDao {

    private static final String FILE_PATH = "data/redemption_items.txt";

    /**
     * Reads redemption_items.txt, turns each line into a RedemptionItem,
     * and adds it to redemptionItemList.
     * @param redemptionItemList the container to add the loaded
     * RedemptionItem objects into
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
