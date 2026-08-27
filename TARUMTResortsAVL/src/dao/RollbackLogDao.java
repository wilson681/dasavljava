package dao;

import adt.ListInterface;
import entity.RollbackLogEntry;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import utility.DataFileLocator;

/**
 * RollbackLogDao.java - reads data/rollback_log.txt, builds
 * RollbackLogEntry objects, and adds them to rollbackLog.
 *
 * Notes:
 * - Only reads the file, builds objects, and stores them in the container -
 *   no business logic here.
 * - txt format: roomNumber,fromStatus,toStatus,date (same order as the
 *   constructor).
 *
 * @author All
 */
public class RollbackLogDao {

    private static final String FILE_PATH = "data/rollback_log.txt";

    public void loadRollbackLog(ListInterface<RollbackLogEntry> rollbackLog) {
        File file = DataFileLocator.locate(RollbackLogDao.class, FILE_PATH);
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
                String roomNumber = fields[0].trim();
                String fromStatus = fields[1].trim();
                String toStatus = fields[2].trim();
                String date = fields[3].trim();
                rollbackLog.add(new RollbackLogEntry(roomNumber, fromStatus, toStatus, date));
            }
        } catch (IOException e) {
            System.out.println("Failed to load " + FILE_PATH + ": " + e.getMessage());
        }
    }
}
