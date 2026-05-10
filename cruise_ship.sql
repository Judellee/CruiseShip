-- =============================================
-- CRUISE SHIP MANAGEMENT SYSTEM
-- Based on ERD from group diagram
-- =============================================
-- Target database is specified in the mysql connection command

-- Core entities
CREATE TABLE Ship (
    ShipID INT PRIMARY KEY AUTO_INCREMENT,
    ShipName VARCHAR(100) NOT NULL,
    Capacity INT,
    ShipTypeID INT
);

CREATE TABLE ShipType (
    ShipTypeID INT PRIMARY KEY AUTO_INCREMENT,
    TypeName VARCHAR(50) NOT NULL
);

CREATE TABLE Deck (
    DeckID INT PRIMARY KEY AUTO_INCREMENT,
    DeckNumber INT NOT NULL,
    ShipID INT,
    FOREIGN KEY (ShipID) REFERENCES Ship(ShipID)
);

CREATE TABLE Cabin (
    CabinID INT PRIMARY KEY AUTO_INCREMENT,
    CabinNumber VARCHAR(10) NOT NULL,
    CabinType VARCHAR(30),
    DeckID INT,
    ShipID INT,
    FOREIGN KEY (DeckID) REFERENCES Deck(DeckID),
    FOREIGN KEY (ShipID) REFERENCES Ship(ShipID)
);

-- Employee/Crew
CREATE TABLE JobPosition (
    PositionID INT PRIMARY KEY AUTO_INCREMENT,
    Title VARCHAR(50) NOT NULL
);

CREATE TABLE Employee (
    EmployeeID INT PRIMARY KEY AUTO_INCREMENT,
    FirstName VARCHAR(50) NOT NULL,
    LastName VARCHAR(50) NOT NULL,
    PositionID INT,
    HireDate DATE,
    FOREIGN KEY (PositionID) REFERENCES JobPosition(PositionID)
);

CREATE TABLE Captain (
    CaptainID INT PRIMARY KEY AUTO_INCREMENT,
    EmployeeID INT,
    LicenseNumber VARCHAR(50) NOT NULL,
    FOREIGN KEY (EmployeeID) REFERENCES Employee(EmployeeID)
);

CREATE TABLE CaptainShipType (
    CaptainShipTypeID INT PRIMARY KEY AUTO_INCREMENT,
    CaptainID INT,
    ShipTypeID INT,
    FOREIGN KEY (CaptainID) REFERENCES Captain(CaptainID),
    FOREIGN KEY (ShipTypeID) REFERENCES ShipType(ShipTypeID)
);

-- Crew-specific assignments
CREATE TABLE ShipCrew (
    AssignmentID INT PRIMARY KEY AUTO_INCREMENT,
    EmployeeID INT,
    ShipID INT,
    StartDate DATE,
    EndDate DATE,
    FOREIGN KEY (EmployeeID) REFERENCES Employee(EmployeeID),
    FOREIGN KEY (ShipID) REFERENCES Ship(ShipID)
);

CREATE TABLE CrewCabin (
    CrewCabinID INT PRIMARY KEY AUTO_INCREMENT,
    EmployeeID INT,
    CabinID INT,
    AssignedDate DATE,
    FOREIGN KEY (EmployeeID) REFERENCES Employee(EmployeeID),
    FOREIGN KEY (CabinID) REFERENCES Cabin(CabinID)
);

-- Work schedule
CREATE TABLE WorkSchedule (
    ScheduleID INT PRIMARY KEY AUTO_INCREMENT,
    EmployeeID INT,
    ShipID INT,
    WorkDate DATE,
    ShiftStart TIME,
    ShiftEnd TIME,
    FOREIGN KEY (EmployeeID) REFERENCES Employee(EmployeeID),
    FOREIGN KEY (ShipID) REFERENCES Ship(ShipID)
);

-- Maintenance
CREATE TABLE Maintenance (
    MaintenanceID INT PRIMARY KEY AUTO_INCREMENT,
    MaintenanceName VARCHAR(100) NOT NULL,
    ShipID INT,
    FOREIGN KEY (ShipID) REFERENCES Ship(ShipID)
);

