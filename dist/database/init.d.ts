import sqlite3 from "sqlite3";
import { Client } from "pg";
export declare function initializeDatabase(): Promise<void>;
export declare function getDatabase(): sqlite3.Database | null;
export declare function getPostgresClient(): Client | null;
