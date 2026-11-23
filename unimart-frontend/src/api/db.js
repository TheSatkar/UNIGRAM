/**
 * LocalStorage Fake Database simulating MySQL tables with delay.
 * Tables:
 * - users (id, name, email, student_id, password_hash, role, created_at)
 * - listings (id, seller_id, title, category_id, subcategory_id, price, original_price, item_condition, meetup_spot, description, has_notes, status, pending_type, created_at, updated_at)
 * - listing_photos (id, listing_id, url, sort_order)
 * - listing_pending_edits (id, listing_id, snapshot_json, created_at)
 * - wishlist (user_id, listing_id, created_at)
 * - conversations (id, listing_id, buyer_id, seller_id, created_at)
 * - messages (id, conversation_id, sender_id, body, created_at)
 */

import imgCalcBook from '../assets/products/calculus.jpg';
import imgMatchaMug from '../assets/products/matcha_mug.jpg';
import imgGraphCalc from '../assets/products/graphing_calc.jpg';
import imgRoomSpray from '../assets/products/room_spray.jpg';
import imgOrgChem from '../assets/products/organic_chem.jpg';
import imgToteBag from '../assets/products/tote_bag.jpg';
import imgArduinoKit from '../assets/products/arduino_kit.jpg';
import imgSolidPerfume from '../assets/products/solid_perfume.jpg';

const DB_PREFIX = 'unimart_db_';
export const DB_VERSION = 'v7';

// In-memory fallback if localStorage throws in private / restricted browsing
const memoryStore = {};

export const delay = (ms = 300) => new Promise((resolve) => setTimeout(resolve, ms));

export function getTable(tableName) {
  const key = `${DB_PREFIX}${tableName}_${DB_VERSION}`;
  try {
    const raw = localStorage.getItem(key);
    if (raw) return JSON.parse(raw);
  } catch (_e) {
    if (memoryStore[key]) return memoryStore[key];
  }
  return null;
}

export function setTable(tableName, data) {
  const key = `${DB_PREFIX}${tableName}_${DB_VERSION}`;
  try {
    localStorage.setItem(key, JSON.stringify(data));
  } catch (err) {
    // Fallback for private browsing or quota limits
    memoryStore[key] = data;
    console.error(`[UniMart Storage] Failed to persist table ${tableName} to localStorage:`, err);
    if (typeof window !== 'undefined' && (err?.name === 'QuotaExceededError' || err?.code === 22)) {
      window.dispatchEvent(new CustomEvent('unimart:storage-quota-exceeded', { detail: { table: tableName } }));
    }
  }
}

// Initial Seed Data
const SEED_USERS = [
  {
    id: 'admin_1',
    name: 'Campus Admin',
    email: 'admin@chitkara.edu.in',
    student_id: 'ADMIN-001',
    password_hash: 'Unimart@123',
    role: 'admin',
    department: null,
    created_at: new Date('2026-01-01').toISOString()
  },
  {
    id: 'student_a',
    name: 'Satkar Singh',
    email: 'satkar0696.becse25@chitkara.edu.in',
    student_id: '2110990696',
    password_hash: 'student123',
    role: 'student',
    department: null,
    created_at: new Date('2026-01-15').toISOString()
  },
  {
    id: 'student_b',
    name: 'Priya K.',
    email: 'priya.k@chitkara.edu.in',
    student_id: '2110990697',
    password_hash: 'student123',
    role: 'student',
    department: 'Math Dept',
    created_at: new Date('2026-01-20').toISOString()
  },
  {
    id: 'student_c',
    name: 'Nisha R.',
    email: 'nisha.r@chitkara.edu.in',
    student_id: '2110990698',
    password_hash: 'student123',
    role: 'student',
    department: 'Fine Arts',
    created_at: new Date('2026-01-21').toISOString()
  },
  {
    id: 'student_d',
    name: 'Madhur T.',
    email: 'madhur.t@chitkara.edu.in',
    student_id: '2110990699',
    password_hash: 'student123',
    role: 'student',
    department: 'Engineering',
    created_at: new Date('2026-01-22').toISOString()
  },
  {
    id: 'student_e',
    name: 'Charlie W.',
    email: 'charlie.w@chitkara.edu.in',
    student_id: '2110990700',
    password_hash: 'student123',
    role: 'student',
    department: 'Biochem Maker',
    created_at: new Date('2026-01-23').toISOString()
  },
  {
    id: 'student_f',
    name: 'Simran D.',
    email: 'simran.d@chitkara.edu.in',
    student_id: '2110990701',
    password_hash: 'student123',
    role: 'student',
    department: 'Design Club',
    created_at: new Date('2026-01-24').toISOString()
  },
  {
    id: 'student_g',
    name: 'Harsh P.',
    email: 'harsh.p@chitkara.edu.in',
    student_id: '2110990702',
    password_hash: 'student123',
    role: 'student',
    department: 'Maker Collective',
    created_at: new Date('2026-01-25').toISOString()
  }
];

