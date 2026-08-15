package dao;

import adt.HashTableInterface;
import entity.BillingRecord;
import entity.Guest;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import utility.DataFileLocator;

/**
 * BillingRecordDao.java - 负责把 data/billing_records.txt 读进来,组成
 * BillingRecord,挂到 guestTable 里对应的 Guest 身上。
 *
 * @author 某某
 *
 * 说明:
 * - 只做"读文件、组物件、塞进容器"这件事,不做任何业务判断
 * - 必须在 GuestDao 之后跑,靠 guestTable.getEntry(new Guest(confirmationNumber))
 *   找到已存在的 Guest
 * - txt格式: confirmationNumber,roomFee,extraCharges,totalAmount,pointsEarned,date
 *   (少了 billingId——那个由这个 DAO 自己的计数器生成,跟真实退房结账的做法一样)
 * - billingId 前缀 SBR,不会跟真人操作退房产生的 BR 编号撞号
 */
public class BillingRecordDao {

    private static final String FILE_PATH = "data/billing_records.txt";

    private int billingCounter = 0;

    public void loadBillingRecords(HashTableInterface<Guest> guestTable) {
        File file = DataFileLocator.locate(BillingRecordDao.class, FILE_PATH);
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
                String confirmationNumber = fields[0].trim();
                double roomFee = Double.parseDouble(fields[1].trim());
                double extraCharges = Double.parseDouble(fields[2].trim());
                double totalAmount = Double.parseDouble(fields[3].trim());
                int pointsEarned = Integer.parseInt(fields[4].trim());
                String date = fields[5].trim();

                Guest guest = guestTable.getEntry(new Guest(confirmationNumber));
                if (guest == null) {
                    System.out.println("Skipped " + FILE_PATH + " row: no guest found for confirmation number " + confirmationNumber);
                    continue;
                }

                billingCounter++;
                String billingId = "SBR" + String.format("%06d", billingCounter);
                guest.addBillingRecord(new BillingRecord(billingId, confirmationNumber,
                        roomFee, extraCharges, totalAmount, pointsEarned, date));
            }
        } catch (IOException e) {
            System.out.println("Failed to load " + FILE_PATH + ": " + e.getMessage());
        }
    }
}