CREATE TABLE MaintenanceRecord (
    RecordID INT PRIMARY KEY AUTO_INCREMENT,
    MaintenanceID INT,
    MaintenanceDate DATE,
    EmployeeID INT,
    Notes TEXT,
    FOREIGN KEY (MaintenanceID) REFERENCES Maintenance(MaintenanceID),
    FOREIGN KEY (EmployeeID) REFERENCES Employee(EmployeeID)
);

-- Ports & voyages
CREATE TABLE Port (
    PortID INT PRIMARY KEY AUTO_INCREMENT,
    PortName VARCHAR(100) NOT NULL,
    Country VARCHAR(50)
);

CREATE TABLE DockAssignment (
    DockAssignmentID INT PRIMARY KEY AUTO_INCREMENT,
    ShipID INT,
    PortID INT,
    DockDate DATE NOT NULL,
    BerthNumber VARCHAR(20),
    FOREIGN KEY (ShipID) REFERENCES Ship(ShipID),
    FOREIGN KEY (PortID) REFERENCES Port(PortID)
);

CREATE TABLE Itinerary (
    ItineraryID INT PRIMARY KEY AUTO_INCREMENT,
    ItineraryName VARCHAR(100) NOT NULL
);

CREATE TABLE Stop (
    StopID INT PRIMARY KEY AUTO_INCREMENT,
    StopOrder INT NOT NULL,
    ItineraryID INT,
    PortID INT,
    FOREIGN KEY (ItineraryID) REFERENCES Itinerary(ItineraryID),
    FOREIGN KEY (PortID) REFERENCES Port(PortID)
);

CREATE TABLE Season (
    SeasonID INT PRIMARY KEY AUTO_INCREMENT,
    SeasonName VARCHAR(30) NOT NULL
);

CREATE TABLE Voyage (
    VoyageID INT PRIMARY KEY AUTO_INCREMENT,
    DepartureDate DATE NOT NULL,
    ReturnDate DATE,
    ShipID INT,
    ItineraryID INT,
    SeasonID INT,
    FOREIGN KEY (ShipID) REFERENCES Ship(ShipID),
    FOREIGN KEY (ItineraryID) REFERENCES Itinerary(ItineraryID),
    FOREIGN KEY (SeasonID) REFERENCES Season(SeasonID)
);

-- Passengers & reservations
CREATE TABLE Passenger (
    PassengerID INT PRIMARY KEY AUTO_INCREMENT,
    FirstName VARCHAR(50) NOT NULL,
    LastName VARCHAR(50) NOT NULL,
    Email VARCHAR(100),
    Phone VARCHAR(20)
);

CREATE TABLE Reservation (
    ReservationID INT PRIMARY KEY AUTO_INCREMENT,
    ReservationDate DATE NOT NULL,
    Status VARCHAR(20),
    PassengerID INT,
    VoyageID INT,
    CabinID INT,
    FOREIGN KEY (PassengerID) REFERENCES Passenger(PassengerID),
    FOREIGN KEY (VoyageID) REFERENCES Voyage(VoyageID),
    FOREIGN KEY (CabinID) REFERENCES Cabin(CabinID)
);

CREATE TABLE Ticket (
    TicketID INT PRIMARY KEY AUTO_INCREMENT,
    IssueDate DATE NOT NULL,
    ReservationID INT,
    TicketPrice DECIMAL(10,2),
    FOREIGN KEY (ReservationID) REFERENCES Reservation(ReservationID)
);

CREATE TABLE Payment (
    PaymentID INT PRIMARY KEY AUTO_INCREMENT,
    Amount DECIMAL(10,2) NOT NULL,
    PaymentDate DATE NOT NULL,
    PaymentMethod VARCHAR(30),
    ReservationID INT,
    FOREIGN KEY (ReservationID) REFERENCES Reservation(ReservationID)
);

-- Excursions
CREATE TABLE Excursion (
    ExcursionID INT PRIMARY KEY AUTO_INCREMENT,
    ExcursionName VARCHAR(100) NOT NULL,
    Price DECIMAL(10,2),
    PortID INT,
    SeasonID INT,
    FOREIGN KEY (PortID) REFERENCES Port(PortID),
    FOREIGN KEY (SeasonID) REFERENCES Season(SeasonID)
);

