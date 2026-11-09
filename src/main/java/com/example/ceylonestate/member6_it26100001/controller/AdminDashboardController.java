package com.example.ceylonestate.member6_it26100001.controller;

import com.example.ceylonestate.member2_it26100672.model.ListingStatus;
import com.example.ceylonestate.member6_it26100001.model.MessageStatus;
import com.example.ceylonestate.member1_it26100453.model.User;
import com.example.ceylonestate.member6_it26100001.repository.AdminActivityRepository;
import com.example.ceylonestate.member4_it26100746.repository.BrokerRepository;
import com.example.ceylonestate.member6_it26100001.repository.ContactMessageRepository;
import com.example.ceylonestate.member2_it26100672.repository.PropertyRepository;
import com.example.ceylonestate.member1_it26100453.repository.UserRepository;
import com.example.ceylonestate.member1_it26100453.service.CurrentUserService;
import com.example.ceylonestate.member6_it26100001.service.VisitorStatsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** The admin home page, the Visitors statistics page and the activity log. */
@Controller
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final BrokerRepository brokerRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final AdminActivityRepository adminActivityRepository;
    private final VisitorStatsService visitorStatsService;
    private final CurrentUserService currentUserService;

    public AdminDashboardController(UserRepository userRepository,
                                    PropertyRepository propertyRepository,
                                    BrokerRepository brokerRepository,
                                    ContactMessageRepository contactMessageRepository,
                                    AdminActivityRepository adminActivityRepository,
                                    VisitorStatsService visitorStatsService,
                                    CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.brokerRepository = brokerRepository;
        this.contactMessageRepository = contactMessageRepository;
        this.adminActivityRepository = adminActivityRepository;
        this.visitorStatsService = visitorStatsService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        User me = currentUserService.get().orElse(null);
        model.addAttribute("me", me);
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("adminCount", userRepository.countAdmins());
        model.addAttribute("liveListings",
                propertyRepository.countByStatus(ListingStatus.FOR_SALE) + propertyRepository.countByStatus(ListingStatus.FOR_RENT));
        model.addAttribute("completedListings",
                propertyRepository.countByStatus(ListingStatus.SOLD) + propertyRepository.countByStatus(ListingStatus.RENTED));
        model.addAttribute("brokerCount", brokerRepository.count());
        model.addAttribute("stats", visitorStatsService.compute(14));
        model.addAttribute("newMessages", contactMessageRepository.findTop5ByStatusOrderByCreatedAtDesc(MessageStatus.NEW));
        model.addAttribute("pending", propertyRepository.findByStatusOrderByIdAsc(ListingStatus.PENDING)
                .stream().limit(5).toList());
        model.addAttribute("activity", adminActivityRepository.findTop8ByOrderByCreatedAtDesc());
        return "admin";
    }

    @GetMapping("/admin/visitors")
    public String visitors(Model model) {
        model.addAttribute("stats", visitorStatsService.compute(30));
        model.addAttribute("recent", visitorStatsService.recentViews());
        return "admin/visitors";
    }

    @GetMapping("/admin/activity")
    public String activity(Model model) {
        model.addAttribute("items", adminActivityRepository.findTop100ByOrderByCreatedAtDesc());
        return "admin/activity";
    }
}
