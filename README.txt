CampusPark — College Parking Management System

This project connects a responsive web dashboard to the Java services extracted from your supplied project archive and to the existing MySQL database college_parking. It uses the real parking_slots, vehicles, and parking_records tables. The dashboard refreshes automatically every 15 seconds.

Features
- Live slot totals and availability by student/faculty and vehicle type
- Current parked vehicles and parking slot
- Register and edit vehicles with the existing VehicleService validation
- Park a registered vehicle or release it; the original ParkingService transaction updates the slot and parking record together
- Search vehicles and parking history
- Delete an unparked vehicle, subject to the existing service rules

Database
The backend uses the existing schema and does not seed, reset, or overwrite records. The active-parking query from sql/active_parking.sql is also used by the original parking-record DAO. MySQL Connector/J is bundled in WebContent/WEB-INF/lib. Database connection settings are read from environment variables; the password is never stored in the project files.

Start on this computer
1. Make sure the MySQL80 service is running and college_parking is present.
2. Open PowerShell in this folder and run .\start-server.ps1.
3. Enter the MySQL password when prompted. Keep that PowerShell window open while using the app; Ctrl+C stops Tomcat.
4. Open http://localhost:8080/CampusPark/ .

Optional connection variables: CAMPUSPARK_DB_URL, CAMPUSPARK_DB_USER, CAMPUSPARK_DB_PASSWORD, and JAVA_HOME.

Eclipse
Import this folder as an Existing Maven Project (File > Import > Maven > Existing Maven Projects). The server runtime is the bundled tomcat/apache-tomcat-10.1.62 directory. The project uses Jakarta Servlet API 6 and a Java 11 source target.

The supplied archive contained the DAO, model and service source, but no main class, login implementation, schema dump, or build configuration. This project adds the web API and browser UI around the included VehicleService and ParkingService. No demo parking records are generated.

Container deployment preparation
- Dockerfile and compose.yaml run the web app with a separate MySQL 8.4 container.
- Copy .env.example to .env and set strong local passwords before running docker compose up --build.
- sql/schema.sql creates an empty compatible schema and inserts no records. Existing local vehicles, owners, slots, and parking history are not copied automatically; moving those records to a cloud database requires an explicit destination and approval.
- Keep .env and database exports private. They are excluded from Git by .gitignore.
