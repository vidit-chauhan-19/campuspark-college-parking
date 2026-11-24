package web;

import dao.ParkingRecordDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.ParkingRecord;
import model.ParkingSlot;
import model.Vehicle;
import service.ParkingService;
import service.VehicleService;
import util.DBConnection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet(urlPatterns = "/api/*")
public class ParkingApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Pattern VEHICLE_PATH = Pattern.compile("/vehicles/(\\d+)");
    private static final Pattern PARKING_PATH = Pattern.compile("/parking/(\\d+)/(park|exit)");
    private final ParkingService parking = new ParkingService();
    private final VehicleService vehicles = new VehicleService();
    private final ParkingRecordDAO recordDao = new ParkingRecordDAO();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        try {
            switch (path) {
                case "/health":
                    try (Connection ignored = DBConnection.getConnection()) {
                        send(resp, 200, map("ok", true, "database", "college_parking"));
                    }
                    return;
                case "/dashboard":
                    send(resp, 200, dashboard());
                    return;
                case "/vehicles":
                    send(resp, 200, vehicleList(vehicles.searchVehicle(req.getParameter("q"))));
                    return;
                case "/records":
                    send(resp, 200, recordList(parking.searchRecords(req.getParameter("q"))));
                    return;
                case "/slots":
                    List<Map<String, Object>> slots = new ArrayList<>();
                    for (ParkingSlot slot : parking.getAllSlots()) slots.add(slotJson(slot));
                    send(resp, 200, slots);
                    return;
                default:
                    send(resp, 404, map("error", "API route not found"));
            }
        } catch (Exception ex) { fail(resp, ex); }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        try {
            if ("/vehicles".equals(path)) {
                Vehicle v = readVehicle(req, 0);
                vehicles.addVehicle(v);
                send(resp, 201, map("ok", true));
                return;
            }
            Matcher match = PARKING_PATH.matcher(path);
            if (match.matches()) {
                int id = Integer.parseInt(match.group(1));
                String slot = "park".equals(match.group(2)) ? parking.parkVehicle(id) : parking.exitVehicle(id);
                send(resp, 200, map("ok", true, "slotNumber", slot));
                return;
            }
            send(resp, 404, map("error", "API route not found"));
        } catch (Exception ex) { fail(resp, ex); }
    }

    @Override protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Matcher match = VEHICLE_PATH.matcher(req.getPathInfo() == null ? "" : req.getPathInfo());
        if (!match.matches()) { send(resp, 404, map("error", "API route not found")); return; }
        try {
            vehicles.updateVehicle(readVehicle(req, Integer.parseInt(match.group(1))));
            send(resp, 200, map("ok", true));
        } catch (Exception ex) { fail(resp, ex); }
    }

    @Override protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Matcher match = VEHICLE_PATH.matcher(req.getPathInfo() == null ? "" : req.getPathInfo());
        if (!match.matches()) { send(resp, 404, map("error", "API route not found")); return; }
        try {
            vehicles.deleteVehicle(Integer.parseInt(match.group(1)));
            send(resp, 200, map("ok", true));
        } catch (Exception ex) { fail(resp, ex); }
    }

    private Map<String, Object> dashboard() throws SQLException {
        List<String[]> groups = parking.getSummary();
        List<Map<String, Object>> summary = new ArrayList<>();
        int total = 0, occupied = 0;
        for (String[] group : groups) {
            int groupTotal = Integer.parseInt(group[2]);
            int groupOccupied = Integer.parseInt(group[3]);
            total += groupTotal;
            occupied += groupOccupied;
            summary.add(map("category", group[0], "vehicleType", group[1], "total", groupTotal,
                "occupied", groupOccupied, "available", Integer.parseInt(group[4])));
        }
        List<Vehicle> parked = parking.getParkedVehicles();
        Map<Integer, String> slotsByVehicle = new HashMap<>();
        for (ParkingRecord record : recordDao.getAll()) {
            if (record.getExitTime() == null) slotsByVehicle.put(record.getVehicleId(), record.getSlotNumber());
        }
        List<Map<String, Object>> active = new ArrayList<>();
        Set<Integer> parkedIds = new HashSet<>();
        for (Vehicle vehicle : parked) {
            parkedIds.add(vehicle.getVehicleId());
            active.add(vehicleJson(vehicle, true, slotsByVehicle.get(vehicle.getVehicleId())));
        }
        List<Map<String, Object>> recent = new ArrayList<>();
        List<ParkingRecord> allRecords = parking.getRecords();
        for (int i = 0; i < Math.min(5, allRecords.size()); i++) recent.add(recordJson(allRecords.get(i)));
        Map<String, Object> result = map("totalSlots", total, "occupiedSlots", occupied,
            "availableSlots", total - occupied, "parkedCount", parked.size(),
            "registeredVehicles", vehicles.getAllVehicles().size(), "summary", summary,
            "parkedVehicles", active, "recentRecords", recent);
        return result;
    }

    private List<Map<String, Object>> vehicleList(List<Vehicle> list) throws SQLException {
        Set<Integer> parkedIds = new HashSet<>();
        Map<Integer, String> slots = new HashMap<>();
        for (Vehicle v : parking.getParkedVehicles()) parkedIds.add(v.getVehicleId());
        for (ParkingRecord r : recordDao.getAll()) if (r.getExitTime() == null) slots.put(r.getVehicleId(), r.getSlotNumber());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Vehicle v : list) result.add(vehicleJson(v, parkedIds.contains(v.getVehicleId()), slots.get(v.getVehicleId())));
        return result;
    }

    private List<Map<String, Object>> recordList(List<ParkingRecord> list) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ParkingRecord r : list) result.add(recordJson(r));
        return result;
    }

    private Map<String, Object> vehicleJson(Vehicle v, boolean parked, String slot) {
        return map("vehicleId", v.getVehicleId(), "vehicleNumber", v.getVehicleNumber(),
            "vehicleType", v.getVehicleType(), "ownerCategory", v.getOwnerCategory(),
            "ownerName", v.getOwnerName(), "parked", parked, "slotNumber", slot);
    }

    private Map<String, Object> slotJson(ParkingSlot s) {
        return map("slotId", s.getSlotId(), "slotNumber", s.getSlotNumber(), "slotType", s.getSlotType(),
            "category", s.getUserCategory(), "occupied", s.isOccupied());
    }

    private Map<String, Object> recordJson(ParkingRecord r) {
        return map("recordId", r.getRecordId(), "vehicleId", r.getVehicleId(),
            "vehicleNumber", r.getVehicleNumber(), "slotNumber", r.getSlotNumber(),
            "entryTime", r.getEntryTime() == null ? null : r.getEntryTime().toString(),
            "exitTime", r.getExitTime() == null ? null : r.getExitTime().toString(),
            "active", r.getExitTime() == null);
    }

    private Vehicle readVehicle(HttpServletRequest req, int id) {
        Vehicle v = new Vehicle();
        v.setVehicleId(id);
        v.setVehicleNumber(req.getParameter("vehicleNumber"));
        v.setVehicleType(req.getParameter("vehicleType"));
        v.setOwnerCategory(req.getParameter("ownerCategory"));
        v.setOwnerName(req.getParameter("ownerName"));
        v.setUserId(0);
        return v;
    }

    private void fail(HttpServletResponse resp, Exception ex) throws IOException {
        getServletContext().log("CampusPark API request failed", ex);
        int status = ex instanceof IllegalArgumentException ? 400
            : ex instanceof IllegalStateException ? 409
            : ex instanceof SQLException ? 503 : 500;
        String message = ex instanceof SQLException
            ? "Database request failed. Check MySQL and the database settings in start-server.ps1."
            : ex.getMessage();
        send(resp, status, map("error", message == null ? "Request failed" : message));
    }

    private void send(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write(Json.encode(body));
    }

    private static Map<String, Object> map(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }

    private static final class Json {
        static String encode(Object value) {
            if (value == null) return "null";
            if (value instanceof String) return quote((String) value);
            if (value instanceof Number || value instanceof Boolean) return value.toString();
            if (value instanceof Map) {
                StringBuilder out = new StringBuilder("{");
                boolean first = true;
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                    if (!first) out.append(',');
                    first = false;
                    out.append(quote(String.valueOf(entry.getKey()))).append(':').append(encode(entry.getValue()));
                }
                return out.append('}').toString();
            }
            if (value instanceof Iterable) {
                StringBuilder out = new StringBuilder("[");
                boolean first = true;
                for (Object item : (Iterable<?>) value) {
                    if (!first) out.append(',');
                    first = false;
                    out.append(encode(item));
                }
                return out.append(']').toString();
            }
            return quote(value.toString());
        }
        private static String quote(String value) {
            StringBuilder out = new StringBuilder("\"");
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                    case '"': out.append("\\\""); break;
                    case '\\': out.append("\\\\"); break;
                    case '\n': out.append("\\n"); break;
                    case '\r': out.append("\\r"); break;
                    case '\t': out.append("\\t"); break;
                    default: if (c < 32) out.append(String.format("\\u%04x", (int)c)); else out.append(c);
                }
            }
            return out.append('"').toString();
        }
    }
}




