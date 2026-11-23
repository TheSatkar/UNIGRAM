/**
 * Seeded mock accounts for UNIMART
 */

export const SEEDED_USERS = [
  {
    id: 'admin_1',
    email: 'admin@chitkara.edu.in',
    password: 'Unimart@123',
    name: 'Campus Admin',
    studentId: 'ADMIN-001',
    role: 'admin'
  },
  {
    id: 'student_a',
    email: 'satkar0696.becse25@chitkara.edu.in',
    password: 'student123',
    name: 'Satkar Singh',
    studentId: '2110990696',
    department: 'Computer Science & Engineering',
    role: 'student'
  },
  {
    id: 'student_b',
    email: 'priya.k@chitkara.edu.in',
    password: 'student123',
    name: 'Priya K.',
    studentId: '2110990697',
    department: 'Mathematics',
    role: 'student'
  },
  {
    id: 'student_c',
    email: 'nisha.r@chitkara.edu.in',
    password: 'student123',
    name: 'Nisha R.',
    studentId: '2110990698',
    department: 'Fine Arts',
    role: 'student'
  },
  {
    id: 'student_d',
    email: 'madhur.t@chitkara.edu.in',
    password: 'student123',
    name: 'Madhur T.',
    studentId: '2110990699',
    department: 'Mechanical Engg',
    role: 'student'
  },
  {
    id: 'student_e',
    email: 'charlie.w@chitkara.edu.in',
    password: 'student123',
    name: 'Charlie W.',
    studentId: '2110990700',
    department: 'Biotech Dept',
    role: 'student'
  },
  {
    id: 'student_f',
    email: 'simran.d@chitkara.edu.in',
    password: 'student123',
    name: 'Simran D.',
    studentId: '2110990701',
    department: 'Design & Media',
    role: 'student'
  },
  {
    id: 'student_g',
    email: 'harsh.p@chitkara.edu.in',
    password: 'student123',
    name: 'Harsh P.',
    studentId: '2110990702',
    department: 'Maker Collective',
    role: 'student'
  },
  {
    id: 'student_h',
    email: 'aman.v@chitkara.edu.in',
    password: 'student123',
    name: 'Aman V.',
    studentId: '2110990703',
    department: 'Electronics Engg',
    role: 'student'
  },
  {
    id: 'student_i',
    email: 'rohan.s@chitkara.edu.in',
    password: 'student123',
    name: 'Rohan S.',
    studentId: '2110990704',
    department: 'Sports & Athletics',
    role: 'student'
  },
  {
    id: 'student_j',
    email: 'kavya.m@chitkara.edu.in',
    password: 'student123',
    name: 'Kavya M.',
    studentId: '2110990705',
    department: 'Architecture',
    role: 'student'
  },
  {
    id: 'student_k',
    email: 'ananya.g@chitkara.edu.in',
    password: 'student123',
    name: 'Ananya G.',
    studentId: '2110990706',
    department: 'Computer Science',
    role: 'student'
  },
  {
    id: 'student_l',
    email: 'vikram.b@chitkara.edu.in',
    password: 'student123',
    name: 'Vikram B.',
    studentId: '2110990707',
    department: 'Civil Engg',
    role: 'student'
  }
];

export function findUserByCredentials(email, password) {
  const normalizedEmail = (email || '').trim().toLowerCase();
  return SEEDED_USERS.find(
    (u) => u.email.toLowerCase() === normalizedEmail && u.password === password
  );
}

/**
 * Look up a user by email only (used by forgot-password flow).
 * Returns the user object or undefined if not found.
 */
export function findUserByEmail(email) {
  const normalizedEmail = (email || '').trim().toLowerCase();
  return SEEDED_USERS.find((u) => u.email.toLowerCase() === normalizedEmail);
}

/**
 * Reset a user's password in the in-memory store.
 * Returns true if the user was found and updated, false otherwise.
 */
export function resetUserPassword(email, newPassword) {
  const normalizedEmail = (email || '').trim().toLowerCase();
  const user = SEEDED_USERS.find((u) => u.email.toLowerCase() === normalizedEmail);
  if (!user) return false;
  user.password = newPassword;
  return true;
}

export function registerNewStudent({ name, email, studentId, password }) {
  const newStudent = {
    id: `student_${Date.now()}`,
    name: name.trim(),
    email: email.trim().toLowerCase(),
    studentId: studentId.trim(),
    password,
    role: 'student' // Registration must default to student; no role selection allowed
  };
  SEEDED_USERS.push(newStudent);
  return newStudent;
}
