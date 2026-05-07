-- =============================================
-- CRUISE SHIP MANAGEMENT SYSTEM
-- Based on ERD from group diagram
-- =============================================
USE COSC457;

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
    FOREIGN KEY (PortID) REFERENCES Port(PortID)
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

-- Supplies & drills
CREATE TABLE Supplies (
    SupplyID INT PRIMARY KEY AUTO_INCREMENT,
    SupplyName VARCHAR(100) NOT NULL,
    QuantityInStock INT,
    ShipID INT,
    FOREIGN KEY (ShipID) REFERENCES Ship(ShipID)
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

INSERT INTO Deck (DeckNumber, ShipID) VALUES (1, 1), (2, 1), (3, 1);
INSERT INTO Cabin (CabinNumber, CabinType, DeckID, ShipID) 
VALUES ('101', 'Interior', 1, 1), ('202', 'Ocean View', 2, 1);

INSERT INTO Employee (FirstName, LastName, PositionID, HireDate) 
VALUES ('James', 'Kirk', 1, '2020-01-15'), ('Montgomery', 'Scott', 2, '2019-06-10');

INSERT INTO Port (PortName, Country) VALUES ('Miami', 'USA'), ('Nassau', 'Bahamas');

-- Sample query: Show all crew on ShipID = 1
SELECT e.FirstName, e.LastName, j.Title 
FROM Employee e
JOIN ShipCrew sc ON e.EmployeeID = sc.EmployeeID
JOIN JobPosition j ON e.PositionID = j.PositionID
WHERE sc.ShipID = 1;