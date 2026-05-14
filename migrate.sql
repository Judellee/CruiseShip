-- Safe migration: adds new tables and columns without dropping anything

CREATE TABLE IF NOT EXISTS Supplier (
    SupplierID INT PRIMARY KEY AUTO_INCREMENT,
    SupplierName VARCHAR(100) NOT NULL,
    ContactName VARCHAR(100),
    Phone VARCHAR(20),
    Email VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS Captain (
    CaptainID INT PRIMARY KEY AUTO_INCREMENT,
    EmployeeID INT,
    LicenseNumber VARCHAR(50) NOT NULL,
    FOREIGN KEY (EmployeeID) REFERENCES Employee(EmployeeID)
);

CREATE TABLE IF NOT EXISTS CaptainShipType (
    CaptainShipTypeID INT PRIMARY KEY AUTO_INCREMENT,
    CaptainID INT,
    ShipTypeID INT,
    FOREIGN KEY (CaptainID) REFERENCES Captain(CaptainID),
    FOREIGN KEY (ShipTypeID) REFERENCES ShipType(ShipTypeID)
);

CREATE TABLE IF NOT EXISTS DockAssignment (
    DockAssignmentID INT PRIMARY KEY AUTO_INCREMENT,
    ShipID INT,
    PortID INT,
    DockDate DATE NOT NULL,
    BerthNumber VARCHAR(20),
    FOREIGN KEY (ShipID) REFERENCES Ship(ShipID),
    FOREIGN KEY (PortID) REFERENCES Port(PortID)
);

ALTER TABLE Excursion ADD COLUMN SeasonID INT,
    ADD FOREIGN KEY (SeasonID) REFERENCES Season(SeasonID);

ALTER TABLE Supplies ADD COLUMN SupplierID INT,
    ADD FOREIGN KEY (SupplierID) REFERENCES Supplier(SupplierID);

INSERT IGNORE INTO Supplier (SupplierID, SupplierName, ContactName, Phone, Email) VALUES
(1, 'Maritime Safety Co.',   'Robert Lane',  '555-0201', 'rlane@msafety.com'),
(2, 'Ocean Provisions Ltd.', 'Alice Moreau', '555-0202', 'amoreau@oceanprov.com'),
(3, 'MedSupply Marine',      'Carlos Ruiz',  '555-0203', 'cruiz@medsupply.com');

UPDATE Excursion SET SeasonID = 2 WHERE ExcursionName = 'Nassau Snorkeling Tour'  AND SeasonID IS NULL;
UPDATE Excursion SET SeasonID = 4 WHERE ExcursionName = 'Nassau Holiday Festival' AND SeasonID IS NULL;

INSERT INTO Excursion (ExcursionName, Price, PortID, SeasonID)
SELECT 'Nassau Holiday Festival', 55.00, PortID, 4
FROM Port WHERE PortName = 'Nassau'
AND NOT EXISTS (SELECT 1 FROM Excursion WHERE ExcursionName = 'Nassau Holiday Festival');

INSERT IGNORE INTO DockAssignment (ShipID, PortID, DockDate, BerthNumber) VALUES
(1, 1, '2026-06-15', 'A-1'),
(1, 2, '2026-06-17', 'B-3'),
(1, 3, '2026-06-19', 'C-2'),
(2, 1, '2026-07-10', 'A-2'),
(2, 2, '2026-07-12', 'B-1');

INSERT INTO Employee (FirstName, LastName, PositionID, HireDate)
SELECT 'Jean-Luc', 'Picard', 1, '2012-03-20'
WHERE NOT EXISTS (SELECT 1 FROM Employee WHERE FirstName='Jean-Luc' AND LastName='Picard');

INSERT INTO Captain (EmployeeID, LicenseNumber)
SELECT EmployeeID, 'CAPT-FR-002' FROM Employee WHERE FirstName='Jean-Luc' AND LastName='Picard'
AND NOT EXISTS (SELECT 1 FROM Captain WHERE LicenseNumber='CAPT-FR-002');

INSERT IGNORE INTO CaptainShipType (CaptainID, ShipTypeID)
SELECT c.CaptainID, st.ShipTypeID
FROM Captain c JOIN Employee e ON c.EmployeeID=e.EmployeeID
JOIN ShipType st ON st.TypeName IN ('Cruise','Expedition')
WHERE e.LastName='Picard'
AND NOT EXISTS (SELECT 1 FROM CaptainShipType x WHERE x.CaptainID=c.CaptainID AND x.ShipTypeID=st.ShipTypeID);
