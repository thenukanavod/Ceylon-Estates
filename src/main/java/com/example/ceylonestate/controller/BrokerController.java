package com.example.ceylonestate.controller;

import com.example.ceylonestate.model.Broker;
import com.example.ceylonestate.model.District;
import com.example.ceylonestate.model.Province;
import com.example.ceylonestate.repository.BrokerRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Locale;

/** The public mortgage broker directory (the "broker database" buyers can browse). */
@Controller
public class BrokerController {

    private final BrokerRepository brokerRepository;

    public BrokerController(BrokerRepository brokerRepository) {
        this.brokerRepository = brokerRepository;
    }

    @GetMapping("/brokers")
    public String directory(@RequestParam(required = false) String province,
                            @RequestParam(required = false) String q,
                            Model model) {

        Province selected = Province.fromKey(province);
        List<Broker> brokers = selected == null
                ? brokerRepository.findAllByOrderByNameAsc()
                : brokerRepository.findByDistrictInOrderByNameAsc(District.inProvince(selected));

        if (q != null && !q.isBlank()) {
            String needle = q.trim().toLowerCase(Locale.ROOT);
            brokers = brokers.stream()
                    .filter(b -> (b.getName() + " " + b.getCompany() + " " + b.getDistrict().getLabel())
                            .toLowerCase(Locale.ROOT).contains(needle))
                    .toList();
        }

        model.addAttribute("brokers", brokers);
        model.addAttribute("provinces", Province.values());
        model.addAttribute("selectedProvince", selected == null ? "" : selected.getKey());
        model.addAttribute("q", q == null ? "" : q);
        return "brokers";
    }
}
