package ticket.booking.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.ArrayList;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    private String name;
    private String password;        // plain for current session/login only (not persisted)
    private String hashedPassword;  // persisted
    private List<Ticket> ticketsBooked;
    private String userId;

    public User(String name, String password, String hashedPassword, List<Ticket> ticketsBooked, String userId){
        this.name = name;
        this.password = password;
        this.hashedPassword = hashedPassword;
        this.ticketsBooked = ticketsBooked;
        this.userId = userId;
    }
    public User(){
        this.ticketsBooked = new ArrayList<>();
    }

    public User(String name, String password){
        this.name = name;
        this.password = password;
    }

    public String getName() { return name; }
    public String getPassword(){ return password; }
    public String getHashedPassword() { return hashedPassword; }
    public List<Ticket> getTicketsBooked() { return ticketsBooked; }
    public String getUserId() { return userId; }

    public void setName(String name) { this.name = name; }
    public void setHashedPassword(String hashedPassword) { this.hashedPassword = hashedPassword; }
    public void setTicketsBooked(List<Ticket> ticketsBooked) { this.ticketsBooked = ticketsBooked; }
    public void setUserId(String userId) { this.userId = userId; }

    public void addTicket(Ticket ticket) {
        if (ticketsBooked == null) {
            ticketsBooked = new ArrayList<>();
        }
        ticketsBooked.add(ticket);
    }

    public void removeTicket(Ticket ticket) {
        if (ticket != null && ticketsBooked != null) {
            ticketsBooked.remove(ticket);
        }
    }

    public void printTickets() {
        if (ticketsBooked == null || ticketsBooked.isEmpty()) {
            System.out.println("No tickets booked yet.");
            return;
        }
        for (Ticket ticket : ticketsBooked) {
            System.out.println(ticket.getTicketInfo());
        }
    }
}
