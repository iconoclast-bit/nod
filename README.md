# Daily Life Admin (Nod)

This project consists of a Flutter frontend, a Spring Boot backend, and a PostgreSQL database. Follow these steps to run the complete stack locally.

## Prerequisites
- **Docker & Docker Compose** (for the database)
- **Java 21 & Maven** (for the backend)
- **Flutter SDK** (for the frontend)

---

## Step 1: Start the Database

The project uses a local PostgreSQL database configured via Docker.

1. Open your terminal and navigate to the root directory of the project (`daily-life-admin`).
2. Run the following command in the background:
   ```bash
   docker-compose up -d
   ```
*(Note: To stop the database later, you can run `docker-compose down`)*

---

## Step 2: Start the Backend

The backend is a Java Spring Boot application. 

1. Before starting, ensure your Gemini API key is properly set in `backend/src/main/resources/application.properties`:
   ```properties
   gemini.api.key=YOUR_GEMINI_API_KEY
   ```
2. Open a new terminal tab.
3. Navigate to the `backend` directory:
   ```bash
   cd backend
   ```
4. Start the application:
   ```bash
   ./mvnw spring-boot:run
   ```
The backend will connect to the PostgreSQL database and run on `http://localhost:8080`.

---

## Step 3: Start the Frontend

The frontend is a Flutter web application. **You must run it on a fixed port** to prevent Google OAuth `origin_mismatch` errors.

1. Open a new terminal tab.
2. Navigate to the `frontend` directory:
   ```bash
   cd frontend
   ```
3. Run the Flutter web app on a specific port (e.g., `4040`):
   ```bash
   flutter run -d chrome --web-port 4040
   ```

### ⚠️ Google Sign-In Setup (Crucial)
Because you are using Google Sign-In, the exact URL you run your frontend on (e.g., `http://localhost:4040`) must be authorized.
If you get an `Error 400: origin_mismatch` when logging in:
1. Go to your [Google Cloud Console Credentials](https://console.cloud.google.com/apis/credentials).
2. Edit your OAuth 2.0 Client ID.
3. Add `http://localhost:4040` (with **no trailing slash**) to the **Authorized JavaScript origins**.
4. Click Save, and hard-refresh your browser.

---

## Shutting Down
- **Frontend / Backend**: Go to their respective terminal windows and press `Ctrl + C` (or `q` for Flutter) to stop the processes.
- **Database**: Run `docker-compose down` in the root directory to stop and remove the PostgreSQL container.
