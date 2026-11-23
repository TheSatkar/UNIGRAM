package com.unigram.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain entity representing a Chitkara student marketplace listing.
 */
public class Listing {

    private long id;
    private String sellerId;
    private String title;
    private String categoryId;
    private String subcategoryId;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String itemCondition;
    private String meetupSpot;
    private String description;
    private boolean hasNotes;
    private String badge;
    private String status; // "pending", "approved", "rejected", "sold"
    private String pendingType; // "new", "edit", null
    private String sellerName;
    private String pendingEdit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> photos = new ArrayList<>();

    public Listing() {
        this.status = "pending";
        this.pendingType = "new";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Listing(long id, String sellerId, String title, String categoryId, BigDecimal price,
                   String itemCondition, String meetupSpot, String description) {
        this();
        this.id = id;
        this.sellerId = sellerId;
        this.title = title;
        this.categoryId = categoryId;
        this.price = price;
        this.itemCondition = itemCondition;
        this.meetupSpot = meetupSpot;
        this.description = description;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getSubcategoryId() { return subcategoryId; }
    public void setSubcategoryId(String subcategoryId) { this.subcategoryId = subcategoryId; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }

    public String getItemCondition() { return itemCondition; }
    public void setItemCondition(String itemCondition) { this.itemCondition = itemCondition; }

    public String getMeetupSpot() { return meetupSpot; }
    public void setMeetupSpot(String meetupSpot) { this.meetupSpot = meetupSpot; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isHasNotes() { return hasNotes; }
    public void setHasNotes(boolean hasNotes) { this.hasNotes = hasNotes; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPendingType() { return pendingType; }
    public void setPendingType(String pendingType) { this.pendingType = pendingType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getPendingEdit() { return pendingEdit; }
    public void setPendingEdit(String pendingEdit) { this.pendingEdit = pendingEdit; }

    public List<String> getPhotos() { return photos; }
    public void setPhotos(List<String> photos) { this.photos = photos != null ? photos : new ArrayList<>(); }

    public static List<Listing> createSeedCatalog() {
        List<Listing> list = new ArrayList<>(40);

        // 1. books-notes
        list.add(createSeedItem(1, "student_a", "Data Structures & Algorithms in Java (4th Ed)", "books-notes", "engineering-textbooks",
                "550.00", "1299.00", "Like New", "Library Entrance",
                "Standard DSA textbook with detailed code snippets and practice questions. Clean pages with no marks.", "Academic Pick", false,
                "/assets/products/calculus.jpg"));
        list.add(createSeedItem(2, "student_c", "Discrete Mathematics & Its Applications (Rosen)", "books-notes", "engineering-textbooks",
                "420.00", "950.00", "Good", "Library Entrance",
                "Kenneth Rosen 8th Edition discrete structures book. Great condition with handwritten summary cheat sheets.", "Exam Essential", true,
                "/assets/products/organic_chem.jpg"));
        list.add(createSeedItem(3, "student_b", "Calculus: Early Transcendentals (Stewart)", "books-notes", "engineering-textbooks",
                "480.00", "1499.00", "Like New", "Library Entrance",
                "Single Variable Calculus textbook with limits, derivatives & integrals. Clean condition.", "Academic Pick", false,
                "/assets/products/calculus.jpg"));
        list.add(createSeedItem(4, "student_e", "GATE CS 2026 Solved Papers & Notes", "books-notes", "competitive-exam-prep",
                "350.00", "800.00", "Good", "Square One",
                "12 years solved GATE CSE question papers with detailed answer keys and handwritten revision notes.", "GATE Ready", true,
                "/assets/products/organic_chem.jpg"));

        // 2. electronics-gadgets
        list.add(createSeedItem(5, "student_a", "Logitech MX Master 2S Wireless Mouse", "electronics-gadgets", "keyboards-mice",
                "1850.00", "3995.00", "Excellent", "Turing Block",
                "Ergonomic wireless mouse with hyper-fast scroll wheel and multi-device Bluetooth pairing. Battery holds charge for weeks.", "Verified Working", false,
                "/assets/products/graphing_calc.jpg"));
        list.add(createSeedItem(6, "student_f", "Boat Stone 650 10W Bluetooth Speaker", "electronics-gadgets", "audio-earphones-speakers",
                "799.00", "1990.00", "Like New", "Hostel Gate",
                "Rugged waterproof Bluetooth speaker with deep bass and aux input. Used only a couple times in hostel room.", "Hostel Favorite", false,
                "/assets/products/arduino_kit.jpg"));
        list.add(createSeedItem(7, "student_b", "Apple 20W USB-C Power Adapter + Cable", "electronics-gadgets", "chargers-cables",
                "990.00", "1900.00", "Like New", "Square One",
                "Original Apple 20W fast charging brick with 1m braided USB-C to Lightning cable. Perfect working order.", "Original Apple", false,
                "/assets/products/graphing_calc.jpg"));
        list.add(createSeedItem(8, "student_g", "SanDisk Ultra 128GB Dual Drive Type-C", "electronics-gadgets", "storage-pendrives-hdds",
                "550.00", "1200.00", "Brand New", "Library Entrance",
                "High-speed OTG USB 3.1 pendrive with both Type-C and Type-A connectors for easy phone and laptop transfers.", "Unopened", false,
                "/assets/products/arduino_kit.jpg"));

        // 3. lab-engineering-gear
        list.add(createSeedItem(9, "student_d", "Arduino Uno R3 Starter Kit with 30+ Sensors", "lab-engineering-gear", "arduino-robotics-kits",
                "1150.00", "2400.00", "Like New", "Turing Block",
                "Original ATmega328P Arduino board, breadboard, OLED display, ultrasonic sensors, motors and resistors. Tested in Turing lab.", "Lab Ready", false,
                "/assets/products/arduino_kit.jpg"));
        list.add(createSeedItem(10, "student_h", "Standard Student Lab Coat (Size L, Unisex)", "lab-engineering-gear", "lab-coats-safety-gear",
                "250.00", "550.00", "Good", "Picasso Block",
                "White 100% cotton lab coat required for chemistry and physics practicals. Washed and sanitized.", "Mandatory Gear", false,
                "/assets/products/tote_bag.jpg"));
        list.add(createSeedItem(11, "student_d", "Casio fx-991EX ClassWiz Scientific Calculator", "lab-engineering-gear", "scientific-calculators",
                "850.00", "1595.00", "Excellent", "Turing Block",
                "High-resolution solar scientific calculator with matrix and equation solver. Allowed in university examinations.", "Exam Approved", false,
                "/assets/products/graphing_calc.jpg"));
        list.add(createSeedItem(12, "student_d", "Omega Mini Drafter & Engineering Drawing Set", "lab-engineering-gear", "drafters-drawing-kits",
                "390.00", "850.00", "Like New", "Edison Block",
                "Mini drafter with scale, clamps, sheet clips, compass and sheet container for engineering graphics lab.", "First Year Must", false,
                "/assets/products/arduino_kit.jpg"));

        // 4. stationery-supplies
        list.add(createSeedItem(13, "student_h", "Classmate Spiral Notebook Bundle (Pack of 5)", "stationery-supplies", "notebooks-registers",
                "220.00", "400.00", "Brand New", "Square One",
                "Unused 300-page single line ruled spiral bound registers with thick smooth pages for semester note-taking.", "Value Pack", false,
                "/assets/products/calculus.jpg"));
        list.add(createSeedItem(14, "student_g", "Uni-ball Eye Rollerball Pens (Set of 6)", "stationery-supplies", "pens-writing",
                "180.00", "360.00", "Brand New", "Food Court",
                "Smooth waterproof pigment ink roller pens. Brand new sealed pack of 3 blue and 3 black pens.", "Smooth Writer", false,
                "/assets/products/solid_perfume.jpg"));
        list.add(createSeedItem(15, "student_c", "Camel Acrylic Color Tubes (12 Shades) + Brushes", "stationery-supplies", "art-supplies",
                "280.00", "520.00", "Like New", "Picasso Block",
                "Set of 12 rich pigment acrylic colors with 4 synthetic brushes. Only 2 tubes opened for a college poster project.", "Creative Pick", false,
                "/assets/products/matcha_mug.jpg"));
        list.add(createSeedItem(16, "student_f", "Expanding Document File Folder (13 Pockets)", "stationery-supplies", "files-folders",
                "150.00", "320.00", "Like New", "Library Entrance",
                "Heavy duty accordion style organizer folder with colored divider tabs for keeping semester handouts safe.", "Organized", false,
                "/assets/products/tote_bag.jpg"));

        // 5. hostel-room-essentials
        list.add(createSeedItem(17, "student_e", "Prestige 1.5L Electric Stainless Steel Kettle", "hostel-room-essentials", "kitchen-kettle-induction",
                "550.00", "1195.00", "Excellent", "Hostel Gate",
                "Fast boiling electric kettle with auto shut-off. Perfect for late-night Maggi, coffee, and green tea in hostel.", "Hostel Essential", false,
                "/assets/products/room_spray.jpg"));
        list.add(createSeedItem(18, "student_i", "Rechargeable LED Desk Study Lamp", "hostel-room-essentials", "study-lamp-electricals",
                "320.00", "699.00", "Like New", "Hostel Gate",
                "Touch sensor 3 brightness dimmable study lamp with flexible gooseneck and USB charging port.", "Night Study", false,
                "/assets/products/graphing_calc.jpg"));
        list.add(createSeedItem(19, "student_j", "Havells Personal Table Fan (White/Blue)", "hostel-room-essentials", "cooling-heating",
                "650.00", "1400.00", "Good", "Hostel Gate",
                "Compact quiet table fan with 3 speed settings. Lightweight and fits easily on hostel study desk.", "Room Cooler", false,
                "/assets/products/arduino_kit.jpg"));
        list.add(createSeedItem(20, "student_j", "Stackable 3-Tier Storage Drawer Organizer", "hostel-room-essentials", "storage-organisers",
                "380.00", "799.00", "Like New", "Square One",
                "Durable modular storage cabinet for toiletries, snacks, medicines, and cables. Keeps hostel desk tidy.", "Space Saver", false,
                "/assets/products/matcha_mug.jpg"));

        // 6. fashion-accessories
        list.add(createSeedItem(21, "student_h", "Casio Vintage Digital Watch (A168WA)", "fashion-accessories", "watches-jewellery",
                "950.00", "1895.00", "Like New", "Square One",
                "Classic retro stainless steel digital watch with Illuminator backlight, stopwatch, and water resistance.", "Vintage Classic", false,
                "/assets/products/graphing_calc.jpg"));
        list.add(createSeedItem(22, "student_f", "Chitkara CSE Official Pullover Hoodie (Size L)", "fashion-accessories", "college-merch-hoodies",
                "599.00", "1200.00", "Like New", "Turing Block",
                "Warm fleece black campus hoodie with embroidered university crest. Barely worn during previous winter fest.", "Official Merch", false,
                "/assets/products/tote_bag.jpg"));
        list.add(createSeedItem(23, "student_j", "Wildcraft 35L Water-Resistant Laptop Backpack", "fashion-accessories", "bags-backpacks",
                "750.00", "1699.00", "Good", "Library Entrance",
                "Sturdy laptop college backpack with rain cover, ergonomic shoulder padding, and dedicated 15.6 inch laptop sleeve.", "Campus Daily", false,
                "/assets/products/tote_bag.jpg"));
        list.add(createSeedItem(24, "student_k", "Fastrack Aviator Sunglasses with Hard Case", "fashion-accessories", "watches-jewellery",
                "490.00", "1199.00", "Like New", "Food Court",
                "100% UV protected metal frame aviator shades with polarized lenses and scratch-free carrying case.", "UV Protected", false,
                "/assets/products/solid_perfume.jpg"));

        // 7. perfumes-grooming
        list.add(createSeedItem(25, "student_g", "Solid Amber & Sandalwood Cologne (15g Pocket Tin)", "perfumes-grooming", "perfumes-deos",
                "249.00", "450.00", "Brand New", "Hostel Gate",
                "Alcohol-free travel solid cologne in a sliding matte metal tin. Warm Mysore sandalwood notes blended with amber.", "Pocket Cologne", false,
                "/assets/products/solid_perfume.jpg"));
        list.add(createSeedItem(26, "student_g", "Beardo Godfather Hair Wax + Beard Wash Combo", "perfumes-grooming", "grooming-kits",
                "290.00", "650.00", "Brand New", "Square One",
                "Strong hold matte finish hair clay wax and refreshing aloe beard cleanser. Unopened sealed packaging.", "Grooming Pack", false,
                "/assets/products/room_spray.jpg"));
        list.add(createSeedItem(27, "student_e", "Botanical Room & Body Mist: Cedar & Lavender (50ml)", "perfumes-grooming", "perfumes-deos",
                "299.00", "499.00", "Freshly Crafted", "Food Court",
                "Includes: Natural Essential Oils. Freshly crafted hand-blended botanical fragrance for calming study vibes.", "Organic", false,
                "/assets/products/room_spray.jpg"));
        list.add(createSeedItem(28, "student_k", "The Ordinary Niacinamide 10% + Zinc 1% (30ml)", "perfumes-grooming", "skincare",
                "380.00", "600.00", "Brand New", "Library Entrance",
                "Original authentic serum for blemish control and sebum reduction. Sealed dropper bottle.", "Sealed Bottle", false,
                "/assets/products/solid_perfume.jpg"));

        // 8. handmade-art
        list.add(createSeedItem(29, "student_c", "Hand-Carved Wooden Pen Stand & Mobile Dock", "handmade-art", "custom-gifts",
                "280.00", "550.00", "Handmade", "Picasso Block",
                "Artisan polished sheesham wood pen organizer with integrated smartphone viewing notch. Handcrafted on campus.", "Campus Artisan", false,
                "/assets/products/matcha_mug.jpg"));
        list.add(createSeedItem(30, "student_j", "Minimalist Campus Skyline Charcoal Sketch (Framed)", "handmade-art", "paintings-sketches",
                "450.00", "900.00", "Brand New", "Square One",
                "Original black and white charcoal drawing of Chitkara university clock tower in a clean black wooden frame (A4).", "Original Art", false,
                "/assets/products/tote_bag.jpg"));
        list.add(createSeedItem(31, "student_c", "Handcrafted Matcha Green Ceramic Coffee Mug", "handmade-art", "decor",
                "350.00", "699.00", "Handmade", "Square One",
                "Hand-thrown ceramic pottery mug with earthy green glaze finish. Perfect for matcha, chai, or desk decor.", "Student Artisan", false,
                "/assets/products/matcha_mug.jpg"));
        list.add(createSeedItem(32, "student_f", "Handmade Daisy Floral Crochet Tote Bag", "handmade-art", "crochet-knitting",
                "380.00", "750.00", "Handmade", "Gate No. 4",
                "Custom crochet shoulder tote bag knit with premium soft milk cotton yarn in pastel cream and yellow daisy pattern.", "Hand Knitted", false,
                "/assets/products/tote_bag.jpg"));

        // 9. sports-fitness-cycles
        list.add(createSeedItem(33, "student_i", "Yonex Muscle Power 29 Light Badminton Racket", "sports-fitness-cycles", "racket-sports",
                "850.00", "1790.00", "Good", "Sports Complex",
                "Lightweight isometric badminton racket strung at 24 lbs with new grip tape and full head cover. Great smash power.", "Tournament Ready", false,
                "/assets/products/arduino_kit.jpg"));
        list.add(createSeedItem(34, "student_i", "Rubber Coated Hex Dumbbells (5kg Pair)", "sports-fitness-cycles", "gym-fitness",
                "650.00", "1300.00", "Good", "Hostel Gate",
                "Solid cast iron hexagonal dumbbells with non-slip chrome knurled handles. Floor friendly rubber coating.", "Hostel Workout", false,
                "/assets/products/graphing_calc.jpg"));
        list.add(createSeedItem(35, "student_i", "Nivia Storm Football (Size 5, Rubber)", "sports-fitness-cycles", "football-basketball",
                "390.00", "750.00", "Like New", "Sports Complex",
                "All-weather durable rubber moulded football suitable for turf and grass grounds. Includes inflation pin.", "Match Ready", false,
                "/assets/products/solid_perfume.jpg"));
        list.add(createSeedItem(36, "student_l", "Anti-Skid High Density 6mm Yoga Mat with Strap", "sports-fitness-cycles", "yoga",
                "420.00", "999.00", "Like New", "Square One",
                "Eco-friendly textured EVA foam fitness mat with excellent cushioning and dual-sided grip. Includes carry strap.", "Fitness Pick", false,
                "/assets/products/tote_bag.jpg"));

        // 10. other
        list.add(createSeedItem(37, "student_h", "Heavy Duty 4-Digit Combination Cable Bike Lock", "other", "bicycles-accessories",
                "190.00", "450.00", "Like New", "Hostel Gate",
                "Resettable combination steel cable lock for securing bicycles and hostel storage trunks. Weatherproof vinyl coating.", "Campus Security", false,
                "/assets/products/arduino_kit.jpg"));
        list.add(createSeedItem(38, "student_d", "Foldable Laptop Bed Table with Cup Holder", "other", null,
                "360.00", "750.00", "Good", "Hostel Gate",
                "Engineered wood portable desk tray with non-slip metal legs and tablet slot. Perfect for studying from hostel bed.", "Study Comfort", false,
                "/assets/products/matcha_mug.jpg"));
        list.add(createSeedItem(39, "student_e", "Compact 3-Fold UV Resistant Rain Umbrella", "other", null,
                "220.00", "499.00", "Like New", "Square One",
                "Windproof lightweight folding umbrella with black UV sun blocking coating. Fits easily inside college backpack.", "Monsoon Ready", false,
                "/assets/products/room_spray.jpg"));
        list.add(createSeedItem(40, "student_l", "Noise-Cancelling Memory Foam Earplugs (3 Pairs)", "other", null,
                "120.00", "280.00", "Brand New", "Library Entrance",
                "High SNR 38dB slow rebound foam earplugs in compact hygiene cases. Ideal for exam revision in busy hostel rooms.", "Exam Focus", false,
                "/assets/products/solid_perfume.jpg"));

        return list;
    }

    public static String getSellerNameForId(String sellerId) {
        if (sellerId == null) return "Campus Student";
        return switch (sellerId) {
            case "student_a" -> "Satkar Singh";
            case "student_b" -> "Priya K.";
            case "student_c" -> "Nisha R.";
            case "student_d" -> "Madhur T.";
            case "student_e" -> "Charlie W.";
            case "student_f" -> "Simran D.";
            case "student_g" -> "Harsh P.";
            case "student_h" -> "Aman V.";
            case "student_i" -> "Rohan S.";
            case "student_j" -> "Kavya M.";
            case "student_k" -> "Ananya G.";
            case "student_l" -> "Vikram B.";
            case "admin_1" -> "Campus Admin";
            default -> "Campus Student";
        };
    }

    private static Listing createSeedItem(long id, String sellerId, String title, String categoryId, String subcategoryId,
                                         String price, String origPrice, String condition, String spot,
                                         String desc, String badge, boolean hasNotes, String photoUrl) {
        Listing l = new Listing(id, sellerId, title, categoryId, new BigDecimal(price), condition, spot, desc);
        if (origPrice != null) l.setOriginalPrice(new BigDecimal(origPrice));
        l.setSubcategoryId(subcategoryId);
        l.setBadge(badge);
        l.setHasNotes(hasNotes);
        l.setStatus("approved");
        l.setPendingType(null);
        l.setSellerName(getSellerNameForId(sellerId));
        if (photoUrl != null) {
            l.setPhotos(new ArrayList<>(List.of(photoUrl)));
        }
        return l;
    }
}
