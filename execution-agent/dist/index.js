"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const pg_1 = require("pg");
const puppeteer_1 = __importDefault(require("puppeteer"));
const path_1 = __importDefault(require("path"));
const dotenv_1 = __importDefault(require("dotenv"));
dotenv_1.default.config();
const connectionString = process.env.DATABASE_URL || 'postgresql://postgres:postgres@localhost:5432/nod';
let isProcessing = false;
async function run() {
    const client = new pg_1.Client({ connectionString });
    try {
        await client.connect();
        console.log('Connected to PostgreSQL database');
        // Poll the database for approved cards
        setInterval(async () => {
            if (isProcessing)
                return;
            try {
                const res = await client.query("SELECT * FROM nod_cards WHERE status = 'approved' ORDER BY created_at ASC LIMIT 1");
                if (res.rows.length > 0) {
                    const card = res.rows[0];
                    isProcessing = true;
                    await processCard(client, card);
                    isProcessing = false;
                }
            }
            catch (err) {
                console.error('Error during polling query:', err);
                isProcessing = false;
            }
        }, 3000);
    }
    catch (err) {
        console.error('Database connection failed:', err);
        process.exit(1);
    }
}
async function processCard(client, card) {
    console.log(`Processing card ${card.id} for user ${card.user_id}: ${card.summary_text}`);
    let browser;
    try {
        // Launch headless browser. Connects via Browserbase if configuration variables exist.
        const wsUrl = process.env.BROWSERBASE_API_KEY
            ? `wss://connect.browserbase.com?apiKey=${process.env.BROWSERBASE_API_KEY}&projectId=${process.env.BROWSERBASE_PROJECT_ID}`
            : undefined;
        if (wsUrl) {
            console.log('Connecting to Browserbase...');
            browser = await puppeteer_1.default.connect({ browserWSEndpoint: wsUrl });
        }
        else {
            console.log('Launching local Puppeteer instance...');
            browser = await puppeteer_1.default.launch({ headless: true });
        }
        const page = await browser.newPage();
        // Navigation target (mock subscription portal)
        const mockFilePath = path_1.default.resolve('mock/index.html');
        const targetUrl = 'file://' + mockFilePath;
        console.log(`Navigating to ${targetUrl}`);
        await page.goto(targetUrl);
        // Step 1: Click the initial "Cancel Subscription" button
        console.log('Waiting for cancel button...');
        await page.waitForSelector('#btn-cancel-initial');
        await page.click('#btn-cancel-initial');
        console.log('Clicked Cancel Subscription');
        // Step 2: Wait for the confirmation step dialog to load
        await page.waitForSelector('#btn-cancel-final');
        console.log('Navigated to confirmation view');
        // ==========================================
        // APPROVAL GATE
        // ==========================================
        console.log('Entering Approval Gate. Pausing execution before final confirmation.');
        // Update card status to 'awaiting_user_nod'
        await client.query("UPDATE nod_cards SET status = 'awaiting_user_nod', updated_at = NOW() WHERE id = $1", [card.id]);
        console.log(`Card ${card.id} status updated to 'awaiting_user_nod'`);
        // Poll the DB waiting for the user to nod (updates status to 'approved' again)
        // or rejects (status = 'rejected')
        let approved = false;
        let rejected = false;
        const maxWaitSeconds = 300; // 5 minutes timeout
        const checkIntervalMs = 2000;
        const loops = (maxWaitSeconds * 1000) / checkIntervalMs;
        for (let i = 0; i < loops; i++) {
            await new Promise(resolve => setTimeout(resolve, checkIntervalMs));
            const pollRes = await client.query("SELECT status FROM nod_cards WHERE id = $1", [card.id]);
            if (pollRes.rows.length === 0) {
                console.log(`Card ${card.id} deleted. Aborting.`);
                rejected = true;
                break;
            }
            const currentStatus = pollRes.rows[0].status;
            if (currentStatus === 'approved') {
                approved = true;
                console.log(`User approved Card ${card.id}. Resuming execution...`);
                break;
            }
            else if (currentStatus === 'rejected') {
                rejected = true;
                console.log(`User rejected Card ${card.id}. Aborting.`);
                break;
            }
        }
        if (approved) {
            // Step 3: Complete execution (click final confirmation button)
            await page.click('#btn-cancel-final');
            console.log('Clicked final Confirm Cancellation button');
            // Wait for success screen
            await page.waitForSelector('#view-success');
            console.log('Success screen loaded. Subscription cancelled successfully.');
            // Update database status to 'completed'
            await client.query("UPDATE nod_cards SET status = 'completed', updated_at = NOW() WHERE id = $1", [card.id]);
            console.log(`Card ${card.id} marked as 'completed'`);
        }
        else if (rejected) {
            // Clean exit (already rejected)
            console.log(`Card ${card.id} process closed with status 'rejected'`);
        }
        else {
            // Timeout
            console.log(`Approval Gate timed out after ${maxWaitSeconds} seconds.`);
            await client.query("UPDATE nod_cards SET status = 'failed', updated_at = NOW() WHERE id = $1", [card.id]);
        }
    }
    catch (err) {
        console.error(`Error processing card ${card.id}:`, err);
        try {
            await client.query("UPDATE nod_cards SET status = 'failed', updated_at = NOW() WHERE id = $1", [card.id]);
        }
        catch (dbErr) {
            console.error('Failed to set status to failed in database:', dbErr);
        }
    }
    finally {
        if (browser) {
            await browser.close();
            console.log('Closed browser session');
        }
    }
}
// Start execution
run().catch(console.error);
