package com.demo.ranktracker;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Controller
public class RankTracker {
    private final SearchApiClient searchApi;
    private final String domain;
    private static final Logger log = LoggerFactory.getLogger(RankTracker.class);
    private final JdbcClient db;

    public RankTracker(SearchApiClient searchApi,
                       @Value("${tracker.domain}") String domain, JdbcClient db) {
        this.searchApi = searchApi;
        this.domain = domain;
        this.db = db;
    }

    public void check(String keyword) {
        LocalDate today = LocalDate.now();
        boolean checkedToday = db.sql("SELECT COUNT(*) FROM ranking WHERE keyword = ? AND checked_on = ?")
                .params(keyword, today).query(Integer.class).single() > 0;

        if (checkedToday) {
            log.info("Already checked keyword {} today", keyword);
            return;
        }

        Integer position = searchApi.top100(keyword).stream()
                .filter(result -> result.domain() != null
                        && (result.domain().equals(domain) || result.domain().endsWith("." + domain)))
                .map(SearchApiClient.Result::position)
                .findFirst()
                .orElse(null);

        db.sql("INSERT INTO ranking (keyword, position, checked_on) " +
                        "VALUES (?, ?, ?)")
                .params(keyword, position, today).update();

        log.info("Position for keyword {} on domain {}: {}", keyword, domain,
                position == null ? "Not in top 100" : "#" + position);
    }


    @GetMapping("/")
    public String dashboard(Model model) {
        // read from DB
        List<Ranking> rankings = db.sql("SELECT keyword, checked_on, position FROM ranking ORDER BY checked_on")
                .query(Ranking.class).list();
        Map<String, List<Ranking>> byKeyword = rankings.stream()
                .collect(Collectors.groupingBy(Ranking::keyword, TreeMap::new, Collectors.toList()));
        List<LocalDate> days = rankings.stream().map(Ranking::checkedOn).distinct().toList();


        // Table: latest position vs the check before it
        List<Row> rows = byKeyword.values().stream().map(history -> {
            Integer now = history.getLast().position();
            Integer before = history.size() > 1 ? history.get(history.size() - 2).position() : null;
            return new Row(history.getLast().keyword(), now, change(before, now), alert(before, now));
        }).toList();

        // Chart: one line per keyword, one point per day
        Map<String, List<Integer>> lines = new TreeMap<>();
        byKeyword.forEach((keyword, history) -> lines.put(keyword, days.stream()
                .map(day -> history.stream().filter(r -> r.checkedOn().equals(day)).findFirst()
                        .map(Ranking::position).orElse(null))
                .toList()));

        model.addAttribute("domain", domain);
        model.addAttribute("rows", rows);
        model.
                addAttribute("days", days);
        model.addAttribute("lines", lines);

        return "dashboard";
    }

    @PostMapping("/keywords")
    public String addKeyword(@RequestParam String keyword,
                             RedirectAttributes redirect) {
        try {
            if (!keyword.isBlank()) {
                check(keyword.strip()); // check right away, so you don't wait until tomorrow to see a number
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }

    @Scheduled(cron = "${tracker.check-cron}")
    @EventListener(ApplicationReadyEvent.class)
    public void checkAll() {
        for (String keyword : db.sql("SELECT DISTINCT keyword FROM ranking")
                .query(String.class).list()) {
            try {
                check(keyword);
            } catch (Exception e) {
                System.err.println("Error checking keyword " + keyword + ": " + e.getMessage());
            }
        }
    }

    record Ranking(String keyword, LocalDate checkedOn, Integer position) {
    }

    record Row(String keyword, Integer position, String change, String alert) {
    }

    private static String change(Integer before, Integer now) {
        if (before == null || now == null || before.equals(now)) {
            return "";
        }
        return now < before ? "▲ " + (before - now) : "▼ " + (now - before);
    }

    private static String alert(Integer before, Integer now) {
        if (before == null || Objects.equals(before, now)) {
            return null;
        }
        if (now == null) {
            return "dropped out of the top 100 (was #" + before + ")";
        }
        if (before > 10 && now <= 10) {
            return "entered the top 10 (#" + before + " → #" + now + ")";
        }
        if (Math.abs(before - now) >= 5) {
            return "moved " + (now < before ? "up " : "down ") + Math.abs(before - now) + " places (#" + before + " → #" + now + ")";
        }
        return null;
    }
}
