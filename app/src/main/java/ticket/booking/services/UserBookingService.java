package ticket.booking.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ticket.booking.entities.Ticket;
import ticket.booking.entities.Train;
import ticket.booking.entities.User;
import ticket.booking.util.UserServiceUtil;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class UserBookingService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private List<User> userList = new ArrayList<>();
    private User user;

    private static final String USER_FILE_PATH = "app/src/main/java/ticket/booking/localDb/users.json";

    // ctor used after login attempt (App option 2)
    public UserBookingService(User user) throws IOException {
        this.user = user;
        loadUserListFromFile();
    }

    // default ctor (App boot)
    public UserBookingService() throws IOException {
        loadUserListFromFile();
    }

    /* ---------- persistence ---------- */

    private void loadUserListFromFile() throws IOException {
        File usersFile = new File(USER_FILE_PATH);
        if (!usersFile.exists()) {
            usersFile.getParentFile().mkdirs();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(usersFile, new ArrayList<User>());
            userList = new ArrayList<>();
            return;
        }
        if (usersFile.length() == 0) {
            userList = new ArrayList<>();
            return;
        }
        userList = objectMapper.readValue(usersFile, new TypeReference<List<User>>() {});
        if (userList == null) userList = new ArrayList<>();
    }

    private void saveUserListToFile() throws IOException {
        File usersFile = new File(USER_FILE_PATH);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(usersFile, userList);
    }

    /* ---------- auth / users ---------- */

    public Boolean loginUser() {
        if (user == null) return Boolean.FALSE;
        Optional<User> foundUser = userList.stream()
                .filter(u -> u.getName().equals(user.getName())
                        && UserServiceUtil.checkPassword(user.getPassword(), u.getHashedPassword()))
                .findFirst();
        return foundUser.isPresent();
    }

    public Boolean signUp(User user1) {
        try {
            // very light duplicate username check (case-sensitive to keep it simple)
            boolean exists = userList.stream().anyMatch(u -> u.getName().equals(user1.getName()));
            if (exists) {
                System.out.println("Username already exists.");
                return Boolean.FALSE;
            }
            userList.add(user1);
            saveUserListToFile();
            return Boolean.TRUE;
        } catch (IOException ex) {
            ex.printStackTrace();
            return Boolean.FALSE;
        }
    }

    public void fetchBookings() {
        if (user == null) {
            System.out.println("Not logged in.");
            return;
        }
        Optional<User> userFetched = userList.stream()
                .filter(u -> u.getName().equals(user.getName())
                        && UserServiceUtil.checkPassword(user.getPassword(), u.getHashedPassword()))
                .findFirst();
        if (userFetched.isPresent()) {
            userFetched.get().printTickets();
        } else {
            System.out.println("No such user or wrong password.");
        }
    }

    /* ---------- trains / search ---------- */

    public List<Train> getTrains(String source, String destination) {
        try {
            TrainService trainService = new TrainService();
            return trainService.searchTrains(source, destination);
        } catch (IOException ex) {
            ex.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<List<Integer>> fetchSeats(Train train) {
        // Always re-load the authoritative train to avoid stale seat maps
        try {
            TrainService trainService = new TrainService();
            Train fresh = trainService.getTrainById(train.getTrainId());
            return fresh != null ? fresh.getSeats() : train.getSeats();
        } catch (IOException e) {
            e.printStackTrace();
            return train.getSeats();
        }
    }

    /* ---------- booking / cancel ---------- */

    public Boolean bookTrainSeat(Train selectedTrain, int row, int seat) {
        try {
            TrainService trainService = new TrainService();

            // Always operate on authoritative train from DB, not the selected copy
            Train train = trainService.getTrainById(selectedTrain.getTrainId());
            if (train == null) {
                System.out.println("Train not found.");
                return Boolean.FALSE;
            }

            List<List<Integer>> seats = train.getSeats();
            // bounds
            if (row < 0 || row >= seats.size() || seat < 0 || seat >= seats.get(row).size()) {
                System.out.println("Invalid seat selection.");
                return Boolean.FALSE;
            }
            // already booked?
            if (seats.get(row).get(seat) == 1) {
                System.out.println("Seat already booked.");
                return Boolean.FALSE;
            }

            // mark booked
            seats.get(row).set(seat, 1);
            train.setSeats(seats);
            // persist train
            trainService.updateTrain(train);

            // find current user
            Optional<User> userFetched = userList.stream()
                    .filter(u -> u.getName().equals(user.getName())
                            && UserServiceUtil.checkPassword(user.getPassword(), u.getHashedPassword()))
                    .findFirst();
            if (userFetched.isEmpty()) {
                System.out.println("User not found.");
                return Boolean.FALSE;
            }
            User currentUser = userFetched.get();

            // date string same as before
            String travelDateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            // build ticket (snapshot with row/col, plus train info)
            Ticket ticket = new Ticket(
                    UUID.randomUUID().toString(),
                    currentUser.getUserId(),
                    train.getStations().get(0),
                    train.getStations().get(train.getStations().size() - 1),
                    travelDateStr,
                    train,
                    row,
                    seat
            );

            currentUser.addTicket(ticket);
            saveUserListToFile();

            System.out.println("Booking successful. Ticket ID: " + ticket.getTicketId());
            return Boolean.TRUE;

        } catch (IOException ex) {
            ex.printStackTrace();
            return Boolean.FALSE;
        }
    }

    public Boolean cancelBooking(String ticketId) {
        if (ticketId == null || ticketId.isEmpty()) {
            System.out.println("Ticket ID cannot be null or empty.");
            return Boolean.FALSE;
        }

        // find logged-in user
        Optional<User> userFetched = userList.stream()
                .filter(u -> u.getName().equals(user.getName())
                        && UserServiceUtil.checkPassword(user.getPassword(), u.getHashedPassword()))
                .findFirst();

        if (userFetched.isEmpty()) {
            System.out.println("User not found.");
            return Boolean.FALSE;
        }
        User currentUser = userFetched.get();

        // find the ticket (do not mutate while iterating)
        Ticket ticketToCancel = currentUser.getTicketsBooked().stream()
                .filter(t -> t.getTicketId().equals(ticketId))
                .findFirst()
                .orElse(null);

        if (ticketToCancel == null) {
            System.out.println("No ticket found with ID " + ticketId);
            return Boolean.FALSE;
        }

        try {
            // Always load the authoritative train from DB (avoid stale snapshot in ticket)
            TrainService trainService = new TrainService();
            Train train = trainService.getTrainById(ticketToCancel.getTrain().getTrainId());

            if (train == null) {
                System.out.println("Associated train not found.");
                return Boolean.FALSE;
            }

            // bounds safety
            int r = ticketToCancel.getRow();
            int c = ticketToCancel.getCol();
            List<List<Integer>> seats = train.getSeats();
            if (r >= 0 && r < seats.size() && c >= 0 && c < seats.get(r).size()) {
                seats.get(r).set(c, 0); // free the seat
                train.setSeats(seats);
                trainService.updateTrain(train);
            }

            // remove ticket from user and persist
            currentUser.removeTicket(ticketToCancel);
            saveUserListToFile();

            System.out.println("Ticket with ID " + ticketId + " has been canceled.");
            return Boolean.TRUE;

        } catch (IOException e) {
            e.printStackTrace();
            return Boolean.FALSE;
        }
    }
}