CREATE TABLE ReservationExcursion (
    ReservationExcursionID INT PRIMARY KEY AUTO_INCREMENT,
    ReservationID INT,
    ExcursionID INT,
    FOREIGN KEY (ReservationID) REFERENCES Reservation(ReservationID),
    FOREIGN KEY (ExcursionID) REFERENCES Excursion(ExcursionID)
);

-- Onboard amenities
CREATE TABLE DiningVenue (
    DiningVenueID INT PRIMARY KEY AUTO_INCREMENT,
    VenueName VARCHAR(100) NOT NULL,
    ShipID INT,
    Capacity INT,
    FOREIGN KEY (ShipID) REFERENCES Ship(ShipID)
);

CREATE TABLE Facility (
    FacilityID INT PRIMARY KEY AUTO_INCREMENT,
    FacilityName VARCHAR(100) NOT NULL,
    ShipID INT,
    FOREIGN KEY (ShipID) REFERENCES Ship(ShipID)
);

CREATE TABLE EntertainmentEvent (
    EventID INT PRIMARY KEY AUTO_INCREMENT,
    EventName VARCHAR(100) NOT NULL,
    EventDateTime DATETIME,
    Venue VARCHAR(100),
    ShipID INT,
    FOREIGN KEY (ShipID) REFERENCES Ship(ShipID)
);

-- Suppliers & supplies
CREATE TABLE Supplier (
    SupplierID INT PRIMARY KEY AUTO_INCREMENT,
    SupplierName VARCHAR(100) NOT NULL,
    ContactName VARCHAR(100),
    Phone VARCHAR(20),
    Email VARCHAR(100)
);

CREATE TABLE Supplies (
    SupplyID INT PRIMARY KEY AUTO_INCREMENT,
    SupplyName VARCHAR(100) NOT NULL,
    QuantityInStock INT,
    ShipID INT,
    SupplierID INT,
    FOREIGN KEY (ShipID) REFERENCES Ship(ShipID),
    FOREIGN KEY (SupplierID) REFERENCES Supplier(SupplierID)
);

CREATE TABLE EmergencyDrill (
    DrillID INT PRIMARY KEY AUTO_INCREMENT,
    DrillDate DATE NOT NULL,
    DrillType VARCHAR(50),
    ShipID INT,
    FOREIGN KEY (ShipID) REFERENCES Ship(ShipID)
);

-- Passenger accounts for portal login
CREATE TABLE PassengerAccount (
    AccountID    INT PRIMARY KEY AUTO_INCREMENT,
    Username     VARCHAR(50) NOT NULL UNIQUE,
    PasswordHash VARCHAR(64) NOT NULL,
    PassengerID  INT NOT NULL,
    FOREIGN KEY (PassengerID) REFERENCES Passenger(PassengerID)
);

-- Sample INSERT data (for testing)
INSERT INTO ShipType (TypeName) VALUES ('Cruise'), ('Luxury'), ('Expedition');
INSERT INTO JobPosition (Title) VALUES ('Captain'), ('Engineer'), ('Steward'), ('Chef');

INSERT INTO Ship (ShipName, Capacity, ShipTypeID) 
VALUES ('Ocean Pearl', 2500, 1), ('Star Voyager', 1800, 2);

-- Decks: Ocean Pearl (ShipID=1) gets 3, Star Voyager (ShipID=2) gets 2
INSERT INTO Deck (DeckNumber, ShipID) VALUES (1, 1), (2, 1), (3, 1), (1, 2), (2, 2);

-- Cabins (DeckIDs 1-3=Ocean Pearl, 4-5=Star Voyager)
INSERT INTO Cabin (CabinNumber, CabinType, DeckID, ShipID) VALUES
('101', 'Interior',   1, 1), ('102', 'Interior',   1, 1),
('201', 'Ocean View', 2, 1), ('202', 'Ocean View', 2, 1),
('301', 'Balcony',    3, 1), ('302', 'Suite',       3, 1),
('101', 'Interior',   4, 2), ('201', 'Ocean View',  5, 2),
('202', 'Balcony',    5, 2);

