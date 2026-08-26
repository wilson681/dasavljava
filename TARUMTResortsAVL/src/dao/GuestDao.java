package dao;

import adt.HashTableInterface;
import entity.Guest;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import utility.DataFileLocator;

/**
 * GuestDao.java - reads data/guests.txt, builds Guest objects, and adds
 * them to guestTable.
 *
 * Notes:
 * - Only reads the file, builds objects, and stores them in the container -
 *   no business logic here.
 * - txt format: confirmationNumber,name,phone,memberId,tier,
 *   registrationTime,checkInDate,checkOutDate,numberOfNights (same order as
 *   the full Guest constructor); an empty memberId means a non-member.
 * - Must run before GuestBookingDao/BillingRecordDao, since both rely on
 *   guestTable.getEntry() to find the Guest objects added here before they
 *   can attach a Booking/BillingRecord to them.
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
