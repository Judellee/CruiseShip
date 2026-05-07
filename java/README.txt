============================================================
  CruiseMS — Cruise Ship Management System
  COSC 457 Database Management Systems
============================================================

WHAT IT DOES
------------
A Java Swing desktop application for managing a cruise ship company.
Two portals are available from the home screen:

  Passenger Portal
    - Register an account and log in
    - Browse available voyages and ships
    - Book a voyage (cabin selection included)
    - View your reservations, tickets, payments, and excursions
    - Add excursions to an existing reservation

  Staff Portal (password: admin123)
    - Ships, Ship Details (decks, dining venues, facilities, events)
    - Employees and Crew (ship assignments, crew cabins, work schedules)
    - Voyages, Passengers, Reservations
    - Financials (tickets, payments, excursion catalog)
    - Maintenance records and Supplies inventory
    - Safety (emergency drills, ports, itineraries and stops)


REQUIREMENTS
------------
  - Java JDK 17 or higher
  - MySQL Server
  - MySQL Connector/J JAR already in lib\ folder


SETUP
-----
1. Create the database
   In MySQL run:
     CREATE DATABASE IF NOT EXISTS COSC457;
   Then load the schema and sample data:
     mysql -u root -p COSC457 < cruise_ship.sql

2. Configure the connection
   Open db.properties and set your credentials:

     host=localhost
     port=3306
     database=COSC457
     user=root
     password=your_mysql_password
     admin_password=admin123

   Change admin_password to set the staff login password.

3. Run the app
   Double-click CruiseMS.jar
   or from a terminal in this folder:
     java -jar CruiseMS.jar


PROJECT FILES
-------------
  CruiseMS.jar        Runnable application (double-click to open)
  cruise_ship.sql     Database schema with sample data
  db.properties       Database connection settings
  README.txt          This file
  src\                Java source code
  lib\                JDBC driver


TOWSON NETWORK (off-campus)
----------------------------
If using the Towson MySQL server instead of a local install,
connect to the Towson VPN first: https://vpnc.towson.edu
Then update host and credentials in db.properties accordingly.

============================================================