-- Employees
INSERT INTO Employee (FirstName, LastName, PositionID, HireDate) VALUES
('Montgomery', 'Scott',   2, '2019-06-10'),
('Nyota',      'Uhura',   3, '2021-03-22'),
('Antoine',    'Dubois',  4, '2018-09-05'),
('Maria',      'Santos',  3, '2022-07-14'),
('David',      'Chen',    2, '2020-11-30'),
('James',      'Kirk',    1, '2015-01-15'),
('Jean-Luc',   'Picard',  1, '2012-03-20');

INSERT INTO Port (PortName, Country) VALUES ('Miami', 'USA'), ('Nassau', 'Bahamas'), ('Cozumel', 'Mexico'), ('Grand Cayman', 'Cayman Islands');

INSERT INTO Season (SeasonName) VALUES ('Spring'), ('Summer'), ('Fall'), ('Winter');

INSERT INTO Itinerary (ItineraryName) VALUES ('Caribbean Classic'), ('Bahamas Getaway');

INSERT INTO Stop (StopOrder, ItineraryID, PortID) VALUES
(1, 1, 1), (2, 1, 2), (3, 1, 3),
(1, 2, 1), (2, 2, 2);

INSERT INTO Voyage (DepartureDate, ReturnDate, ShipID, ItineraryID, SeasonID) VALUES
('2026-06-15', '2026-06-22', 1, 1, 2),
('2026-07-10', '2026-07-17', 2, 2, 2),
('2026-12-20', '2026-12-27', 1, 2, 4);

-- ShipCrew assignments
INSERT INTO ShipCrew (EmployeeID, ShipID, StartDate, EndDate) VALUES
(1, 1, '2019-06-10', NULL),
(2, 1, '2021-03-22', NULL),
(3, 1, '2018-09-05', NULL),
(4, 2, '2022-07-14', NULL),
(5, 2, '2020-11-30', NULL);

-- CrewCabin assignments
INSERT INTO CrewCabin (EmployeeID, CabinID, AssignedDate) VALUES
(1, 1, '2019-06-10'),
(2, 2, '2021-03-22'),
(4, 7, '2022-07-14'),
(5, 8, '2020-11-30');

-- Work schedules
INSERT INTO WorkSchedule (EmployeeID, ShipID, WorkDate, ShiftStart, ShiftEnd) VALUES
(1, 1, '2026-06-15', '07:00', '15:00'),
(2, 1, '2026-06-15', '10:00', '18:00'),
(4, 2, '2026-07-10', '08:00', '16:00'),
(5, 2, '2026-07-10', '07:00', '15:00');

-- Maintenance
INSERT INTO Maintenance (MaintenanceName, ShipID) VALUES
('Engine Inspection',   1),
('Hull Cleaning',       1),
('HVAC Servicing',      2),
('Navigation Systems',  2);

-- Maintenance records
INSERT INTO MaintenanceRecord (MaintenanceID, MaintenanceDate, EmployeeID, Notes) VALUES
(1, '2026-03-10', 1, 'All systems checked, minor valve replaced.'),
(2, '2026-04-05', 1, 'Hull cleaned and repainted below waterline.'),
(3, '2026-02-20', 5, 'Filters replaced, system running normally.'),
(4, '2026-03-15', 5, 'Radar calibrated and software updated.');

-- Passengers
INSERT INTO Passenger (FirstName, LastName, Email, Phone) VALUES
('John',  'Smith',  'john.smith@email.com',  '410-555-0101'),
('Sarah', 'Johnson','sarah.j@email.com',     '443-555-0102'),
('Carlos','Rivera', 'c.rivera@email.com',    '301-555-0103');

-- Reservations (VoyageIDs: 1=June Ocean Pearl, 2=July Star Voyager, 3=Dec Ocean Pearl)
INSERT INTO Reservation (ReservationDate, Status, PassengerID, VoyageID, CabinID) VALUES
('2026-04-01', 'Confirmed', 1, 1, 1),
('2026-04-15', 'Confirmed', 2, 2, 7),
('2026-05-01', 'Confirmed', 3, 3, 5);

-- Tickets
INSERT INTO Ticket (IssueDate, ReservationID, TicketPrice) VALUES
('2026-04-01', 1, 1299.00),
('2026-04-15', 2,  999.00),
('2026-05-01', 3, 1499.00);