const SEED_LISTINGS = [
  // 1. books-notes (2 Satkar, 2 Priya)
  {
    id: 1,
    seller_id: 'student_a',
    title: 'Data Structures & Algorithms in Java (4th Ed)',
    category_id: 'books-notes',
    subcategory_id: 'engineering-textbooks',
    price: 550,
    original_price: 1299,
    item_condition: 'Like New',
    meetup_spot: 'Library Entrance',
    description: 'Standard DSA textbook with detailed code snippets and practice questions. Clean pages with no marks.',
    has_notes: false,
    badge: 'Academic Pick',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:40Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:40Z').toISOString()
  },
  {
    id: 2,
    seller_id: 'student_a',
    title: 'Discrete Mathematics & Its Applications (Rosen)',
    category_id: 'books-notes',
    subcategory_id: 'engineering-textbooks',
    price: 420,
    original_price: 950,
    item_condition: 'Good',
    meetup_spot: 'Library Entrance',
    description: 'Kenneth Rosen 8th Edition discrete structures book. Great condition with handwritten summary cheat sheets.',
    has_notes: true,
    badge: 'Exam Essential',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:39Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:39Z').toISOString()
  },
  {
    id: 3,
    seller_id: 'student_b',
    title: 'Calculus: Early Transcendentals (Stewart)',
    category_id: 'books-notes',
    subcategory_id: 'engineering-textbooks',
    price: 480,
    original_price: 1499,
    item_condition: 'Like New',
    meetup_spot: 'Library Entrance',
    description: 'Single Variable Calculus textbook with limits, derivatives & integrals. Clean condition.',
    has_notes: false,
    badge: 'Academic Pick',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:38Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:38Z').toISOString()
  },
  {
    id: 4,
    seller_id: 'student_b',
    title: 'GATE CS 2026 Solved Papers & Notes',
    category_id: 'books-notes',
    subcategory_id: 'competitive-exam-prep',
    price: 350,
    original_price: 800,
    item_condition: 'Good',
    meetup_spot: 'Square One',
    description: '12 years solved GATE CSE question papers with detailed answer keys and handwritten revision notes.',
    has_notes: true,
    badge: 'GATE Ready',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:37Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:37Z').toISOString()
  },

  // 2. electronics-gadgets (2 Satkar, 2 Priya)
  {
    id: 5,
    seller_id: 'student_a',
    title: 'Logitech MX Master 2S Wireless Mouse',
    category_id: 'electronics-gadgets',
    subcategory_id: 'keyboards-mice',
    price: 1850,
    original_price: 3995,
    item_condition: 'Excellent',
    meetup_spot: 'Turing Block',
    description: 'Ergonomic wireless mouse with hyper-fast scroll wheel and multi-device Bluetooth pairing. Battery holds charge for weeks.',
    has_notes: false,
    badge: 'Verified Working',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:36Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:36Z').toISOString()
  },
  {
    id: 6,
    seller_id: 'student_a',
    title: 'Boat Stone 650 10W Bluetooth Speaker',
    category_id: 'electronics-gadgets',
    subcategory_id: 'audio-earphones-speakers',
    price: 799,
    original_price: 1990,
    item_condition: 'Like New',
    meetup_spot: 'Hostel Gate',
    description: 'Rugged waterproof Bluetooth speaker with deep bass and aux input. Used only a couple times in hostel room.',
    has_notes: false,
    badge: 'Hostel Favorite',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:35Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:35Z').toISOString()
  },
  {
    id: 7,
    seller_id: 'student_b',
    title: 'Apple 20W USB-C Power Adapter + Cable',
    category_id: 'electronics-gadgets',
    subcategory_id: 'chargers-cables',
    price: 990,
    original_price: 1900,
    item_condition: 'Like New',
    meetup_spot: 'Square One',
    description: 'Original Apple 20W fast charging brick with 1m braided USB-C to Lightning cable. Perfect working order.',
    has_notes: false,
    badge: 'Original Apple',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:34Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:34Z').toISOString()
  },
  {
    id: 8,
    seller_id: 'student_b',
    title: 'SanDisk Ultra 128GB Dual Drive Type-C',
    category_id: 'electronics-gadgets',
    subcategory_id: 'storage-pendrives-hdds',
    price: 550,
    original_price: 1200,
    item_condition: 'Brand New',
    meetup_spot: 'Library Entrance',
    description: 'High-speed OTG USB 3.1 pendrive with both Type-C and Type-A connectors for easy phone and laptop transfers.',
    has_notes: false,
    badge: 'Unopened',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:33Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:33Z').toISOString()
  },

  // 3. lab-engineering-gear (2 Satkar, 2 Priya)
  {
    id: 9,
    seller_id: 'student_a',
    title: 'Arduino Uno R3 Starter Kit with 30+ Sensors',
    category_id: 'lab-engineering-gear',
    subcategory_id: 'arduino-robotics-kits',
    price: 1150,
    original_price: 2400,
    item_condition: 'Like New',
    meetup_spot: 'Turing Block',
    description: 'Original ATmega328P Arduino board, breadboard, OLED display, ultrasonic sensors, motors and resistors. Tested in Turing lab.',
    has_notes: false,
    badge: 'Lab Ready',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:32Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:32Z').toISOString()
  },
  {
    id: 10,
    seller_id: 'student_a',
    title: 'Standard Student Lab Coat (Size L, Unisex)',
    category_id: 'lab-engineering-gear',
    subcategory_id: 'lab-coats-safety-gear',
    price: 250,
    original_price: 550,
    item_condition: 'Good',
    meetup_spot: 'Picasso Block',
    description: 'White 100% cotton lab coat required for chemistry and physics practicals. Washed and sanitized.',
    has_notes: false,
    badge: 'Mandatory Gear',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:31Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:31Z').toISOString()
  },
  {
    id: 11,
    seller_id: 'student_b',
    title: 'Casio fx-991EX ClassWiz Scientific Calculator',
    category_id: 'lab-engineering-gear',
    subcategory_id: 'scientific-calculators',
    price: 850,
    original_price: 1595,
    item_condition: 'Excellent',
    meetup_spot: 'Turing Block',
    description: 'High-resolution solar scientific calculator with matrix and equation solver. Allowed in university examinations.',
    has_notes: false,
    badge: 'Exam Approved',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:30Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:30Z').toISOString()
  },
  {
    id: 12,
    seller_id: 'student_b',
    title: 'Omega Mini Drafter & Engineering Drawing Set',
    category_id: 'lab-engineering-gear',
    subcategory_id: 'drafters-drawing-kits',
    price: 390,
    original_price: 850,
    item_condition: 'Like New',
    meetup_spot: 'Edison Block',
    description: 'Mini drafter with scale, clamps, sheet clips, compass and sheet container for engineering graphics lab.',
    has_notes: false,
    badge: 'First Year Must',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:29Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:29Z').toISOString()
  },

  // 4. stationery-supplies (2 Satkar, 2 Priya)
  {
    id: 13,
    seller_id: 'student_a',
    title: 'Classmate Spiral Notebook Bundle (Pack of 5)',
    category_id: 'stationery-supplies',
    subcategory_id: 'notebooks-registers',
    price: 220,
    original_price: 400,
    item_condition: 'Brand New',
    meetup_spot: 'Square One',
    description: 'Unused 300-page single line ruled spiral bound registers with thick smooth pages for semester note-taking.',
    has_notes: false,
    badge: 'Value Pack',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:28Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:28Z').toISOString()
  },
  {
    id: 14,
    seller_id: 'student_a',
    title: 'Uni-ball Eye Rollerball Pens (Set of 6)',
    category_id: 'stationery-supplies',
    subcategory_id: 'pens-writing',
    price: 180,
    original_price: 360,
    item_condition: 'Brand New',
    meetup_spot: 'Food Court',
    description: 'Smooth waterproof pigment ink roller pens. Brand new sealed pack of 3 blue and 3 black pens.',
    has_notes: false,
    badge: 'Smooth Writer',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:27Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:27Z').toISOString()
  },
  {
    id: 15,
    seller_id: 'student_b',
    title: 'Camel Acrylic Color Tubes (12 Shades) + Brushes',
    category_id: 'stationery-supplies',
    subcategory_id: 'art-supplies',
    price: 280,
    original_price: 520,
    item_condition: 'Like New',
    meetup_spot: 'Picasso Block',
    description: 'Set of 12 rich pigment acrylic colors with 4 synthetic brushes. Only 2 tubes opened for a college poster project.',
    has_notes: false,
    badge: 'Creative Pick',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:26Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:26Z').toISOString()
  },
  {
    id: 16,
    seller_id: 'student_b',
    title: 'Expanding Document File Folder (13 Pockets)',
    category_id: 'stationery-supplies',
    subcategory_id: 'files-folders',
    price: 150,
    original_price: 320,
    item_condition: 'Like New',
    meetup_spot: 'Library Entrance',
    description: 'Heavy duty accordion style organizer folder with colored divider tabs for keeping semester handouts safe.',
    has_notes: false,
    badge: 'Organized',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:25Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:25Z').toISOString()
  },

  // 5. hostel-room-essentials (2 Satkar, 2 Priya)
  {
    id: 17,
    seller_id: 'student_a',
    title: 'Prestige 1.5L Electric Stainless Steel Kettle',
    category_id: 'hostel-room-essentials',
    subcategory_id: 'kitchen-kettle-induction',
    price: 550,
    original_price: 1195,
    item_condition: 'Excellent',
    meetup_spot: 'Hostel Gate',
    description: 'Fast boiling electric kettle with auto shut-off. Perfect for late-night Maggi, coffee, and green tea in hostel.',
    has_notes: false,
    badge: 'Hostel Essential',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:24Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:24Z').toISOString()
  },
  {
    id: 18,
    seller_id: 'student_a',
    title: 'Rechargeable LED Desk Study Lamp',
    category_id: 'hostel-room-essentials',
    subcategory_id: 'study-lamp-electricals',
    price: 320,
    original_price: 699,
    item_condition: 'Like New',
    meetup_spot: 'Hostel Gate',
    description: 'Touch sensor 3 brightness dimmable study lamp with flexible gooseneck and USB charging port.',
    has_notes: false,
    badge: 'Night Study',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:23Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:23Z').toISOString()
  },
  {
    id: 19,
    seller_id: 'student_b',
    title: 'Havells Personal Table Fan (White/Blue)',
    category_id: 'hostel-room-essentials',
    subcategory_id: 'cooling-heating',
    price: 650,
    original_price: 1400,
    item_condition: 'Good',
    meetup_spot: 'Hostel Gate',
    description: 'Compact quiet table fan with 3 speed settings. Lightweight and fits easily on hostel study desk.',
    has_notes: false,
    badge: 'Room Cooler',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:22Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:22Z').toISOString()
  },
  {
    id: 20,
    seller_id: 'student_b',
    title: 'Stackable 3-Tier Storage Drawer Organizer',
    category_id: 'hostel-room-essentials',
    subcategory_id: 'storage-organisers',
    price: 380,
    original_price: 799,
    item_condition: 'Like New',
    meetup_spot: 'Square One',
    description: 'Durable modular storage cabinet for toiletries, snacks, medicines, and cables. Keeps hostel desk tidy.',
    has_notes: false,
    badge: 'Space Saver',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:21Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:21Z').toISOString()
  },

  // 6. fashion-accessories (2 Satkar, 2 Priya)
  {
    id: 21,
    seller_id: 'student_a',
    title: 'Casio Vintage Digital Watch (A168WA)',
    category_id: 'fashion-accessories',
    subcategory_id: 'watches-jewellery',
    price: 950,
    original_price: 1895,
    item_condition: 'Like New',
    meetup_spot: 'Square One',
    description: 'Classic retro stainless steel digital watch with Illuminator backlight, stopwatch, and water resistance.',
    has_notes: false,
    badge: 'Vintage Classic',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:20Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:20Z').toISOString()
  },
  {
    id: 22,
    seller_id: 'student_a',
    title: 'Chitkara CSE Official Pullover Hoodie (Size L)',
    category_id: 'fashion-accessories',
    subcategory_id: 'college-merch-hoodies',
    price: 599,
    original_price: 1200,
    item_condition: 'Like New',
    meetup_spot: 'Turing Block',
    description: 'Warm fleece black campus hoodie with embroidered university crest. Barely worn during previous winter fest.',
    has_notes: false,
    badge: 'Official Merch',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:19Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:19Z').toISOString()
  },
  {
    id: 23,
    seller_id: 'student_b',
    title: 'Wildcraft 35L Water-Resistant Laptop Backpack',
    category_id: 'fashion-accessories',
    subcategory_id: 'bags-backpacks',
    price: 750,
    original_price: 1699,
    item_condition: 'Good',
    meetup_spot: 'Library Entrance',
    description: 'Sturdy laptop college backpack with rain cover, ergonomic shoulder padding, and dedicated 15.6 inch laptop sleeve.',
    has_notes: false,
    badge: 'Campus Daily',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:18Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:18Z').toISOString()
  },
  {
    id: 24,
    seller_id: 'student_b',
    title: 'Fastrack Aviator Sunglasses with Hard Case',
    category_id: 'fashion-accessories',
    subcategory_id: 'watches-jewellery',
    price: 490,
    original_price: 1199,
    item_condition: 'Like New',
    meetup_spot: 'Food Court',
    description: '100% UV protected metal frame aviator shades with polarized lenses and scratch-free carrying case.',
    has_notes: false,
    badge: 'UV Protected',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:17Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:17Z').toISOString()
  },

  // 7. perfumes-grooming (2 Satkar, 2 Priya)
  {
    id: 25,
    seller_id: 'student_a',
    title: 'Solid Amber & Sandalwood Cologne (15g Pocket Tin)',
    category_id: 'perfumes-grooming',
    subcategory_id: 'perfumes-deos',
    price: 249,
    original_price: 450,
    item_condition: 'Brand New',
    meetup_spot: 'Hostel Gate',
    description: 'Alcohol-free travel solid cologne in a sliding matte metal tin. Warm Mysore sandalwood notes blended with amber.',
    has_notes: false,
    badge: 'Pocket Cologne',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:16Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:16Z').toISOString()
  },
  {
    id: 26,
    seller_id: 'student_a',
    title: 'Beardo Godfather Hair Wax + Beard Wash Combo',
    category_id: 'perfumes-grooming',
    subcategory_id: 'grooming-kits',
    price: 290,
    original_price: 650,
    item_condition: 'Brand New',
    meetup_spot: 'Square One',
    description: 'Strong hold matte finish hair clay wax and refreshing aloe beard cleanser. Unopened sealed packaging.',
    has_notes: false,
    badge: 'Grooming Pack',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:15Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:15Z').toISOString()
  },
  {
    id: 27,
    seller_id: 'student_b',
    title: 'Botanical Room & Body Mist: Cedar & Lavender (50ml)',
    category_id: 'perfumes-grooming',
    subcategory_id: 'perfumes-deos',
    price: 299,
    original_price: 499,
    item_condition: 'Freshly Crafted',
    meetup_spot: 'Food Court',
    description: 'Includes: Natural Essential Oils. Freshly crafted hand-blended botanical fragrance for calming study vibes.',
    has_notes: false,
    badge: 'Organic',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:14Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:14Z').toISOString()
  },
  {
    id: 28,
    seller_id: 'student_b',
    title: 'The Ordinary Niacinamide 10% + Zinc 1% (30ml)',
    category_id: 'perfumes-grooming',
    subcategory_id: 'skincare',
    price: 380,
    original_price: 600,
    item_condition: 'Brand New',
    meetup_spot: 'Library Entrance',
    description: 'Original authentic serum for blemish control and sebum reduction. Sealed dropper bottle.',
    has_notes: false,
    badge: 'Sealed Bottle',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:13Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:13Z').toISOString()
  },

  // 8. handmade-art (2 Satkar, 2 Priya)
  {
    id: 29,
    seller_id: 'student_a',
    title: 'Hand-Carved Wooden Pen Stand & Mobile Dock',
    category_id: 'handmade-art',
    subcategory_id: 'custom-gifts',
    price: 280,
    original_price: 550,
    item_condition: 'Handmade',
    meetup_spot: 'Picasso Block',
    description: 'Artisan polished sheesham wood pen organizer with integrated smartphone viewing notch. Handcrafted on campus.',
    has_notes: false,
    badge: 'Campus Artisan',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:12Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:12Z').toISOString()
  },
  {
    id: 30,
    seller_id: 'student_a',
    title: 'Minimalist Campus Skyline Charcoal Sketch (Framed)',
    category_id: 'handmade-art',
    subcategory_id: 'paintings-sketches',
    price: 450,
    original_price: 900,
    item_condition: 'Brand New',
    meetup_spot: 'Square One',
    description: 'Original black and white charcoal drawing of Chitkara university clock tower in a clean black wooden frame (A4).',
    has_notes: false,
    badge: 'Original Art',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:11Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:11Z').toISOString()
  },
  {
    id: 31,
    seller_id: 'student_b',
    title: 'Handcrafted Matcha Green Ceramic Coffee Mug',
    category_id: 'handmade-art',
    subcategory_id: 'decor',
    price: 350,
    original_price: 699,
    item_condition: 'Handmade',
    meetup_spot: 'Square One',
    description: 'Hand-thrown ceramic pottery mug with earthy green glaze finish. Perfect for matcha, chai, or desk decor.',
    has_notes: false,
    badge: 'Student Artisan',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:10Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:10Z').toISOString()
  },
  {
    id: 32,
    seller_id: 'student_b',
    title: 'Handmade Daisy Floral Crochet Tote Bag',
    category_id: 'handmade-art',
    subcategory_id: 'crochet-knitting',
    price: 380,
    original_price: 750,
    item_condition: 'Handmade',
    meetup_spot: 'Gate No. 4',
    description: 'Custom crochet shoulder tote bag knit with premium soft milk cotton yarn in pastel cream and yellow daisy pattern.',
    has_notes: false,
    badge: 'Hand Knitted',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:09Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:09Z').toISOString()
  },

  // 9. sports-fitness-cycles (2 Satkar, 2 Priya)
  {
    id: 33,
    seller_id: 'student_a',
    title: 'Yonex Muscle Power 29 Light Badminton Racket',
    category_id: 'sports-fitness-cycles',
    subcategory_id: 'racket-sports',
    price: 850,
    original_price: 1790,
    item_condition: 'Good',
    meetup_spot: 'Sports Complex',
    description: 'Lightweight isometric badminton racket strung at 24 lbs with new grip tape and full head cover. Great smash power.',
    has_notes: false,
    badge: 'Tournament Ready',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:08Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:08Z').toISOString()
  },
  {
    id: 34,
    seller_id: 'student_a',
    title: 'Rubber Coated Hex Dumbbells (5kg Pair)',
    category_id: 'sports-fitness-cycles',
    subcategory_id: 'gym-fitness',
    price: 650,
    original_price: 1300,
    item_condition: 'Good',
    meetup_spot: 'Hostel Gate',
    description: 'Solid cast iron hexagonal dumbbells with non-slip chrome knurled handles. Floor friendly rubber coating.',
    has_notes: false,
    badge: 'Hostel Workout',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:07Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:07Z').toISOString()
  },
  {
    id: 35,
    seller_id: 'student_b',
    title: 'Nivia Storm Football (Size 5, Rubber)',
    category_id: 'sports-fitness-cycles',
    subcategory_id: 'football-basketball',
    price: 390,
    original_price: 750,
    item_condition: 'Like New',
    meetup_spot: 'Sports Complex',
    description: 'All-weather durable rubber moulded football suitable for turf and grass grounds. Includes inflation pin.',
    has_notes: false,
    badge: 'Match Ready',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:06Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:06Z').toISOString()
  },
  {
    id: 36,
    seller_id: 'student_b',
    title: 'Anti-Skid High Density 6mm Yoga Mat with Strap',
    category_id: 'sports-fitness-cycles',
    subcategory_id: 'yoga',
    price: 420,
    original_price: 999,
    item_condition: 'Like New',
    meetup_spot: 'Square One',
    description: 'Eco-friendly textured EVA foam fitness mat with excellent cushioning and dual-sided grip. Includes carry strap.',
    has_notes: false,
    badge: 'Fitness Pick',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:05Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:05Z').toISOString()
  },

  // 10. other (2 Satkar, 2 Priya)
  {
    id: 37,
    seller_id: 'student_a',
    title: 'Heavy Duty 4-Digit Combination Cable Bike Lock',
    category_id: 'other',
    subcategory_id: 'bicycles-accessories',
    price: 190,
    original_price: 450,
    item_condition: 'Like New',
    meetup_spot: 'Hostel Gate',
    description: 'Resettable combination steel cable lock for securing bicycles and hostel storage trunks. Weatherproof vinyl coating.',
    has_notes: false,
    badge: 'Campus Security',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:04Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:04Z').toISOString()
  },
  {
    id: 38,
    seller_id: 'student_a',
    title: 'Foldable Laptop Bed Table with Cup Holder',
    category_id: 'other',
    subcategory_id: null,
    price: 360,
    original_price: 750,
    item_condition: 'Good',
    meetup_spot: 'Hostel Gate',
    description: 'Engineered wood portable desk tray with non-slip metal legs and tablet slot. Perfect for studying from hostel bed.',
    has_notes: false,
    badge: 'Study Comfort',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:03Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:03Z').toISOString()
  },
  {
    id: 39,
    seller_id: 'student_b',
    title: 'Compact 3-Fold UV Resistant Rain Umbrella',
    category_id: 'other',
    subcategory_id: null,
    price: 220,
    original_price: 499,
    item_condition: 'Like New',
    meetup_spot: 'Square One',
    description: 'Windproof lightweight folding umbrella with black UV sun blocking coating. Fits easily inside college backpack.',
    has_notes: false,
    badge: 'Monsoon Ready',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:02Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:02Z').toISOString()
  },
  {
    id: 40,
    seller_id: 'student_b',
    title: 'Noise-Cancelling Memory Foam Earplugs (3 Pairs)',
    category_id: 'other',
    subcategory_id: null,
    price: 120,
    original_price: 280,
    item_condition: 'Brand New',
    meetup_spot: 'Library Entrance',
    description: 'High SNR 38dB slow rebound foam earplugs in compact hygiene cases. Ideal for exam revision in busy hostel rooms.',
    has_notes: false,
    badge: 'Exam Focus',
    status: 'approved',
    pending_type: null,
    created_at: new Date('2026-01-01T00:00:01Z').toISOString(),
    updated_at: new Date('2026-01-01T00:00:01Z').toISOString()
  }
];

