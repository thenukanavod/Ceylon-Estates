package com.example.ceylonestate.config;

import com.example.ceylonestate.model.AdminUser;
import com.example.ceylonestate.model.ApartmentProperty;
import com.example.ceylonestate.model.Article;
import com.example.ceylonestate.model.ArticleTopic;
import com.example.ceylonestate.model.Broker;
import com.example.ceylonestate.model.ContactMessage;
import com.example.ceylonestate.model.District;
import com.example.ceylonestate.model.HouseProperty;
import com.example.ceylonestate.model.LandProperty;
import com.example.ceylonestate.model.ListingStatus;
import com.example.ceylonestate.model.LocalInfo;
import com.example.ceylonestate.model.MessageStatus;
import com.example.ceylonestate.model.PageView;
import com.example.ceylonestate.model.Property;
import com.example.ceylonestate.model.PropertyImage;
import com.example.ceylonestate.repository.ArticleRepository;
import com.example.ceylonestate.repository.BrokerRepository;
import com.example.ceylonestate.repository.ContactMessageRepository;
import com.example.ceylonestate.repository.LocalInfoRepository;
import com.example.ceylonestate.repository.PageViewRepository;
import com.example.ceylonestate.repository.PropertyRepository;
import com.example.ceylonestate.repository.ViewingRequestRepository;
import com.example.ceylonestate.repository.SavedPropertyRepository;
import com.example.ceylonestate.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static com.example.ceylonestate.model.District.*;
import static com.example.ceylonestate.model.ListingStatus.*;