-- Payments
INSERT INTO Payment (Amount, PaymentDate, PaymentMethod, ReservationID) VALUES
(1299.00, '2026-04-01', 'Credit Card', 1),
( 999.00, '2026-04-15', 'Debit Card',  2),
( 749.50, '2026-05-01', 'Credit Card', 3),
( 749.50, '2026-05-10', 'Credit Card', 3);

-- Excursions (SeasonID NULL = available all seasons)
INSERT INTO Excursion (ExcursionName, Price, PortID, SeasonID) VALUES
('Nassau Snorkeling Tour',     79.00, 2, 2),
('Cozumel Mayan Ruins',       120.00, 3, NULL),
('Grand Cayman Stingray City', 95.00, 4, NULL),
('Nassau City Walking Tour',   45.00, 2, NULL),
('Nassau Holiday Festival',    55.00, 2, 4);

-- Reservation excursions
INSERT INTO ReservationExcursion (ReservationID, ExcursionID) VALUES
(1, 1), (1, 2), (2, 1), (3, 3);

-- Dining venues
INSERT INTO DiningVenue (VenueName, ShipID, Capacity) VALUES
('The Grand Dining Room', 1, 400),
('Cafe Soleil',           1,  80),
('The Pearl Buffet',      1, 250),
('Star Grill',            2, 300),
('Voyager Cafe',          2,  60);

-- Facilities
INSERT INTO Facility (FacilityName, ShipID) VALUES
('Main Pool Deck',   1),
('Fitness Center',   1),
('Full-Service Spa', 1),
('Casino',           1),
('Pool Deck',        2),
('Spa & Wellness',   2);

-- Entertainment events
INSERT INTO EntertainmentEvent (EventName, EventDateTime, Venue, ShipID) VALUES
('Caribbean Night Show',  '2026-06-17 20:00:00', 'Main Theater',      1),
('Live Jazz Evening',     '2026-06-18 19:30:00', 'Cafe Soleil',        1),
('Trivia Night',          '2026-06-19 21:00:00', 'Sky Lounge',         1),
('Welcome Gala',          '2026-07-11 19:00:00', 'Star Ballroom',      2),
('Movie Under the Stars', '2026-07-12 21:00:00', 'Pool Deck',          2);

-- Suppliers
INSERT INTO Supplier (SupplierName, ContactName, Phone, Email) VALUES
('Maritime Safety Co.',   'Robert Lane',  '555-0201', 'rlane@msafety.com'),
('Ocean Provisions Ltd.', 'Alice Moreau', '555-0202', 'amoreau@oceanprov.com'),
('MedSupply Marine',      'Carlos Ruiz',  '555-0203', 'cruiz@medsupply.com');

-- Supplies
INSERT INTO Supplies (SupplyName, QuantityInStock, ShipID, SupplierID) VALUES
('Life Jackets',      3000, 1, 1),
('First Aid Kits',      50, 1, 3),
('Fire Extinguishers', 120, 1, 1),
('Sunscreen (SPF 50)', 500, 1, 2),
('Life Jackets',      2200, 2, 1),
('First Aid Kits',      40, 2, 3),
('Fire Extinguishers',  90, 2, 1);

-- Captains and ship type certifications (EmployeeIDs 6=Kirk, 7=Picard)
INSERT INTO Captain (EmployeeID, LicenseNumber) VALUES
(6, 'CAPT-US-001'),
(7, 'CAPT-FR-002');

INSERT INTO CaptainShipType (CaptainID, ShipTypeID) VALUES
(1, 1), (1, 2),
(2, 1), (2, 3);

-- Dock assignments
INSERT INTO DockAssignment (ShipID, PortID, DockDate, BerthNumber) VALUES
(1, 1, '2026-06-15', 'A-1'),
(1, 2, '2026-06-17', 'B-3'),
(1, 3, '2026-06-19', 'C-2'),
(2, 1, '2026-07-10', 'A-2'),
(2, 2, '2026-07-12', 'B-1');

-- Emergency drills
INSERT INTO EmergencyDrill (DrillDate, DrillType, ShipID) VALUES
('2026-06-15', 'Muster Drill',       1),
('2026-06-16', 'Fire Drill',         1),
('2026-07-10', 'Muster Drill',       2),
('2026-07-11', 'Evacuation Drill',   2);