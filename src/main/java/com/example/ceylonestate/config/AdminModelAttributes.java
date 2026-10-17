package com.example.ceylonestate.config;

import com.example.ceylonestate.model.ApplicationStatus;
import com.example.ceylonestate.model.ListingStatus;
import com.example.ceylonestate.model.MessageStatus;
import com.example.ceylonestate.repository.BrokerApplicationRepository;
import com.example.ceylonestate.repository.ContactMessageRepository;
import com.example.ceylonestate.repository.PropertyRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Adds the "unread messages" and "pending listings" counters to every admin page,
 * so the admin menu can show badges without each controller passing them.
 */
@ControllerAdvice
public class AdminModelAttributes {

    private final ContactMessageRepository contactMessageRepository;
    private final PropertyRepository propertyRepository;
    private final BrokerApplicationRepository brokerApplicationRepository;

    public AdminModelAttributes(ContactMessageRepository contactMessageRepository,
                                PropertyRepository propertyRepository,
                                BrokerApplicationRepository brokerApplicationRepository) {
        this.contactMessageRepository = contactMessageRepository;
        this.propertyRepository = propertyRepository;
        this.brokerApplicationRepository = brokerApplicationRepository;
    }

    @ModelAttribute
    public void addAdminCounters(HttpServletRequest request, Model model) {
        if (!request.getRequestURI().substring(request.getContextPath().length()).startsWith("/admin")) {
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.isAuthenticated()
                && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin) {
            return;
        }
        model.addAttribute("unreadMessages", contactMessageRepository.countByStatus(MessageStatus.NEW));
        model.addAttribute("pendingListings", propertyRepository.countByStatus(ListingStatus.PENDING));
        model.addAttribute("pendingApplications", brokerApplicationRepository.countByStatus(ApplicationStatus.PENDING));
    }
}
