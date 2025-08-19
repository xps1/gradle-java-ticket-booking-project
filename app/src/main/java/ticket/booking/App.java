package ticket.booking;

import ticket.booking.entities.Train;
import ticket.booking.entities.User;
import ticket.booking.services.UserBookingService;
import ticket.booking.util.UserServiceUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class App {

    public static void main(String[] args) {
        System.out.println("Running Train Booking System");
        Scanner scanner = new Scanner(System.in);
        int option = 0;

        UserBookingService userBookingService;
        try {
            userBookingService = new UserBookingService();
        } catch (Exception ex) {
            System.out.println("There is something wrong");
            ex.printStackTrace();
            return;
        }

        Train trainSelectedForBooking = null;

        while (option != 7) {
            System.out.println("Choose option");
            System.out.println("1. Sign up");
            System.out.println("2. Login");
            System.out.println("3. Fetch Bookings");
            System.out.println("4. Search Trains");
            System.out.println("5. Book a Seat");
            System.out.println("6. Cancel my Booking");
            System.out.println("7. Exit the App");

            if (!scanner.hasNextInt()) {
                System.out.println("Please enter a number 1-7.");
                scanner.next(); // consume invalid
                continue;
            }
            option = scanner.nextInt();

            switch (option) {
                case 1: {
                    System.out.println("Enter the username to signup");
                    String nameToSignUp = scanner.next();
                    System.out.println("Enter the password to signup");
                    String passwordToSignUp = scanner.next();

                    User userToSignup = new User(
                            nameToSignUp,
                            passwordToSignUp,                               // plain (used only for current session)
                            UserServiceUtil.hashPassword(passwordToSignUp), // stored hash
                            new ArrayList<>(),
                            UUID.randomUUID().toString()
                    );
                    boolean ok = userBookingService.signUp(userToSignup);
                    System.out.println(ok ? "Sign up successful." : "Sign up failed.");
                    break;
                }
                case 2: {
                    System.out.println("Enter the username to Login");
                    String nameToLogin = scanner.next();
                    System.out.println("Enter the password to Login");
                    String passwordToLogin = scanner.next();

                    // The constructor takes a User whose plain password will be checked against the stored hash.
                    User userToLogin = new User(
                            nameToLogin,
                            passwordToLogin,
                            "",                         // hash not required here
                            new ArrayList<>(),
                            ""                          // id not required for login lookup
                    );
                    try {
                        userBookingService = new UserBookingService(userToLogin);
                        System.out.println("Logged in (context set).");
                    } catch (IOException ex) {
                        System.out.println("Login failed due to an error.");
                    }
                    break;
                }
                case 3: {
                    System.out.println("Fetching your bookings");
                    userBookingService.fetchBookings();
                    break;
                }
                case 4: {
                    System.out.println("Type your source station");
                    String source = scanner.next();
                    System.out.println("Type your destination station");
                    String dest = scanner.next();

                    List<Train> trains = userBookingService.getTrains(source, dest);
                    if (trains.isEmpty()) {
                        System.out.println("No trains found for that route.");
                        break;
                    }

                    int index = 1;
                    for (Train t : trains) {
                        System.out.println(index + " Train id : " + t.getTrainId());
                        for (Map.Entry<String, String> entry : t.getStationTimes().entrySet()) {
                            System.out.println("station " + entry.getKey() + " time: " + entry.getValue());
                        }
                        index++;
                    }
                    System.out.println("Select a train by typing 1.." + trains.size());

                    if (!scanner.hasNextInt()) {
                        System.out.println("Invalid choice.");
                        scanner.next();
                        break;
                    }
                    int trainChoice = scanner.nextInt();
                    if (trainChoice < 1 || trainChoice > trains.size()) {
                        System.out.println("Invalid train number.");
                        break;
                    }
                    trainSelectedForBooking = trains.get(trainChoice - 1);
                    System.out.println("Train selected: " + trainSelectedForBooking.getTrainId());
                    break;
                }
                case 5: {
                    if (trainSelectedForBooking == null) {
                        System.out.println("Please search and select a train first (Option 4).");
                        break;
                    }

                    System.out.println("Select a seat out of these seats");
                    List<List<Integer>> seats = userBookingService.fetchSeats(trainSelectedForBooking);
                    for (List<Integer> row : seats) {
                        for (Integer val : row) {
                            System.out.print(val + " ");
                        }
                        System.out.println();
                    }
                    System.out.println("Select the seat by typing the row and column");
                    System.out.println("Enter the row");
                    if (!scanner.hasNextInt()) { System.out.println("Invalid row."); scanner.next(); break; }
                    int row = scanner.nextInt();
                    System.out.println("Enter the column");
                    if (!scanner.hasNextInt()) { System.out.println("Invalid column."); scanner.next(); break; }
                    int col = scanner.nextInt();

                    System.out.println("Booking your seat....");
                    Boolean booked = userBookingService.bookTrainSeat(trainSelectedForBooking, row, col);
                    if (booked.equals(Boolean.TRUE)) {
                        System.out.println("Booked! Enjoy your journey");
                    } else {
                        System.out.println("Can't book this seat");
                    }
                    break;
                }
                case 6: {
                    System.out.println("Enter the Ticket ID you want to cancel:");
                    scanner.nextLine(); // consume newline
                    String ticketIdToCancel = scanner.nextLine();

                    Boolean cancelled = userBookingService.cancelBooking(ticketIdToCancel);
                    if (cancelled.equals(Boolean.TRUE)) {
                        System.out.println("Ticket canceled successfully.");
                    } else {
                        System.out.println("Could not cancel the ticket. Check Ticket ID.");
                    }
                    break;
                }
                case 7: {
                    System.out.println("Exiting app...");
                    break;
                }
                default:
                    System.out.println("Invalid option, try again.");
            }
        }
    }
}
