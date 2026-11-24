USE college_parking;

SELECT v.vehicle_number,
       v.owner_name,
       v.owner_category,
       s.slot_number,
       r.entry_time
FROM parking_records AS r
JOIN vehicles AS v ON v.vehicle_id = r.vehicle_id
JOIN parking_slots AS s ON s.slot_id = r.slot_id
WHERE r.exit_time IS NULL
ORDER BY r.entry_time DESC;
