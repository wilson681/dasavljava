package dao;

import adt.HashTableInterface;
import entity.Guest;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import utility.DataFileLocator;

/**
 * GuestDao.java - 负责把 data/guests.txt 读进来,组成 Guest 物件,加进 guestTable。
 *
 * @author 某某
 *
 * 说明:
 * - 只做"读文件、组物件、塞进容器"这件事,不做任何业务判断
 * - txt格式: confirmationNumber,name,phone,memberId,tier,registrationTime,checkInDate,checkOutDate,numberOfNights
 *   (跟 Guest 完整构造函数参数顺序一样);memberId 空白代表非会员
 * - 必须在 GuestBookingDao/BillingRecordDao 之前跑,那两个都要靠 guestTable.getEntry()
 *   找到这里已经塞进去的 Guest 物件才能往上加 Booking/BillingRecord
 */
public class GuestDao {

    private static final String FILE_PATH = "data/guests.txt";

    public void loadGuests(HashTableInterface<Guest> guestTable) {
        File file = DataFileLocator.locate(GuestDao.class, FILE_PATH);
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
                String name = fields[1].trim();
                String phone = fields[2].trim();
                String memberId = fields[3].trim();
                String tier = fields[4].trim();
                String registrationTime = fields[5].trim();
                String checkInDate = fields[6].trim();
                String checkOutDate = fields[7].trim();
                int numberOfNights = Integer.parseInt(fields[8].trim());

                guestTable.add(new Guest(confirmationNumber, name, phone,
                        memberId.isEmpty() ? null : memberId, tier, registrationTime,
                        checkInDate, checkOutDate, numberOfNights));
            }
        } catch (IOException e) {
            System.out.println("Failed to load " + FILE_PATH + ": " + e.getMessage());
        }
    }
}
