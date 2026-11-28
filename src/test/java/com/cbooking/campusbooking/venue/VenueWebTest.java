package com.cbooking.campusbooking.venue;

import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:venues;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class VenueWebTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired Clock clock;
    LocalDate today;

    @BeforeEach
    void fixture() {
        today = LocalDate.now(clock);
        db.execute("DROP TABLE IF EXISTS t_slot");
        db.execute("DROP TABLE IF EXISTS t_venue");
        db.execute("""
                CREATE TABLE t_venue(id BIGINT, name VARCHAR, type INT, location VARCHAR,
                    capacity INT, open_time TIME, close_time TIME, status INT, create_at TIMESTAMP, update_at TIMESTAMP)
                """);
        db.execute("""
                INSERT INTO t_venue VALUES
                (3,'West',2,'B',5,'08:00','22:00',1,NULL,NULL),
                (2,'East',1,'A',8,'08:00','22:00',0,NULL,NULL),
                (1,'East%Hall',0,'A',4,'08:00','20:00',1,'2026-11-01 10:00:00',NULL)
                """);
        db.execute("CREATE TABLE t_slot(id BIGINT, venue_id BIGINT, slot_date DATE, start_time TIME, end_time TIME, status INT)");
        db.update("INSERT INTO t_slot VALUES (2,1,?,'10:00','11:00',1),(1,1,?,'08:00','09:00',0)", today, today);
    }

    @Test
    void listAndDetail() throws Exception {
        mvc.perform(get("/venues").param("name","%").param("type","BADMINTON"))
                .andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.items[0].id").value("1"));
        mvc.perform(get("/venues").param("page","2").param("size","1"))
                .andExpect(jsonPath("$.data.total").value(2)).andExpect(jsonPath("$.data.items[0].id").value("3"));
        mvc.perform(get("/venues/1")).andExpect(jsonPath("$.data.openTime").value("08:00:00"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-11-01T10:00:00+08:00"));
        mvc.perform(get("/venues/2")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(1004));
        mvc.perform(get("/venues").param("size","101")).andExpect(status().isBadRequest());
    }

    @Test
    void slotsAndWindow() throws Exception {
        String path = "/venues/1/slots";
        mvc.perform(get(path).param("date", today.toString()))
                .andExpect(jsonPath("$.data.length()").value(2)).andExpect(jsonPath("$.data[0].status").value("AVAILABLE"));
        mvc.perform(get(path).param("date", today.toString()).param("status","OCCUPIED"))
                .andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0].startTime").value("10:00:00"));
        mvc.perform(get(path).param("date", today.plusDays(13).toString())).andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(get(path).param("date", today.plusDays(14).toString())).andExpect(jsonPath("$.code").value(3004));
        mvc.perform(get("/venues/2/slots").param("date", today.toString())).andExpect(status().isNotFound());
        mvc.perform(get(path)).andExpect(status().isBadRequest());
    }
}
