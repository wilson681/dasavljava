package dao;

import adt.HashTableInterface;
import adt.ListInterface;
import entity.Booking;
import entity.BookingStatus;
import entity.Guest;
import entity.Member;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import utility.DataFileLocator;
import utility.TierRankUtility;

/**
 * GuestBookingDao.java - reads data/guest_bookings.txt and builds Bookings
 * that already have a room assigned, attaching each to its Guest in
 * guestTable (not into a Queue/AVL Tree - those are still waiting).
 *
 * Notes:
 * - Only reads the file, builds objects, and stores them in the container -
 *   no business logic here.
 * - Must run after GuestDao, relying on
 *   guestTable.getEntry(new Guest(confirmationNumber)) to find the existing
 *   Guest object for that confirmation number.
 * - txt format: confirmationNumber,requestedRoomType,source,memberId,
 *   roomNumber,status,checkInDate,checkOutDate,numberOfNights,registeredAt,
 *   allocatedAt.
 * - After a successful room assignment, must also update
 *   Guest.bookedRooms(addRoom) and Guest.bookings(addBooking), mirroring
 *   the two things the real allocation flow (doAllocate()) does on success.
 * - bookingId uses its own counter with prefix SGB, so it won't collide
 *   with manual operations' WB/VB IDs.
 */
public class GuestBookingDao {

    private static final String FILE_PATH = "data/guest_bookings.txt";

    private int bookingCounter = 0;

    public void loadGuestBookings(HashTableInterface<Guest> guestTable, ListInterface<Member> memberList) {
        File file = DataFileLocator.locate(GuestBookingDao.class, FILE_PATH);
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
                String requestedRoomType = fields[1].trim();
                String source = fields[2].trim();
                String memberId = fields[3].trim();
                String roomNumber = fields[4].trim();
                String status = fields[5].trim();
                String checkInDate = fields[6].trim();
                String checkOutDate = fields[7].trim();
                int numberOfNights = Integer.parseInt(fields[8].trim());
                String registeredAt = fields[9].trim();
                String allocatedAt = fields[10].trim();

                Guest guest = guestTable.getEntry(new Guest(confirmationNumber));
                if (guest == null) {
                    System.out.println("Skipped " + FILE_PATH + " row: no guest found for confirmation number " + confirmationNumber);
                    continue;
                }

                int tierRank = 0;
                if ("VIP".equals(source) && !memberId.isEmpty()) {
                    Member member = findMemberById(memberList, memberId);
                    if (member != null) {
                        tierRank = TierRankUtility.tierToRank(member.getTier());
                    }
                }

                bookingCounter++;
                String bookingId = "SGB" + String.format("%06d", bookingCounter);

                Booking booking = new Booking(bookingId, confirmationNumber, guest.getName(),
                        guest.getPhone(), memberId.isEmpty() ? null : memberId, requestedRoomType,
                        BookingStatus.valueOf(status), source, bookingCounter, tierRank, registeredAt);
                booking.setStayPeriod(checkInDate, checkOutDate, numberOfNights);
                booking.setAssignedRoomNo(roomNumber);
                booking.setAllocatedAt(allocatedAt);

                guest.addBooking(booking);
                guest.addRoom(roomNumber);
            }
        } catch (IOException e) {
            System.out.println("Failed to load " + FILE_PATH + ": " + e.getMessage());
        }
    }

    private Member findMemberById(ListInterface<Member> memberList, String memberId) {
        Iterator<Member> iterator = memberList.getIterator();
        while (iterator.hasNext()) {
            Member member = iterator.next();
            if (member.getMemberId().equals(memberId)) {
                return member;
            }
        }
        return null;
    }
}
