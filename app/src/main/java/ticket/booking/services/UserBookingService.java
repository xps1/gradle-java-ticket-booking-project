package ticket.booking.services;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ticket.booking.entities.Ticket;
import ticket.booking.entities.Train;
import ticket.booking.entities.User;
import ticket.booking.services.TrainService;
import ticket.booking.util.UserServiceUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.text.SimpleDateFormat;

public class UserBookingService{

    private ObjectMapper objectMapper = new ObjectMapper();

    private List<User> userList;

    private User user;

    private final String USER_FILE_PATH = "app/src/main/java/ticket/booking/localDb/users.json";

    public UserBookingService(User user) throws IOException {
        this.user = user;
        loadUserListFromFile();
    }

    public UserBookingService() throws IOException {
        loadUserListFromFile();
    }

    private void loadUserListFromFile() throws IOException {
        userList = objectMapper.readValue(new File(USER_FILE_PATH), new TypeReference<List<User>>() {});
    }

    public Boolean loginUser(){
        Optional<User> foundUser = userList.stream().filter(user1 -> {
            return user1.getName().equals(user.getName()) && UserServiceUtil.checkPassword(user.getPassword(), user1.getHashedPassword());
        }).findFirst();
        return foundUser.isPresent();
    }

    public Boolean signUp(User user1){
        try{
            userList.add(user1);
            saveUserListToFile();
            return Boolean.TRUE;
        }catch (IOException ex){
            return Boolean.FALSE;
        }
    }

    private void saveUserListToFile() throws IOException {
        File usersFile = new File(USER_FILE_PATH);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(usersFile, userList);
    }

    public void fetchBookings(){
        Optional<User> userFetched = userList.stream().filter(user1 -> {
            return user1.getName().equals(user.getName()) && UserServiceUtil.checkPassword(user.getPassword(), user1.getHashedPassword());
        }).findFirst();
        if(userFetched.isPresent()){
            userFetched.get().printTickets();
        }
    }

    public Boolean cancelBooking(String ticketId) {

        if (ticketId == null || ticketId.isEmpty()) {
            System.out.println("Ticket ID cannot be null or empty.");
            return Boolean.FALSE;
        }

        Optional<User> userFetched = userList.stream()
                .filter(u -> u.getName().equals(user.getName())
                        && UserServiceUtil.checkPassword(user.getPassword(), u.getHashedPassword()))
                .findFirst();

        if (userFetched.isEmpty()) {
            System.out.println("User not found.");
            return Boolean.FALSE;
        }

        User currentUser = userFetched.get();

        // ✅ Instead of removeIf, find the ticket first
        Ticket ticketToCancel = currentUser.getTicketsBooked().stream()
                .filter(ticket -> ticket.getTicketId().equals(ticketId))
                .findFirst()
                .orElse(null);

        if (ticketToCancel == null) {
            System.out.println("No ticket found with ID " + ticketId);
            return Boolean.FALSE;
        }

        // ✅ Free the seat
        Train train = ticketToCancel.getTrain();
        train.getSeats().get(ticketToCancel.getRow()).set(ticketToCancel.getCol(), 0);

        try {
            // persist updated train
            TrainService trainService = new TrainService();
            trainService.addTrain(train);

            // remove ticket from user
            currentUser.getTicketsBooked().remove(ticketToCancel);
            saveUserListToFile();

            System.out.println("Ticket with ID " + ticketId + " has been canceled.");
            return Boolean.TRUE;
        } catch (IOException e) {
            e.printStackTrace();
            return Boolean.FALSE;
        }
    }


    public List<Train> getTrains(String source, String destination){
        try{
            TrainService trainService = new TrainService();
            return trainService.searchTrains(source, destination);
        }catch(IOException ex){
            return new ArrayList<>();
        }
    }

    public List<List<Integer>> fetchSeats(Train train){
        return train.getSeats();
    }

    public Boolean bookTrainSeat(Train train, int row, int seat) {
        try {
            TrainService trainService = new TrainService();
            List<List<Integer>> seats = train.getSeats();

            // ✅ Bounds check
            if (row < 0 || row >= seats.size() || seat < 0 || seat >= seats.get(row).size()) {
                System.out.println("Invalid seat selection.");
                return Boolean.FALSE;
            }

            // ✅ Already booked check
            if (seats.get(row).get(seat) == 1) {
                System.out.println("Seat already booked.");
                return Boolean.FALSE;
            }

            // ✅ Mark booked
            seats.get(row).set(seat, 1);
            train.setSeats(seats);

            // Save updated train
            trainService.addTrain(train);

            // ✅ Fetch user
            Optional<User> userFetched = userList.stream()
                    .filter(u -> u.getName().equals(user.getName())
                            && UserServiceUtil.checkPassword(user.getPassword(), u.getHashedPassword()))
                    .findFirst();

            if (userFetched.isEmpty()) {
                System.out.println("User not found.");
                return Boolean.FALSE;
            }

            User currentUser = userFetched.get();

            // ✅ Travel date formatting (keep your existing string flow)
            String travelDateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            // ✅ Create ticket with row/col
            Ticket ticket = new Ticket(
                    UUID.randomUUID().toString(),                 // ticketId
                    currentUser.getUserId(),                      // userId
                    train.getStations().get(0),                   // source
                    train.getStations().get(train.getStations().size() - 1), // destination
                    travelDateStr,                                // travel date (string)
                    train                                         // train
            );

            // Manually set row/col since your constructor doesn’t take them yet
            ticket.setRow(row);
            ticket.setCol(seat);

            // Add ticket to user
            currentUser.getTicketsBooked().add(ticket);

            // Save updated user list
            saveUserListToFile();

            System.out.println("Booking successful. Ticket ID: " + ticket.getTicketId());
            return Boolean.TRUE;

        } catch (IOException ex) {
            ex.printStackTrace();
            return Boolean.FALSE;
        }
    }



}