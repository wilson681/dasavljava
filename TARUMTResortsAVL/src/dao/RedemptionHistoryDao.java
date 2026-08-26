package dao;

import adt.ListInterface;
import entity.RedemptionTransaction;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import utility.DataFileLocator;

/**
 * RedemptionHistoryDao.java - reads data/redemption_history.txt, builds
 * RedemptionTransaction objects, and adds them to redemptionHistory.
 *
 * Notes:
 * - Only reads the file, builds objects, and stores them in the container -
 *   no business logic here.
 * - txt format: memberId,itemRedeemed,pointsUsed,date (same order as the
 *   constructor).
 */
public class RedemptionHistoryDao {

    private static final String FILE_PATH = "data/redemption_history.txt";

    public void loadRedemptionHistory(ListInterface<RedemptionTransaction> redemptionHistory) {
        File file = DataFileLocator.locate(RedemptionHistoryDao.class, FILE_PATH);
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
                String memberId = fields[0].trim();
                String itemRedeemed = fields[1].trim();
                int pointsUsed = Integer.parseInt(fields[2].trim());
                String date = fields[3].trim();
                redemptionHistory.add(new RedemptionTransaction(memberId, itemRedeemed, pointsUsed, date));
            }
        } catch (IOException e) {
            System.out.println("Failed to load " + FILE_PATH + ": " + e.getMessage());
        }
    }
}