/**
 * Fills an empty database with sample brokers, listings and local info the
 * first time the app starts, so every page has something to show.
 * Each table is only seeded if it is empty, so restarting never duplicates
 * data - and anything you add or delete through the site is left alone.
 *
 * Listing photos point at /images/properties/{house|apartment|land}1..5.{jpg|png}
 * - the same files and names used by the homepage "Project Library" gallery
 * (see PHOTO_EXTENSIONS below for which ones are .jpg vs .png).
 * Drop real photos with those names into src/main/resources/static/images/properties/
 * - until you do, the cards simply show their coloured placeholder.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final String SALES_NAME = "Ceylon Estates Sales Desk";
    private static final String SALES_PHONE = "011 234 5678";
    private static final String SALES_EMAIL = "sales@ceylonestates.example";

    /**
     * The 15 bundled property photos are a mix of .jpg and .png (whatever
     * format each one was supplied in) - this says which extension each
     * "{type}{number}" file uses. Anything not listed here defaults to .png.
     * Keep this in sync with the filenames referenced in index.html's
     * "Project Library" gallery.
     */
    private static final java.util.Map<String, String> PHOTO_EXTENSIONS = java.util.Map.of(
            "apartment3", "jpg",
            "apartment4", "jpg",
            "land5", "jpg"
    );

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final PropertyRepository propertyRepository;
    private final BrokerRepository brokerRepository;
    private final ArticleRepository articleRepository;
    private final LocalInfoRepository localInfoRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ContactMessageRepository contactMessageRepository;
    private final PageViewRepository pageViewRepository;
    private final SavedPropertyRepository savedPropertyRepository;
    private final ViewingRequestRepository viewingRequestRepository;

    // The first administrator account comes from application.properties (never from the website).
    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    public DataSeeder(PropertyRepository propertyRepository,
                      BrokerRepository brokerRepository,
                      ArticleRepository articleRepository,
                      LocalInfoRepository localInfoRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      ContactMessageRepository contactMessageRepository,
                      PageViewRepository pageViewRepository,
                      SavedPropertyRepository savedPropertyRepository,
                      ViewingRequestRepository viewingRequestRepository) {
        this.propertyRepository = propertyRepository;
        this.brokerRepository = brokerRepository;
        this.articleRepository = articleRepository;
        this.localInfoRepository = localInfoRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.contactMessageRepository = contactMessageRepository;
        this.pageViewRepository = pageViewRepository;
        this.savedPropertyRepository = savedPropertyRepository;
        this.viewingRequestRepository = viewingRequestRepository;
    }

    /**
     * @Transactional lives here (not on backfillMissingPhotos() itself)
     * because Spring's transaction proxy only intercepts calls that come
     * from outside the bean - Spring Boot calling run() on the bean it
     * holds goes through the proxy, but run() then calling
     * backfillMissingPhotos() on "this" does not, so putting it there was
     * silently ignored (self-invocation bypasses the proxy either way,
     * regardless of the method's visibility).
     */
    @Override
    @Transactional
    public void run(String... args) {
        if (brokerRepository.count() == 0) {
            seedBrokers();
        }
        seedArticles();
        if (localInfoRepository.count() == 0) {
            seedLocalInfo();
        }
        if (propertyRepository.count() == 0) {
            seedListings();
        }
        removeMistakenDuplicates();
        seedCompletedProjects();
        backfillMissingPhotos();
        seedGalleryListings();
        backfillViewCounts();
        seedAdminAccount();
        if (contactMessageRepository.count() == 0) {
            seedContactMessages();
        }
        if (pageViewRepository.count() == 0) {
            seedPageViews();
        }
    }

    // ------------------------------------------------------------------
    // First admin account, sample inbox and sample visitor statistics
    // ------------------------------------------------------------------

    /** Creates the first admin from application.properties, but only if there is no admin yet. */
    private void seedAdminAccount() {
        if (userRepository.countAdmins() > 0) {
            return;
        }
        if (adminPassword == null || adminPassword.isBlank() || adminEmail == null || adminEmail.isBlank()) {
            log.warn("No admin account exists. Set app.admin.username, app.admin.email and app.admin.password "
                    + "in application.properties and restart to create the first admin.");
            return;
        }
        if (userRepository.findByUsername(adminUsername).isPresent() || userRepository.findByEmail(adminEmail).isPresent()) {
            log.warn("Could not create the first admin: that username or email is already used by a normal account. "
                    + "Pick another one, or promote the account from the database.");
            return;
        }
        userRepository.save(new AdminUser(adminUsername, adminEmail, passwordEncoder.encode(adminPassword)));
        log.info("Created the first admin account '{}'. Sign in at /admin/login.", adminUsername);
    }

    private void seedContactMessages() {
        message("Sanduni Rathnayake", "sanduni.r@example.com", "077 123 4567", "Viewing a house in Nugegoda",
                "Hi, I would like to arrange a viewing for the 4-bedroom house listed in Nugegoda this weekend. "
                        + "Is Saturday afternoon possible?", MessageStatus.NEW, 2);
        message("Mohamed Faizal", "faizal.m@example.com", null, "Selling my land in Kurunegala",
                "I have 40 perches near Kurunegala town and want to list it. How does the approval process work "
                        + "and how long does it take?", MessageStatus.NEW, 9);
        message("Priyanka Silva", "priyanka.s@example.com", "071 765 4321", "Pet-friendly rentals in Colombo",
                "Do you have any apartments for rent in Colombo that allow dogs? We have a small labrador.",
                MessageStatus.READ, 27);
        message("Kamal Jayawardena", "kamal.j@example.com", null, "Broker enquiry",
                "I am a licensed mortgage broker in Matara. How can I be added to your broker directory?",
                MessageStatus.READ, 52);
        ContactMessage replied = message("Thilini Perera", "thilini.p@example.com", "075 222 3344", "Question about listing fees",
                "Is it free to list a house for sale? I could not find the pricing anywhere.", MessageStatus.READ, 80);
        replied.markReplied("Hello Thilini, listing on Ceylon Estates is free until the property sells. "
                + "Just sign in, open List Your Property and submit the details. Our team reviews every listing "
                + "before it goes live.", "admin");
        contactMessageRepository.save(replied);
        message("Ashan Gunasekara", "ashan.g@example.com", null, "Old enquiry",
                "Thanks, I found what I was looking for.", MessageStatus.ARCHIVED, 150);
    }

    private ContactMessage message(String name, String email, String phone, String subject, String text,
                                   MessageStatus status, int hoursAgo) {
        ContactMessage m = new ContactMessage(name, email, phone, subject, text);
        m.setStatus(status);
        m.setCreatedAt(LocalDateTime.now().minusHours(hoursAgo));
        return contactMessageRepository.save(m);
    }

    /** About a month of believable sample visits so the dashboard has something to show. */
    private void seedPageViews() {
        Random random = new Random(42);
        List<Long> listingIds = new ArrayList<>();
        for (Property p : propertyRepository.findByStatusInOrderByListingDateDescIdDesc(ListingStatus.LIVE)) {
            listingIds.add(p.getId());
        }
        List<String> visitors = new ArrayList<>();
        for (int i = 0; i < 140; i++) {
            visitors.add(UUID.randomUUID().toString());
        }
        String[] referrers = {null, null, null, null, null, "google.com", "google.com", "google.com",
                "facebook.com", "facebook.com", "instagram.com", "whatsapp.com"};
        String[] devices = {"Mobile", "Mobile", "Mobile", "Mobile", "Mobile", "Mobile", "Desktop", "Desktop",
                "Desktop", "Desktop", "Tablet"};

        List<PageView> batch = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int daysAgo = 29; daysAgo >= 0; daysAgo--) {
            int views = 12 + random.nextInt(30) + (29 - daysAgo) / 2; // slowly growing traffic
            LocalDate day = LocalDate.now().minusDays(daysAgo);
            for (int i = 0; i < views; i++) {
                LocalDateTime when = day.atTime(7 + random.nextInt(16), random.nextInt(60), random.nextInt(60));
                if (when.isAfter(now)) {
                    when = now.minusMinutes(random.nextInt(120));
                }
                batch.add(new PageView(randomPath(random, listingIds),
                        visitors.get(random.nextInt(visitors.size())), when,
                        devices[random.nextInt(devices.length)],
                        referrers[random.nextInt(referrers.length)]));
            }
        }
        pageViewRepository.saveAll(batch);
    }

    private String randomPath(Random random, List<Long> listingIds) {
        int roll = random.nextInt(100);
        if (roll < 28) return "/";
        if (roll < 48) return "/properties";
        if (roll < 78 && !listingIds.isEmpty()) {
            // a few listings are much more popular than the rest
            int index = Math.min(listingIds.size() - 1, (int) (Math.abs(random.nextGaussian()) * listingIds.size() / 4));
            return "/properties/" + listingIds.get(index);
        }
        if (roll < 84) return "/projects";
        if (roll < 90) return "/brokers";
        if (roll < 95) return "/local-info";
        if (roll < 98) return "/contact";
        return "/sell";
    }

    // ------------------------------------------------------------------
    // Articles - the full texts live in src/main/resources/articles/*.txt
    // (header lines "title:", "source:", "topic:", "summary:", then "---", then the body)
    // ------------------------------------------------------------------
    private void seedArticles() {
        // Rows from the old link-only version have no text: remove them so the real articles replace them.
        articleRepository.deleteAll(articleRepository.findAll().stream()
                .filter(a -> a.getBody() == null || a.getBody().isBlank()).toList());
        if (articleRepository.count() > 0) {
            return;
        }
        try {
            Resource[] files = new PathMatchingResourcePatternResolver().getResources("classpath:articles/*.txt");
            java.util.Arrays.sort(files, java.util.Comparator.comparing(Resource::getFilename));
            for (Resource file : files) {
                String text = new String(file.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                String[] parts = text.replace("\r\n", "\n").split("\n---\n", 2);
                if (parts.length < 2) {
                    continue;
                }
                java.util.Map<String, String> header = new java.util.HashMap<>();
                for (String line : parts[0].split("\n")) {
                    int colon = line.indexOf(':');
                    if (colon > 0) {
                        header.put(line.substring(0, colon).strip(), line.substring(colon + 1).strip());
                    }
                }
                articleRepository.save(new Article(header.get("title"), header.get("source"), header.get("summary"),
                        ArticleTopic.valueOf(header.get("topic")), parts[1].strip()));
            }
        } catch (java.io.IOException e) {
            log.warn("Could not load the article files: {}", e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Brokers - the nine on the homepage plus two more
    // ------------------------------------------------------------------

    private void seedBrokers() {
        broker("Nadeesha Perera", COLOMBO, "Island Mortgage Partners", "MB-1001",
                "Home-loan specialist for first-time buyers in Colombo and the western suburbs.");
        broker("Kasun Wickramasinghe", KANDY, "Hill Country Home Finance", "MB-1002",
                "Helps families across the central hills compare bank and housing-loan options.");
        broker("Dilani Fernando", GALLE, "Southern Coast Mortgages", "MB-1003",
                "Specialises in coastal homes and holiday-property financing in Galle and Matara.");
        broker("Thurairajah Kumar", JAFFNA, "Northern Home Loans", "MB-1004",
                "Local adviser for buyers and returning families in the Northern Province.");
        broker("Farhana Rizvi", TRINCOMALEE, "Eastern Property Finance", "MB-1005",
                "Land and housing loans across Trincomalee, Batticaloa and Ampara.");
        broker("Ruwan Jayasuriya", KURUNEGALA, "Rajarata Mortgage Group", "MB-1006",
                "Mortgage and land-purchase loans in the North Western Province.");
        broker("Chamari Bandara", ANURADHAPURA, "North Central Home Finance", "MB-1007",
                "Experienced with agricultural land and small-business property loans.");
        broker("Sampath Herath", BADULLA, "Uva Loan Advisors", "MB-1008",
                "Guides buyers of hill-country homes and tea-land plots in Uva.");
        broker("Nimali Rathnayake", RATNAPURA, "Sabaragamuwa Property Finance", "MB-1009",
                "Residential and land loans in Ratnapura and Kegalle.");
        broker("Isuru Senanayake", GAMPAHA, "Negombo Home Loans", "MB-1010",
                "Mortgage adviser for Negombo, Gampaha and airport-corridor properties.");
        broker("Anoma Gunawardena", MATARA, "Ruhuna Mortgage Services", "MB-1011",
                "Home loans for families and investors around Matara and Hambantota.");
    }

    private void broker(String name, District district, String company, String licence, String bio) {
        String slug = name.toLowerCase().replace(" ", ".");
        String phone = "077 " + (100 + brokerRepository.count()) + " " + (4500 + brokerRepository.count());
        brokerRepository.save(new Broker(name, slug + "@brokers.example", phone, company, licence, district, bio));
    }

    // ------------------------------------------------------------------
    // Listings
    // ------------------------------------------------------------------

    private void seedListings() {
        // ----- Apartments -----
        save(apartment("Emerald Heights – Kandy", KANDY, "Kandy", 8_200_000, false, FOR_SALE, 2, 2, 980, 5, true, false, 2,
                "Bright two-bedroom apartment a short walk from Kandy Lake, with a lift, secure parking and hill views.", 1));
        save(apartment("Skyline Towers – Colombo", COLOMBO, "Colombo 03", 22_750_000, false, FOR_SALE, 3, 2, 1450, 18, true, false, 4,
                "High-floor three-bedroom apartment with sea and city views, a gym and 24-hour security.", 2));
        save(apartment("Ocean View Apartments", COLOMBO, "Mount Lavinia", 31_000_000, false, FOR_SALE, 3, 3, 1700, 9, true, false, 6,
                "Spacious beachside apartment steps from the Mount Lavinia seafront, with a shared pool.", 3));
        save(apartment("Cozy 2BR Apartment – Rajagiriya", COLOMBO, "Rajagiriya", 95_000, true, FOR_RENT, 2, 1, 950, 4, true, true, 1,
                "Well-kept apartment near Ayurveda Mawatha, ideal for a young family. Pets welcome with a small deposit.", 4));
        save(apartment("Studio Apartment – Wellawatte", COLOMBO, "Wellawatte", 55_000, true, FOR_RENT, 1, 1, 500, 2, false, false, 3,
                "Compact modern studio close to the beach and Galle Road, fully furnished.", 5));
        save(apartment("Pearl Residencies – Battaramulla", COLOMBO, "Battaramulla", 27_500_000, false, SOLD, 3, 2, 1380, 7, true, false, 30,
                "Gated-community apartment near the Parliament road, handed over to its new owners.", 6));
        save(apartment("Harbour View Apartment – Galle", GALLE, "Galle", 14_200_000, false, FOR_SALE, 2, 2, 1100, 3, true, true, 5,
                "Modern apartment with partial harbour views, five minutes from the Galle Fort.", 7));
        save(apartment("Hilltop Apartments – Nuwara Eliya", NUWARA_ELIYA, "Nuwara Eliya", 9_800_000, false, FOR_SALE, 2, 1, 900, 2, false, true, 8,
                "Cool-climate apartment overlooking Gregory Lake, suited to a holiday home or full-time living.", 8));
        save(apartment("Marine Drive Flat – Negombo", GAMPAHA, "Negombo", 65_000, true, RENTED, 2, 2, 1000, 3, true, true, 40,
                "Sea-facing rental near Negombo beach, now let to a long-term tenant.", 9));
        save(apartment("Lakeview Studios – Kandy", KANDY, "Kandy", 45_000, true, FOR_RENT, 1, 1, 520, 3, true, true, 2,
                "Neat studio with a balcony facing the lake; pet-friendly with owner approval.", 10));
        save(apartment("Emerald Court – Kandy", KANDY, "Peradeniya", 7_400_000, false, SOLD, 2, 2, 940, 4, true, false, 55,
                "Two-bedroom apartment close to the university, sold to a local family.", 11));
        save(apartment("Sunrise Apartments – Jaffna", JAFFNA, "Jaffna", 11_500_000, false, PENDING, 3, 2, 1250, 2, false, false, 1,
                "New three-bedroom apartment near the city centre. Awaiting review.", 12));

        // ----- Houses -----
        save(house("Silver Palm Residence – Galle", GALLE, "Galle", 15_500_000, false, FOR_SALE, 4, 3, 2400, 12, true, true, 3,
                "Four-bedroom family home with a mature garden and a covered verandah, ten minutes from the Fort.", 1));
        save(house("Cinnamon Grove – Matara", MATARA, "Matara", 12_000_000, false, FOR_SALE, 3, 2, 1800, 10, true, false, 4,
                "Single-storey home in a quiet lane, with a cinnamon-tree garden and room to extend.", 2));
        save(house("Coral Bay Homes – Trincomalee", TRINCOMALEE, "Trincomalee", 9_900_000, false, FOR_SALE, 3, 2, 1500, 15, true, true, 6,
                "Three-bedroom house a short walk from the beach, with a walled compound.", 3));
        save(house("Rosewood Residence – Nugegoda", COLOMBO, "Nugegoda", 38_000_000, false, FOR_SALE, 4, 3, 2800, 10, true, true, 2,
                "Modern four-bedroom home close to schools, supermarkets and the Nugegoda junction.", 4));
        save(house("Lakeside Villa – Kandy", KANDY, "Kandy", 28_000_000, false, SOLD, 4, 3, 2500, 14, true, false, 35,
                "Hillside villa with lake views and a landscaped garden - sold.", 5));
        save(house("Golden Palm Villa – Hikkaduwa", GALLE, "Hikkaduwa", 180_000, true, FOR_RENT, 3, 3, 2200, 20, true, true, 2,
                "Furnished holiday villa with a pool, two minutes from the beach. Pets welcome.", 6));
        save(house("Hill Country Bungalow – Nuwara Eliya", NUWARA_ELIYA, "Nuwara Eliya", 21_000_000, false, FOR_SALE, 3, 2, 2000, 25, true, true, 7,
                "Colonial-style bungalow with a fireplace and a large rose garden.", 7));
        save(house("Family Home – Negombo", GAMPAHA, "Negombo", 17_500_000, false, SOLD, 4, 3, 2200, 12, true, false, 45,
                "Well-kept family home near the Negombo lagoon - sold.", 8));
        save(house("Coastal Cottage – Tangalle", HAMBANTOTA, "Tangalle", 120_000, true, RENTED, 2, 2, 1300, 18, true, true, 50,
                "Beachside cottage with a coconut garden, now rented long-term.", 9));
        save(house("Garden Residence – Kurunegala", KURUNEGALA, "Kurunegala", 14_500_000, false, FOR_SALE, 3, 2, 1900, 16, true, false, 9,
                "Spacious home on a corner plot near the town centre.", 10));
        save(house("Lotus Garden – Matara", MATARA, "Weligama", 19_000_000, false, SOLD, 4, 3, 2300, 12, true, true, 60,
                "Contemporary home a short drive from Weligama beach - sold.", 11));
        save(house("Modern Duplex – Maharagama", COLOMBO, "Maharagama", 42_000_000, false, PENDING, 4, 4, 3000, 8, false, false, 1,
                "Brand-new duplex with a rooftop terrace. Awaiting review.", 12));

        // ----- Land (price is PER PERCH) -----
        save(land("Sunview Block – Negombo", GAMPAHA, "Negombo", 650_000, FOR_SALE, 20, "Residential", 2,
                "Level residential plot with road frontage, close to Negombo town and the Colombo-Katunayake Expressway.", 1));
        save(land("Hillcrest Acres – Nuwara Eliya", NUWARA_ELIYA, "Nuwara Eliya", 480_000, FOR_SALE, 40, "Residential", 3,
                "Gently sloping plot with tea-country views, ideal for a holiday bungalow.", 2));
        save(land("Golden Acres – Anuradhapura", ANURADHAPURA, "Anuradhapura", 120_000, FOR_SALE, 80, "Agricultural", 4,
                "Fertile flat land with a nearby irrigation tank, suited to paddy or vegetables.", 3));
        save(land("Palm Grove Plot – Kalutara", KALUTARA, "Panadura", 380_000, FOR_SALE, 25, "Residential", 5,
                "Coconut-palm plot in a quiet lane, minutes from the Southern Expressway.", 4));
        save(land("Lagoon Side Block – Batticaloa", BATTICALOA, "Batticaloa", 150_000, FOR_SALE, 30, "Mixed", 6,
                "Mixed-use plot beside the lagoon road, with water and electricity connected.", 5));
        save(land("Tea Estate Plot – Badulla", BADULLA, "Bandarawela", 210_000, SOLD, 50, "Agricultural", 38,
                "Productive tea land with a small factory shed - sold.", 6));
        save(land("Beachfront Plot – Tangalle", HAMBANTOTA, "Tangalle", 900_000, SOLD, 16, "Commercial", 42,
                "Prime beachfront plot zoned for a small guesthouse - sold.", 7));
        save(land("Commercial Corner Lot – Kurunegala", KURUNEGALA, "Kurunegala", 750_000, FOR_SALE, 12, "Commercial", 7,
                "Corner lot on a busy main road, well suited to a shop or showroom.", 8));
        save(land("Rubber Land – Kegalle", KEGALLE, "Mawanella", 95_000, FOR_SALE, 100, "Agricultural", 8,
                "Established rubber plantation on a gentle slope with road access.", 9));
        save(land("Riverside Plot – Ratnapura", RATNAPURA, "Ratnapura", 110_000, FOR_SALE, 35, "Residential", 9,
                "Plot beside a quiet stream, a short drive from Ratnapura town.", 10));
        save(land("Sunrise Block – Gampaha", GAMPAHA, "Gampaha", 420_000, SOLD, 15, "Residential", 52,
                "Cleared residential block, sold to a local builder.", 11));
        save(land("Paddy Field – Polonnaruwa", POLONNARUWA, "Polonnaruwa", 60_000, PENDING, 200, "Agricultural", 1,
                "Large paddy field with irrigation access. Awaiting review.", 12));
    }

    private HouseProperty house(String title, District district, String city, double price, boolean rent,
                                ListingStatus status, int beds, int baths, double sqft, double perches,
                                boolean garden, boolean pets, int daysAgo, String description, int photoSeed) {
        HouseProperty p = new HouseProperty();
        fillCommon(p, title, district, city, price, rent, status, pets, daysAgo, description);
        p.setBedrooms(beds);
        p.setBathrooms(baths);
        p.setFloorAreaSqft(sqft);
        p.setLandSizePerches(perches);
        p.setHasGarden(garden);
        addPhotos(p, "house", photoSeed);
        return p;
    }

    private ApartmentProperty apartment(String title, District district, String city, double price, boolean rent,
                                        ListingStatus status, int beds, int baths, double sqft, int floor,
                                        boolean lift, boolean pets, int daysAgo, String description, int photoSeed) {
        ApartmentProperty p = new ApartmentProperty();
        fillCommon(p, title, district, city, price, rent, status, pets, daysAgo, description);
        p.setBedrooms(beds);
        p.setBathrooms(baths);
        p.setFloorAreaSqft(sqft);
        p.setFloorNumber(floor);
        p.setHasLift(lift);
        addPhotos(p, "apartment", photoSeed);
        return p;
    }

    private LandProperty land(String title, District district, String city, double pricePerPerch,
                              ListingStatus status, double perches, String zoning, int daysAgo,
                              String description, int photoSeed) {
        LandProperty p = new LandProperty();
        fillCommon(p, title, district, city, pricePerPerch, false, status, false, daysAgo, description);
        p.setAreaPerches(perches);
        p.setZoning(zoning);
        addPhotos(p, "land", photoSeed);
        return p;
    }

    private void fillCommon(Property p, String title, District district, String city, double price, boolean rent,
                            ListingStatus status, boolean pets, int daysAgo, String description) {
        p.setTitle(title);
        p.setDistrict(district);
        p.setCity(city);
        p.setPrice(price);
        p.setForRent(rent);
        p.setStatus(status);
        p.setPetFriendly(pets);
        p.setListingDate(LocalDate.now().minusDays(daysAgo));
        p.setDescription(description);
        p.setContactName(SALES_NAME);
        p.setContactPhone(SALES_PHONE);
        p.setContactEmail(SALES_EMAIL);
    }

    /**
     * Titles of the listings made by seedGalleryListings(). They deliberately have no photos, so
     * backfillMissingPhotos() skips them (it would otherwise attach sample photos on the next start).
     */
    private static final java.util.Set<String> GALLERY_LISTING_TITLES = java.util.Set.of(
            "Land Plot \u2013 Negombo", "Handover Day \u2013 Kurunegala", "Move-in Day \u2013 Ratnapura",
            // 15 extra made-up listings
            "Palm Haven Plot \u2013 Hambantota", "Misty Ridge Block \u2013 Badulla", "Lotus Pond Land \u2013 Matale",
            "Monsoon Meadows \u2013 Ampara", "Sapphire Fields \u2013 Puttalam",
            "Jasmine Court \u2013 Colombo 05", "Banyan Tree Villa \u2013 Kalutara", "Sandalwood Lane \u2013 Peradeniya",
            "Orchid Hill House \u2013 Ella", "Tamarind Cottage \u2013 Mannar",
            "Harbour Lights \u2013 Colombo 02", "Blue Lagoon Residences \u2013 Bentota", "Cloud Nine Towers \u2013 Colombo 06",
            "Garden City Flats \u2013 Gampaha", "Vista Rise \u2013 Jaffna");

    /**
     * Listings an earlier version of this seeder added by mistake: their names (Silver Palm, Coral Bay Homes,
     * Lakeside Villas, Cinnamon Grove, Sunview Block, Rosewood Residence, Golden Acres) already existed in
     * seedListings() with another town, so they showed up as duplicates. Removed on start-up.
     */
    private static final java.util.Set<String> MISTAKEN_DUPLICATE_TITLES = java.util.Set.of(
            "Silver Palm \u2013 Galle", "Coral Bay Homes \u2013 Matara", "Lakeside Villas \u2013 Kandy",
            "Cinnamon Grove \u2013 Kalutara", "Sunview Block \u2013 Anuradhapura",
            "Rosewood Residence \u2013 Colombo 07", "Golden Acres \u2013 Kurunegala");

    /** Deletes the mistaken duplicates above (only sample listings, never one a real user added). */
    private void removeMistakenDuplicates() {
        java.util.List<Property> toRemove = new java.util.ArrayList<>();
        for (Property p : propertyRepository.findAll()) {
            if (p.getOwner() == null && MISTAKEN_DUPLICATE_TITLES.contains(p.getTitle())
                    && p.getStatus() != ListingStatus.SOLD && p.getStatus() != ListingStatus.RENTED
                    && !isReferenced(p)) {
                toRemove.add(p);
            }
        }
        if (!toRemove.isEmpty()) {
            propertyRepository.deleteAll(toRemove);
            log.info("Removed {} duplicate sample listings added by an earlier version.", toRemove.size());
        }
    }

    /** True if a buyer saved this listing or asked for a viewing of it - deleting it would break those rows. */
    private boolean isReferenced(Property p) {
        return savedPropertyRepository.existsByPropertyId(p.getId())
                || viewingRequestRepository.existsByPropertyId(p.getId());
    }

    /** The 15 completed projects shown in the homepage gallery, in the same order as the gallery tiles. */
    private static final java.util.Set<String> COMPLETED_PROJECT_TITLES = java.util.Set.of(
            "Land Plot \u2013 Negombo", "Silver Palm \u2013 Galle", "Skyline Towers", "Handover Day",
            "Emerald Heights", "Coral Bay Homes", "Hillcrest Acres", "Lakeside Villas", "Cinnamon Grove",
            "Ocean View Apts", "Move-in Day", "Sunview Block", "Rosewood Residence", "Pearl Residencies",
            "Golden Acres");

    /**
     * Makes the Completed Projects page (and the Sold / Rented rows in the admin listings) match the 15 names in
     * the homepage gallery: each one is a sold or rented sample project that uses its gallery photo.
     * The older sample projects with other names are removed (never one a buyer saved or booked a viewing for),
     * and a gallery project is only added when it is not there yet, so restarting never duplicates anything.
     */
    private void seedCompletedProjects() {
        java.util.List<Property> toRemove = new java.util.ArrayList<>();
        java.util.Set<String> have = new java.util.HashSet<>();
        java.util.List<Property> all = new java.util.ArrayList<>(propertyRepository.findAll());
        all.sort(java.util.Comparator.comparing(Property::getId));
        for (Property p : all) {
            boolean completed = p.getStatus() == ListingStatus.SOLD || p.getStatus() == ListingStatus.RENTED;
            if (p.getOwner() != null || !completed) {
                continue;
            }
            boolean wanted = COMPLETED_PROJECT_TITLES.contains(p.getTitle());
            if (wanted && have.add(p.getTitle())) {
                continue;                       // the first copy of a gallery project stays
            }
            if (!isReferenced(p)) {
                toRemove.add(p);                // an old sample project, or a second copy
            } else if (wanted) {
                have.add(p.getTitle());
            }
        }
        if (!toRemove.isEmpty()) {
            propertyRepository.deleteAll(toRemove);
            log.info("Removed {} old or duplicate completed projects.", toRemove.size());
        }

        java.util.List<Property> toAdd = new java.util.ArrayList<>();
        toAdd.add(completed(galleryLand("Land Plot \u2013 Negombo", GAMPAHA, "Negombo", 380_000, 20, "Residential", 70,
                "Residential plot near the Negombo beach, sold with a clear deed."), ListingStatus.SOLD, "gallery1.png"));
        toAdd.add(completed(galleryHouse("Silver Palm \u2013 Galle", GALLE, "Galle", 36_500_000, false, ListingStatus.SOLD, 4, 3, 2800, 12, true, 85,
                "Four-bedroom house with a landscaped garden near the Galle Fort."), ListingStatus.SOLD, "gallery2.png"));
        toAdd.add(completed(galleryApartment("Skyline Towers", COLOMBO, "Colombo 03", 52_000_000, false, ListingStatus.SOLD, 3, 3, 1750, 18, true, false, 95,
                "High-floor apartment with city views, sold to a family relocating from abroad."), ListingStatus.SOLD, "gallery3.png"));
        toAdd.add(completed(galleryHouse("Handover Day", KURUNEGALA, "Kurunegala", 18_900_000, false, ListingStatus.SOLD, 3, 2, 1800, 10, true, 60,
                "New three-bedroom house handed over to its owners."), ListingStatus.SOLD, "gallery4.png"));
        toAdd.add(completed(galleryApartment("Emerald Heights", KANDY, "Kandy", 26_000_000, false, ListingStatus.SOLD, 3, 2, 1400, 9, true, false, 100,
                "Hill-view apartment on the edge of Kandy town."), ListingStatus.SOLD, "gallery5.png"));
        toAdd.add(completed(galleryHouse("Coral Bay Homes", MATARA, "Matara", 160_000, true, ListingStatus.RENTED, 3, 3, 2100, 9, false, 40,
                "Beach-side family home, rented on a two-year lease."), ListingStatus.RENTED, "gallery6.png"));
        toAdd.add(completed(galleryLand("Hillcrest Acres", NUWARA_ELIYA, "Nuwara Eliya", 150_000, 40, "Residential", 110,
                "Cool-climate hillside land with a valley view."), ListingStatus.SOLD, "gallery7.png"));
        toAdd.add(completed(galleryHouse("Lakeside Villas", KANDY, "Kandy", 42_000_000, false, ListingStatus.SOLD, 5, 4, 3400, 18, true, 120,
                "Lakeside villa with a pool deck and a guest wing."), ListingStatus.SOLD, "gallery8.png"));
        toAdd.add(completed(galleryLand("Cinnamon Grove", KALUTARA, "Kalutara", 275_000, 25, "Mixed", 130,
                "Flat plot in a quiet cinnamon-growing area."), ListingStatus.SOLD, "gallery9.png"));
        toAdd.add(completed(galleryApartment("Ocean View Apts", GALLE, "Galle", 95_000, true, ListingStatus.RENTED, 2, 2, 1100, 5, true, true, 45,
                "Sea-facing apartment, rented to a long-stay tenant."), ListingStatus.RENTED, "gallery10.jpg"));
        toAdd.add(completed(galleryHouse("Move-in Day", RATNAPURA, "Ratnapura", 22_000_000, false, ListingStatus.SOLD, 3, 2, 1650, 11, true, 75,
                "Newly painted three-bedroom house, ready to move in."), ListingStatus.SOLD, "gallery11.jpg"));
        toAdd.add(completed(galleryLand("Sunview Block", ANURADHAPURA, "Anuradhapura", 95_000, 30, "Residential", 140,
                "Level block in a growing neighbourhood."), ListingStatus.SOLD, "gallery12.png"));
        toAdd.add(completed(galleryHouse("Rosewood Residence", COLOMBO, "Colombo 07", 98_000_000, false, ListingStatus.SOLD, 5, 5, 4200, 15, true, 150,
                "Elegant residence in Cinnamon Gardens with a courtyard."), ListingStatus.SOLD, "gallery13.png"));
        toAdd.add(completed(galleryApartment("Pearl Residencies", COLOMBO, "Battaramulla", 27_500_000, false, ListingStatus.SOLD, 3, 2, 1380, 7, true, false, 160,
                "Modern apartment near the parliament road."), ListingStatus.SOLD, "gallery14.png"));
        toAdd.add(completed(galleryLand("Golden Acres", KURUNEGALA, "Kurunegala", 120_000, 60, "Agricultural", 170,
                "Large fertile block with a well and coconut trees."), ListingStatus.SOLD, "gallery15.jpg"));

        int added = 0;
        for (Property p : toAdd) {
            if (!have.contains(p.getTitle())) {
                propertyRepository.save(p);
                added++;
            }
        }
        if (added > 0) {
            log.info("Added {} completed projects from the homepage gallery.", added);
        }
    }

    /** Marks a sample listing as a finished project and gives it its gallery photo. */
    private Property completed(Property p, ListingStatus status, String galleryFile) {
        p.setStatus(status);
        p.addImage(new PropertyImage("/images/gallery/" + galleryFile));
        return p;
    }

    /**
     * Gives every sample listing that has not been opened yet a believable view count, so the eye counter on the
     * cards does not start at 0 everywhere. Real visits are added on top of this number (PropertyController).
     */
    private void backfillViewCounts() {
        for (Property p : propertyRepository.findAll()) {
            if (p.getOwner() == null && p.getViewCount() == 0
                    && p.getStatus() != ListingStatus.SOLD && p.getStatus() != ListingStatus.RENTED) {
                p.setViewCount(35 + (int) ((p.getId() * 137L) % 640));
                propertyRepository.save(p);
            }
        }
    }

    /**
     * Adds the gallery project names that did NOT exist as listings yet, with sample details and NO photos.
     * Runs on every start but only adds a listing whose title is not there yet, so nothing is duplicated.
     * The other twelve gallery names (Skyline Towers, Emerald Heights, Ocean View Apartments, Pearl Residencies,
     * Silver Palm, Coral Bay Homes, Lakeside Villas, Cinnamon Grove, Hillcrest Acres, Sunview Block,
     * Rosewood Residence and Golden Acres) are already in seedListings().
     */
    private void seedGalleryListings() {
        java.util.Set<String> existing = new java.util.HashSet<>();
        for (Property p : propertyRepository.findAll()) {
            // Only live listings count: a completed project may share a name with a live listing.
            if (p.getStatus() != ListingStatus.SOLD && p.getStatus() != ListingStatus.RENTED) {
                existing.add(p.getTitle());
            }
        }
        java.util.List<Property> toAdd = new java.util.ArrayList<>();

        toAdd.add(galleryLand("Land Plot \u2013 Negombo", GAMPAHA, "Negombo", 380_000, 20, "Residential", 3,
                "Cleared residential plot with road frontage, a short drive from the Negombo beach and the airport."));
        toAdd.add(galleryHouse("Handover Day \u2013 Kurunegala", KURUNEGALA, "Kurunegala", 18_900_000, false, FOR_SALE, 3, 2, 1800, 10, true, 4,
                "Brand-new three-bedroom house ready for handover, with a covered car porch and a small garden."));
        toAdd.add(galleryHouse("Move-in Day \u2013 Ratnapura", RATNAPURA, "Ratnapura", 140_000, true, FOR_RENT, 3, 2, 1650, 11, true, 8,
                "Newly painted three-bedroom house for rent, ready to move into. Pets welcome."));

        // ----- 15 more made-up listings (all invented; no photos) -----
        // Lands (price is per perch)
        toAdd.add(galleryLand("Palm Haven Plot \u2013 Hambantota", HAMBANTOTA, "Hambantota", 85_000, 35, "Residential", 1,
                "Flat coastal plot with coconut palms, ten minutes from the Hambantota harbour."));
        toAdd.add(galleryLand("Misty Ridge Block \u2013 Badulla", BADULLA, "Badulla", 140_000, 22, "Residential", 2,
                "Cool-climate hillside block with a mountain view and a gravel access road."));
        toAdd.add(galleryLand("Lotus Pond Land \u2013 Matale", MATALE, "Matale", 70_000, 48, "Agricultural", 5,
                "Fertile land beside a small pond, currently planted with spices."));
        toAdd.add(galleryLand("Monsoon Meadows \u2013 Ampara", AMPARA, "Ampara", 45_000, 120, "Agricultural", 7,
                "Wide open meadow with year-round water, good for paddy or a small farm."));
        toAdd.add(galleryLand("Sapphire Fields \u2013 Puttalam", PUTTALAM, "Puttalam", 110_000, 28, "Mixed", 11,
                "Corner plot near the new highway junction, suitable for a home or a small shop."));

        // Houses
        toAdd.add(galleryHouse("Jasmine Court \u2013 Colombo 05", COLOMBO, "Colombo 05", 74_000_000, false, FOR_SALE, 4, 4, 3100, 10, true, 1,
                "Contemporary four-bedroom house on a quiet lane in Narahenpita, with a rooftop terrace."));
        toAdd.add(galleryHouse("Banyan Tree Villa \u2013 Kalutara", KALUTARA, "Kalutara", 52_500_000, false, FOR_SALE, 4, 3, 2900, 20, true, 3,
                "Riverside villa shaded by an old banyan tree, with a boat jetty at the bottom of the garden."));
        toAdd.add(galleryHouse("Sandalwood Lane \u2013 Peradeniya", KANDY, "Peradeniya", 31_000_000, false, FOR_SALE, 3, 3, 2200, 14, true, 6,
                "Split-level family house close to the botanical gardens and the university."));
        toAdd.add(galleryHouse("Orchid Hill House \u2013 Ella", BADULLA, "Ella", 185_000, true, FOR_RENT, 2, 2, 1300, 8, true, 9,
                "Cosy hill house with a wooden deck facing Ella Rock. Furnished, ideal as a long-stay rental."));
        toAdd.add(galleryHouse("Tamarind Cottage \u2013 Mannar", MANNAR, "Mannar", 9_800_000, false, FOR_SALE, 2, 1, 1100, 16, true, 12,
                "Small single-storey cottage with a tamarind tree and a short walk to the sea."));

        // Apartments
        toAdd.add(galleryApartment("Harbour Lights \u2013 Colombo 02", COLOMBO, "Colombo 02", 48_000_000, false, FOR_SALE, 3, 3, 1800, 22, true, false, 2,
                "Upper-floor apartment overlooking the port and the Beira Lake, with a pool and a gym."));
        toAdd.add(galleryApartment("Blue Lagoon Residences \u2013 Bentota", GALLE, "Bentota", 21_500_000, false, FOR_SALE, 2, 2, 1250, 4, true, true, 4,
                "Beach-side holiday apartment with a shared pool, two minutes from the Bentota river."));
        toAdd.add(galleryApartment("Cloud Nine Towers \u2013 Colombo 06", COLOMBO, "Colombo 06", 38_000_000, false, FOR_SALE, 3, 2, 1500, 15, true, false, 6,
                "High-floor three-bedroom flat in Wellawatte with a balcony and 24-hour security."));
        toAdd.add(galleryApartment("Garden City Flats \u2013 Gampaha", GAMPAHA, "Gampaha", 85_000, true, FOR_RENT, 2, 1, 950, 2, false, true, 8,
                "Neat two-bedroom flat in a gated block near the Gampaha railway station."));
        toAdd.add(galleryApartment("Vista Rise \u2013 Jaffna", JAFFNA, "Jaffna", 15_600_000, false, FOR_SALE, 2, 2, 1050, 6, true, false, 10,
                "Modern apartment near the Jaffna fort, with city views and covered parking."));

        int added = 0;
        for (Property p : toAdd) {
            if (!existing.contains(p.getTitle())) {
                propertyRepository.save(p);
                added++;
            }
        }
        if (added > 0) {
            log.info("Added {} sample listings from the homepage gallery (no photos).", added);
        }
    }

    /** A land listing with no photos (the normal land(...) helper always adds some). */
    private LandProperty galleryLand(String title, District district, String city, double pricePerPerch, double perches,
                                     String zoning, int daysAgo, String description) {
        LandProperty p = new LandProperty();
        fillCommon(p, title, district, city, pricePerPerch, false, FOR_SALE, false, daysAgo, description);
        p.setAreaPerches(perches);
        p.setZoning(zoning);
        return p;
    }

    /** An apartment listing with no photos. */
    private ApartmentProperty galleryApartment(String title, District district, String city, double price, boolean rent,
                                               ListingStatus status, int beds, int baths, double sqft, int floor,
                                               boolean lift, boolean pets, int daysAgo, String description) {
        ApartmentProperty p = new ApartmentProperty();
        fillCommon(p, title, district, city, price, rent, status, pets, daysAgo, description);
        p.setBedrooms(beds);
        p.setBathrooms(baths);
        p.setFloorAreaSqft(sqft);
        p.setFloorNumber(floor);
        p.setHasLift(lift);
        return p;
    }

    /** A house listing with no photos. */
    private HouseProperty galleryHouse(String title, District district, String city, double price, boolean rent,
                                       ListingStatus status, int beds, int baths, double sqft, double perches,
                                       boolean garden, int daysAgo, String description) {
        HouseProperty p = new HouseProperty();
        fillCommon(p, title, district, city, price, rent, status, rent, daysAgo, description);
        p.setBedrooms(beds);
        p.setBathrooms(baths);
        p.setFloorAreaSqft(sqft);
        p.setLandSizePerches(perches);
        p.setHasGarden(garden);
        return p;
    }

    /**
     * Runs on every startup, not just when the database is empty - for a
     * dev database seeded before photos were wired up, this is what
     * attaches them without needing a DROP DATABASE. It only ever adds
     * photos to a sample listing that currently has none (owner == null
     * means it's one of this seeder's own listings, never something a real
     * user submitted through the site), so nothing you've added, edited
     * or uploaded yourself is ever touched. Relies on run() being
     * @Transactional (see above) for the session this needs.
     */
    private void backfillMissingPhotos() {
        for (Property p : propertyRepository.findAll()) {
            if (p.getOwner() == null && p.getImages().isEmpty() && !GALLERY_LISTING_TITLES.contains(p.getTitle())) {
                int seed = (int) (p.getId() == null ? 1 : p.getId());
                addPhotos(p, p.getTypeKey(), seed);
                propertyRepository.save(p);
            }
        }
    }

    /** Three photos per listing, picked round-robin from five bundled images per type. */
    private void addPhotos(Property p, String type, int seed) {
        for (int i = 0; i < 3; i++) {
            int number = ((seed + i - 1) % 5) + 1;
            String key = type + number;
            String extension = PHOTO_EXTENSIONS.getOrDefault(key, "png");
            p.addImage(new PropertyImage("/images/properties/" + key + "." + extension));
        }
    }

    private void save(Property property) {
        propertyRepository.save(property);
    }

    // ------------------------------------------------------------------
    // Local info - one entry for each of the 25 districts
    // ------------------------------------------------------------------

    private void seedLocalInfo() {
        info(COLOMBO, 5, 5, 5, 4, true,
                "Sri Lanka's commercial capital - the busiest, most expensive and best-connected part of the island.",
                "Home to many of the country's best-known national and international schools, plus several universities and institutes. Places at the top schools are very competitive.",
                "Colombo Fort is the hub for rail and long-distance buses, with frequent city buses and plenty of three-wheelers and ride-hailing. Expect heavy traffic at peak hours.",
                "Major public and private hospitals, large shopping malls, banks, restaurants and the Galle Face seafront.",
                "Busy and well lit, with police presence in the main commercial areas. Standard city precautions apply.",
                "Pet-friendly rentals exist but are mostly houses; many apartment buildings restrict pets or ask for owner approval and a deposit.");
        info(GAMPAHA, 4, 4, 4, 4, true,
                "A fast-growing suburban district north of Colombo, home to Bandaranaike International Airport and the coastal city of Negombo.",
                "A good mix of government and private schools, with the University of Kelaniya nearby; many families also commute to Colombo schools.",
                "The Colombo-Gampaha railway line, the Colombo-Katunayake Expressway and frequent buses make commuting to Colombo practical.",
                "Established towns with markets, supermarkets and hospitals; Negombo adds beaches, hotels and a busy fishing harbour.",
                "Mostly quiet residential areas; busier around Negombo and the airport corridor.",
                "Suburban houses with gardens are common, so pet-friendly rentals are easier to find here than in central Colombo.");
        info(KALUTARA, 3, 4, 3, 4, true,
                "A coastal district south of Colombo that mixes quiet residential towns with beach resorts.",
                "A reasonable choice of government and private schools; larger towns such as Panadura and Horana have the broadest options.",
                "The coastal railway line and Southern Expressway access make Colombo reachable; buses run frequently along Galle Road.",
                "Beach resorts around Beruwala and Wadduwa, local markets and growing retail in Panadura.",
                "Calm residential areas; the coastal road can be busy.",
                "Houses with gardens are common and owners are often flexible, but always ask before signing.");
        info(KANDY, 5, 4, 4, 5, true,
                "Sri Lanka's cultural capital in the central hills, with a cooler climate and a strong education tradition.",
                "Home to well-known schools such as Trinity College and Kingswood College, with the University of Peradeniya on the edge of the city.",
                "A major rail junction on the main line and the bus hub for the hill country; roads in town are narrow and congested at peak times.",
                "Kandy General Hospital, private hospitals, markets, the Temple of the Tooth and Kandy Lake.",
                "Family-friendly, with steady activity in the city centre and quieter hillside suburbs.",
                "Garden houses on the hillsides are often pet-friendly; apartments are fewer and rules vary.");
        info(MATALE, 3, 3, 3, 4, true,
                "A green district north of Kandy, known for spice gardens and the Sigiriya and Dambulla heritage sites.",
                "Government schools serve most towns, with a wider choice in Matale town and Dambulla.",
                "Bus routes link Matale to Kandy and Dambulla, and the A9 road passes through the district.",
                "Hospitals in Matale and Dambulla, the Dambulla economic centre and tourism around Sigiriya.",
                "Quiet and rural in most areas.",
                "Larger plots and rural homes make keeping pets easy; formal pet policies are rare.");
        info(NUWARA_ELIYA, 3, 3, 3, 4, true,
                "Sri Lanka's tea-country highlands, cool all year with misty mornings.",
                "Schools are concentrated in Nuwara Eliya town, Hatton and Talawakele; families sometimes look to Kandy for more choice.",
                "The main-line railway reaches Nanu Oya and the hill roads are scenic but slow; fog can delay journeys.",
                "Hospitals in Nuwara Eliya and Hatton, Gregory Lake, Victoria Park and Horton Plains.",
                "Quiet and safe; mountain roads need careful driving.",
                "The cool climate suits pets and many houses have gardens, but bungalows and furnished homes are priced at a premium.");
        info(GALLE, 4, 4, 4, 5, true,
                "A southern coastal district centred on the UNESCO-listed Galle Fort, with beaches such as Hikkaduwa and Unawatuna.",
                "Long-established schools such as Richmond College and Mahinda College, plus the Faculty of Medicine of the University of Ruhuna.",
                "The Southern Expressway cuts the trip to Colombo to roughly two hours, and the coastal railway runs through the district.",
                "Karapitiya Teaching Hospital, the Fort's cafes and shops, beaches and an active tourism sector.",
                "Tourist-friendly and relaxed; the usual beach-town awareness applies.",
                "Villas and holiday homes are often pet-friendly; rentals inside the Fort vary by landlord.");
        info(MATARA, 4, 4, 3, 4, true,
                "The southern coast's busy university town, close to Mirissa and Weligama.",
                "Well-known schools such as Rahula College and Sanghamitta Balika Vidyalaya, and the main campus of the University of Ruhuna.",
                "Matara is a terminus of the southern railway line, with Southern Expressway access to Colombo.",
                "Matara General Hospital, markets, beaches and the Star Fort.",
                "Calm and family-oriented.",
                "Houses with gardens are widely available; pet policies depend on the owner.");
        info(HAMBANTOTA, 3, 3, 3, 4, true,
                "A dry-zone district with a major port, Yala and Bundala national parks and long quiet beaches around Tangalle.",
                "Government schools serve the towns; options are fewer outside Tissamaharama and Hambantota town.",
                "The Southern Expressway extension and roads to the port and Mattala airport; public transport is less frequent in rural areas.",
                "Hospitals in Hambantota and Tangalle, and growing hotel and tourism infrastructure.",
                "Quiet; wildlife can be present near the national parks.",
                "Larger plots make pets easy, and beach rentals are often flexible.");
        info(JAFFNA, 4, 3, 3, 4, false,
                "The cultural centre of the Northern Province, with its own distinct Tamil heritage, food and architecture.",
                "A strong academic tradition - Jaffna Hindu College, St. John's College and Jaffna Central College - and the University of Jaffna.",
                "Reached by the A9 highway and the Northern railway line; local buses and three-wheelers cover the peninsula.",
                "Jaffna Teaching Hospital, a busy central market, the Jaffna library and the fort.",
                "Calm and community-oriented; infrastructure has improved steadily.",
                "Pet-friendly rentals are less commonly advertised; ask landlords directly.");
        info(KILINOCHCHI, 2, 3, 2, 4, false,
                "A rapidly developing town on the A9 highway, the gateway between Jaffna and the rest of the island.",
                "Government schools serve the district, and a University of Jaffna campus offers higher education.",
                "Direct A9 road and rail connections to Jaffna and Vavuniya.",
                "Growing but limited: a hospital, local markets and basic services.",
                "Quiet.",
                "Plenty of space, but few formally advertised pet-friendly rentals.");
        info(MANNAR, 2, 2, 2, 4, false,
                "A flat island district known for windmills, birdlife and the road towards Adam's Bridge.",
                "Government schools; limited choice beyond Mannar town.",
                "Road links to Vavuniya and Puttalam; buses are the main public transport.",
                "Mannar hospital, fishing harbours and pilgrim traffic to Madhu Church.",
                "Quiet.",
                "Open plots make pets easy; few formal pet policies.");
        info(VAVUNIYA, 3, 4, 3, 4, false,
                "The gateway to the north on the A9 highway, with a university campus and growing trade.",
                "Government schools and the University of Vavuniya.",
                "A rail and A9 road junction with frequent buses to Colombo, Anuradhapura and Jaffna.",
                "A general hospital and the services of a busy market town.",
                "Quiet.",
                "Moderate availability; ask landlords.");
        info(MULLAITIVU, 2, 2, 2, 4, false,
                "A coastal district in the north-east that is still rebuilding its infrastructure, with long empty beaches.",
                "Government schools; options are limited.",
                "Road links via Puthukudiyiruppu and Mankulam; public transport is sparse.",
                "Basic hospitals and markets; services are limited.",
                "Quiet.",
                "Rural homes with large plots; few formal rentals.");
        info(BATTICALOA, 4, 3, 3, 4, true,
                "A lagoon city on the east coast with calm beaches at Pasikudah and Kalkudah.",
                "Respected schools such as St. Michael's College, and the Eastern University campus at Vantharumoolai.",
                "Train and bus links to Colombo; flat roads suit bicycles and three-wheelers.",
                "Batticaloa Teaching Hospital, the fort, the lagoon, markets and beach tourism.",
                "Calm.",
                "Houses with compounds are common and often accept pets.");
        info(AMPARA, 3, 3, 3, 4, true,
                "A large eastern district with paddy lands, Gal Oya National Park and surf at Arugam Bay.",
                "Government schools and the South Eastern University at Oluvil.",
                "Bus routes to Batticaloa, Monaragala and Colombo; journeys are long.",
                "Hospitals in Ampara and Kalmunai, farming markets and surf tourism.",
                "Quiet.",
                "Rural homes with space for pets.");
        info(TRINCOMALEE, 3, 3, 3, 4, true,
                "A natural-harbour city on the north-east coast with beaches at Nilaveli and Uppuveli.",
                "Government schools in the town and the Trincomalee Campus of the Eastern University.",
                "Buses and rail link to Colombo and Anuradhapura; the port area is busy.",
                "Trincomalee hospital, Koneswaram temple, Fort Frederick, whale-watching and diving.",
                "Tourist areas are calm.",
                "Beachside rentals are often flexible.");
        info(KURUNEGALA, 4, 4, 4, 4, true,
                "A major hub in the North Western Province, gateway to the coconut triangle.",
                "Well-known schools such as Maliyadeva College and Holy Family Convent.",
                "On the main road and rail routes towards Kandy, Anuradhapura and Jaffna, with the Central Expressway shortening trips to Colombo.",
                "Kurunegala Teaching Hospital, markets, malls and the Ethagala (Elephant Rock) viewpoint.",
                "Busy but generally safe.",
                "Suburban houses with gardens are common.");
        info(PUTTALAM, 3, 3, 3, 4, true,
                "A lagoon and coastal district known for salt pans, kitesurfing at Kalpitiya and Wilpattu National Park.",
                "Government schools in Puttalam and Chilaw; options are fewer in rural areas.",
                "The coastal railway and main road to Colombo; buses link the towns.",
                "Chilaw hospital, fishing harbours and beach tourism.",
                "Quiet.",
                "Coastal houses with compounds often allow pets.");
        info(ANURADHAPURA, 3, 3, 3, 4, true,
                "Sri Lanka's ancient capital and a UNESCO World Heritage city in the dry zone.",
                "Established schools such as Anuradhapura Central College, and Rajarata University at Mihintale.",
                "Northern railway line and buses to Colombo, Kandy and Jaffna; flat roads suit bicycles.",
                "Anuradhapura Teaching Hospital, sacred sites, ancient tanks (reservoirs) and large markets.",
                "Quiet and pilgrim-friendly.",
                "Houses with large compounds are common; pets are usually fine.");
        info(POLONNARUWA, 3, 3, 3, 4, true,
                "A medieval capital and UNESCO World Heritage site beside the vast Parakrama Samudra reservoir.",
                "Government schools in Polonnaruwa and Kaduruwela; options are limited elsewhere.",
                "Buses and rail from Colombo and Batticaloa; the roads are flat and straight.",
                "A hospital, markets, and Minneriya National Park with its elephant gatherings.",
                "Quiet.",
                "Rural plots where pets are common.");
        info(BADULLA, 3, 3, 3, 5, true,
                "A hill district on the main railway line, with the tea-country scenery of Ella, Haputale and Bandarawela.",
                "Government schools in Badulla and Bandarawela, and Uva Wellassa University.",
                "The scenic main-line railway ends at Badulla; roads are winding.",
                "Badulla and Bandarawela hospitals, Dunhinda Falls, and Ella's cafes and hiking.",
                "Calm; mountain roads need caution.",
                "Cool-climate houses and bungalows often accept pets.");
        info(MONARAGALA, 2, 2, 2, 4, true,
                "A large, thinly populated district of farmland and dry forest, next to the Yala and Udawalawe parks.",
                "Government schools; fewer choices outside Monaragala and Wellawaya.",
                "Buses to Badulla, Ampara and Colombo; services are less frequent.",
                "Basic hospitals and markets; farming and sugar cane dominate the economy.",
                "Quiet; wildlife can wander into rural areas.",
                "Wide plots make pets easy.");
        info(RATNAPURA, 3, 3, 3, 4, true,
                "The gem capital of Sri Lanka, set among rainforest and rubber estates with heavy rainfall.",
                "Government schools and Sabaragamuwa University at Belihuloya.",
                "Roads to Colombo via Avissawella and Embilipitiya; there is no railway to the city.",
                "Ratnapura Teaching Hospital, gem markets, Sinharaja forest and access to Adam's Peak.",
                "Generally calm; ask about flood and landslide risk for low-lying or steep plots.",
                "Houses with compounds make pets easy.");
        info(KEGALLE, 3, 4, 3, 4, true,
                "A green district between Colombo and Kandy, home to the Pinnawala Elephant Orphanage and tea and rubber lands.",
                "Government schools in Kegalle and Mawanella; many families also use Kandy schools.",
                "On the Colombo-Kandy road and rail corridor, so both cities are easy to reach.",
                "Kegalle hospital, markets, the elephant orphanage and tourist stops.",
                "Calm; ask about landslide risk on steep plots.",
                "Hillside houses with gardens are common.");
    }

    private void info(District district, int schools, int transport, int amenities, int safety, boolean petsCommon,
                      String summary, String schoolText, String transportText, String amenitiesText,
                      String safetyText, String petText) {
        LocalInfo info = new LocalInfo();
        info.setDistrict(district);
        info.setSummary(summary);
        info.setSchoolInfo(schoolText);
        info.setTransportInfo(transportText);
        info.setAmenitiesInfo(amenitiesText);
        info.setSafetyInfo(safetyText);
        info.setPetFriendlyRentalsCommon(petsCommon);
        info.setPetFriendlyNotes(petText);
        info.setSchoolRating(schools);
        info.setTransportRating(transport);
        info.setAmenitiesRating(amenities);
        info.setSafetyRating(safety);
        localInfoRepository.save(info);
    }
}
