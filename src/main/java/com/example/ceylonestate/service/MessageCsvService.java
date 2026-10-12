package com.example.ceylonestate.service;

import com.example.ceylonestate.model.ContactMessage;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * File handling: turns contact messages into the text of a CSV file the admin
 * can open in Excel.
 *
 * Every value is wrapped in quotes with inner quotes doubled, so commas, quotes
 * and line breaks inside a message never break the columns. A value that starts
 * with = + - or @ gets a leading apostrophe so a spreadsheet can never run it as a formula.
 */
@Service
public class MessageCsvService {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public String toCsv(List<ContactMessage> messages) {
        StringBuilder csv = new StringBuilder("\uFEFF"); // marker so Excel reads UTF-8 (Sinhala names) correctly
        csv.append("id,received,status,name,email,phone,subject,message,replied,reply\r\n");
        for (ContactMessage m : messages) {
            csv.append(m.getId()).append(',')
                    .append(cell(m.getCreatedAt().format(WHEN))).append(',')
                    .append(cell(m.getStatus().name())).append(',')
                    .append(cell(m.getName())).append(',')
                    .append(cell(m.getEmail())).append(',')
                    .append(cell(m.getPhone())).append(',')
                    .append(cell(m.getSubject())).append(',')
                    .append(cell(m.getMessage())).append(',')
                    .append(cell(m.isReplied() ? "yes" : "no")).append(',')
                    .append(cell(m.getAdminReply()))
                    .append("\r\n");
        }
        return csv.toString();
    }

    static String cell(String value) {
        if (value == null) {
            return "\"\"";
        }
        String safe = value;
        if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