const photoCycle = [
  imgCalcBook,
  imgOrgChem,
  imgGraphCalc,
  imgArduinoKit,
  imgMatchaMug,
  imgToteBag,
  imgSolidPerfume,
  imgRoomSpray
];

const SEED_LISTING_PHOTOS = SEED_LISTINGS.map((listing, index) => ({
  id: index + 1,
  listing_id: listing.id,
  url: photoCycle[index % photoCycle.length],
  sort_order: 0
}));

export function resetDatabase() {
  setTable('users', SEED_USERS);
  setTable('listings', SEED_LISTINGS);
  setTable('listing_photos', SEED_LISTING_PHOTOS);
  setTable('listing_pending_edits', []);
  setTable('wishlist', []);
  setTable('conversations', []);
  setTable('messages', []);

  // Clear chat conversation storage as well
  try {
    localStorage.removeItem('unimart_chat_conversations_v4');
    localStorage.removeItem('unimart_chat_conversations_v5');
    localStorage.removeItem('unimart_chat_conversations_v6');
  } catch (_e) {}
}

export function clearPreviousDbVersions() {
  const tables = ['users', 'wishlist'];
  try {
    // 1. Safe data migration: copy user registrations and wishlist forward if new version is blank
    for (const table of tables) {
      const currentKey = `${DB_PREFIX}${table}_${DB_VERSION}`;
      const currentData = localStorage.getItem(currentKey);
      if (!currentData) {
        for (let v = 6; v >= 1; v--) {
          const oldKey = `${DB_PREFIX}${table}_v${v}`;
          const oldData = localStorage.getItem(oldKey);
          if (oldData) {
            try {
              localStorage.setItem(currentKey, oldData);
              break;
            } catch (_e) {}
          }
        }
      }
    }

    // 2. Safe cleanup: prune only obsolete versions older than v5 to avoid breaking rolling sessions
    const toRemove = [];
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i);
      if (
        k &&
        (k.startsWith(DB_PREFIX) || k.startsWith('unimart_chat_conversations_')) &&
        !k.includes(DB_VERSION) &&
        !k.includes('v5')
      ) {
        toRemove.push(k);
      }
    }
    toRemove.forEach((k) => {
      try { localStorage.removeItem(k); } catch (_e) {}
    });
  } catch (e) {
    console.warn('[UniMart DB] Safe database migration notice:', e);
  }
}

export function initDatabase() {
  clearPreviousDbVersions();
  if (!getTable('users')) setTable('users', SEED_USERS);
  if (!getTable('listings')) setTable('listings', SEED_LISTINGS);
  if (!getTable('listing_photos')) setTable('listing_photos', SEED_LISTING_PHOTOS);
  if (!getTable('listing_pending_edits')) setTable('listing_pending_edits', []);
  if (!getTable('wishlist')) setTable('wishlist', []);
  if (!getTable('conversations')) setTable('conversations', []);
  if (!getTable('messages')) setTable('messages', []);
}

// Automatically ensure tables exist on module load
initDatabase();

// Expose on window for easy developer & testing reset
if (typeof window !== 'undefined') {
  window.resetUniMartDemoData = () => {
    resetDatabase();
    window.location.reload();
  };
}
