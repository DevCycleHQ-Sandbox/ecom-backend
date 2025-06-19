"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.initializeDatabase = initializeDatabase;
exports.getDatabase = getDatabase;
exports.getPostgresClient = getPostgresClient;
const sqlite3_1 = __importDefault(require("sqlite3"));
const pg_1 = require("pg");
const fs_1 = __importDefault(require("fs"));
const path_1 = __importDefault(require("path"));
let db = null;
let pgClient = null;
async function initializeDatabase() {
    const usePostgres = process.env.DATABASE_TYPE === "postgres";
    if (usePostgres) {
        await initializePostgres();
    }
    else {
        await initializeSQLite();
    }
    await seedDatabase();
}
async function initializeSQLite() {
    return new Promise((resolve, reject) => {
        const dbPath = process.env.DATABASE_URL || "./database.sqlite";
        db = new sqlite3_1.default.Database(dbPath, (err) => {
            if (err) {
                console.error("❌ Error opening SQLite database:", err);
                reject(err);
                return;
            }
            console.log("✅ Connected to SQLite database");
            createTables().then(resolve).catch(reject);
        });
    });
}
async function initializePostgres() {
    pgClient = new pg_1.Client({
        connectionString: process.env.POSTGRES_URL,
    });
    try {
        await pgClient.connect();
        console.log("✅ Connected to PostgreSQL database");
        await createTables();
    }
    catch (error) {
        console.error("❌ Error connecting to PostgreSQL:", error);
        throw error;
    }
}
async function createTables() {
    const sqliteSchema = `
    CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      username TEXT UNIQUE NOT NULL,
      email TEXT UNIQUE NOT NULL,
      password TEXT NOT NULL,
      role TEXT NOT NULL DEFAULT 'user',
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS products (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      description TEXT,
      price REAL NOT NULL,
      image_url TEXT,
      category TEXT,
      stock_quantity INTEGER DEFAULT 0,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS cart_items (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL,
      product_id TEXT NOT NULL,
      quantity INTEGER NOT NULL DEFAULT 1,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (user_id) REFERENCES users (id),
      FOREIGN KEY (product_id) REFERENCES products (id)
    );

    CREATE TABLE IF NOT EXISTS orders (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL,
      total_amount REAL NOT NULL,
      status TEXT NOT NULL DEFAULT 'pending',
      shipping_address TEXT NOT NULL,
      card_number TEXT NOT NULL,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (user_id) REFERENCES users (id)
    );

    CREATE TABLE IF NOT EXISTS order_items (
      id TEXT PRIMARY KEY,
      order_id TEXT NOT NULL,
      product_id TEXT NOT NULL,
      quantity INTEGER NOT NULL,
      price REAL NOT NULL,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (order_id) REFERENCES orders (id),
      FOREIGN KEY (product_id) REFERENCES products (id)
    );
  `;
    if (db) {
        const statements = sqliteSchema.split(";").filter((stmt) => stmt.trim());
        for (const statement of statements) {
            await new Promise((resolve, reject) => {
                db.run(statement, (err) => {
                    if (err)
                        reject(err);
                    else
                        resolve();
                });
            });
        }
    }
    else if (pgClient) {
        const postgresSchema = sqliteSchema
            .replace(/TEXT PRIMARY KEY/g, "UUID PRIMARY KEY DEFAULT gen_random_uuid()")
            .replace(/DATETIME/g, "TIMESTAMP")
            .replace(/REAL/g, "DECIMAL(10,2)");
        await pgClient.query(postgresSchema);
    }
    console.log("✅ Database tables created/verified");
}
async function seedDatabase() {
    try {
        const usersDataPath = path_1.default.join(__dirname, "../../data/users.json");
        const productsDataPath = path_1.default.join(__dirname, "../../data/products.json");
        if (fs_1.default.existsSync(usersDataPath) && fs_1.default.existsSync(productsDataPath)) {
            const usersData = JSON.parse(fs_1.default.readFileSync(usersDataPath, "utf8"));
            const productsData = JSON.parse(fs_1.default.readFileSync(productsDataPath, "utf8"));
            for (const user of usersData.users) {
                await insertUserIfNotExists(user);
            }
            for (const product of productsData.products) {
                await insertProductIfNotExists(product);
            }
            console.log("✅ Database seeded with initial data");
        }
    }
    catch (error) {
        console.log("⚠️  Seed data not found or error seeding database:", error);
    }
}
async function insertUserIfNotExists(user) {
    if (db) {
        return new Promise((resolve, reject) => {
            db.get("SELECT id FROM users WHERE username = ?", [user.username], (err, row) => {
                if (err) {
                    reject(err);
                    return;
                }
                if (!row) {
                    db.run("INSERT INTO users (id, username, email, password, role) VALUES (?, ?, ?, ?, ?)", [user.id, user.username, user.email, user.password, user.role], (err) => {
                        if (err)
                            reject(err);
                        else
                            resolve();
                    });
                }
                else {
                    resolve();
                }
            });
        });
    }
}
async function insertProductIfNotExists(product) {
    if (db) {
        return new Promise((resolve, reject) => {
            db.get("SELECT id FROM products WHERE id = ?", [product.id], (err, row) => {
                if (err) {
                    reject(err);
                    return;
                }
                if (!row) {
                    db.run("INSERT INTO products (id, name, description, price, image_url, category, stock_quantity) VALUES (?, ?, ?, ?, ?, ?, ?)", [
                        product.id,
                        product.name,
                        product.description,
                        product.price,
                        product.image_url,
                        product.category,
                        product.stock_quantity,
                    ], (err) => {
                        if (err)
                            reject(err);
                        else
                            resolve();
                    });
                }
                else {
                    resolve();
                }
            });
        });
    }
}
function getDatabase() {
    return db;
}
function getPostgresClient() {
    return pgClient;
}
//# sourceMappingURL=init.js.map