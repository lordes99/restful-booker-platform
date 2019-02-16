package api;

import db.BookingDB;
import model.Booking;
import model.BookingResults;
import model.CreatedBooking;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import requests.AuthRequests;
import utils.DatabaseScheduler;
import validators.DateCheckValidator;

import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RestController
public class BookingController {

    private BookingDB bookingDB;
    private AuthRequests authRequests;
    private DateCheckValidator dateCheckValidator;

    @Bean
    public WebMvcConfigurer configurer() {
        DatabaseScheduler databaseScheduler = new DatabaseScheduler();
        databaseScheduler.startScheduler(bookingDB, TimeUnit.MINUTES);

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String originHost = "http://localhost:3003";

                if(System.getenv("cors") != null){
                    originHost = System.getenv("cors");
                }

                registry.addMapping("/*")
                        .allowedMethods("GET", "POST", "DELETE", "PUT")
                        .allowedOrigins(originHost)
                        .allowCredentials(true);
            }
        };
    }

    public BookingController() throws SQLException {
        bookingDB = new BookingDB(true);
        authRequests = new AuthRequests();
        dateCheckValidator = new DateCheckValidator();
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<BookingResults> getBookings(@RequestParam("roomid") Optional<String> roomid, @RequestParam("keyword") Optional<String> keyword) throws SQLException {
        if(roomid.isPresent()){
            BookingResults searchResults = new BookingResults(bookingDB.queryBookingsById(roomid.get()));
            return ResponseEntity.ok(searchResults);
        }

        if(keyword.isPresent()){
            BookingResults searchResults = new BookingResults(bookingDB.queryBookingsByName(keyword.get()));
            return ResponseEntity.ok(searchResults);
        }

        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ResponseEntity<?> createBooking(@RequestBody Booking booking, @CookieValue(value ="token", required = false) String token) throws SQLException {
        if(authRequests.postCheckAuth(token)) {
            if(dateCheckValidator.isValid(booking.getBookingDates())) {
                if (bookingDB.checkForBookingConflict(booking)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).build();
                } else {
                    CreatedBooking body = bookingDB.create(booking);
                    return ResponseEntity.ok(body);
                }
            } else {
                return ResponseEntity.badRequest()
                        .body("Dates must be set and Checkout must be after Checkin");
            }
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @RequestMapping(value = "/{id:[0-9]*}", method = RequestMethod.GET)
    public Booking getBooking(@PathVariable(value = "id") int id) throws SQLException {
        return bookingDB.query(id);
    }

    @RequestMapping(value = "/{id:[0-9]*}", method = RequestMethod.DELETE)
    public ResponseEntity deleteBooking(@PathVariable(value = "id") int id, @CookieValue(value ="token", required = false) String token) throws SQLException {
        if(authRequests.postCheckAuth(token)){
            if(bookingDB.delete(id)){
                return ResponseEntity.accepted().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @RequestMapping(value = "/{id:[0-9]*}", method = RequestMethod.PUT)
    public ResponseEntity<CreatedBooking> updateBooking(@RequestBody Booking booking, @PathVariable(value = "id") int id, @CookieValue(value ="token", required = false) String token) throws SQLException {
        if(authRequests.postCheckAuth(token)){
            return ResponseEntity.ok(bookingDB.update(id, booking));
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

}
