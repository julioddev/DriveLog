package com.example.drivelog;

import android.content.Context;
import android.net.Uri;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CsvHelper {

    public static String exportToCsv(Context context, Uri uri) throws Exception {
        AppDao dao = AppDatabase.getInstance(context).appDao();
        StringBuilder sb = new StringBuilder();
        
        int countEarnings = 0;
        int countFuel = 0;
        int countKm = 0;
        int countMaintenance = 0;
        int countRoutes = 0;
        int countStops = 0;

        // EARNINGS
        sb.append("TABLE:EARNINGS\n");
        sb.append("id,baseValue,extraValue,totalValue,platforms,date\n");
        List<Earnings> earnings = dao.getAllEarnings();
        countEarnings = earnings.size();
        for (Earnings e : earnings) {
            String platforms = (e.platforms != null) ? e.platforms.replace("\"", "\"\"") : "";
            sb.append(String.format(Locale.US, "%d,%.2f,%.2f,%.2f,\"%s\",%d\n",
                    e.id, e.baseValue, e.extraValue, e.totalValue, platforms, e.date));
        }

        // FUEL
        sb.append("\nTABLE:FUEL\n");
        sb.append("id,value,pricePerLiter,liters,km,kmDriven,date,isCompleted,fuelType,gasStation\n");
        List<Fuel> fuels = dao.getAllFuel();
        countFuel = fuels.size();
        for (Fuel f : fuels) {
            sb.append(String.format(Locale.US, "%d,%.2f,%.2f,%.2f,%d,%.2f,%d,%b,\"%s\",\"%s\"\n",
                    f.id, f.value, f.pricePerLiter, f.liters, f.km, f.kmDriven, f.date, f.isCompleted,
                    f.fuelType != null ? f.fuelType : "", f.gasStation != null ? f.gasStation : ""));
        }

        // DAILY_KM
        sb.append("\nTABLE:DAILY_KM\n");
        sb.append("id,kmStart,kmEnd,totalKm,estimatedFuelCost,consumptionUsed,date,isCompleted,gpsDistance,isAutomatic\n");
        List<DailyKm> kms = dao.getAllDailyKm();
        countKm = kms.size();
        for (DailyKm d : kms) {
            sb.append(String.format(Locale.US, "%d,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%b,%.8f,%b\n",
                    d.id, d.kmStart, d.kmEnd, d.totalKm, d.estimatedFuelCost, d.consumptionUsed, d.date, d.isCompleted,
                    d.gpsDistance, d.isAutomatic));
        }

        // MAINTENANCE
        sb.append("\nTABLE:MAINTENANCE\n");
        sb.append("id,description,value,date,km,type,intervalKm,alertKm\n");
        List<Maintenance> maintenances = dao.getAllMaintenance();
        countMaintenance = maintenances.size();
        for (Maintenance m : maintenances) {
            String desc = (m.description != null) ? m.description.replace("\"", "\"\"") : "";
            String type = (m.type != null) ? m.type.replace("\"", "\"\"") : "";
            sb.append(String.format(Locale.US, "%d,\"%s\",%.2f,%d,%d,\"%s\",%d,%d\n",
                    m.id, desc, m.value, m.date, m.km, type, m.intervalKm, m.alertKm));
        }

        // PLATFORMS
        sb.append("\nTABLE:PLATFORMS\n");
        sb.append("id,name,isEnabled,defaultValue,isDefault,orderIndex\n");
        for (Platform p : dao.getAllPlatforms()) {
            String pName = (p.name != null) ? p.name.replace("\"", "\"\"") : "";
            sb.append(String.format(Locale.US, "%d,\"%s\",%b,%.2f,%b,%d\n",
                    p.id, pName, p.isEnabled, p.defaultValue, p.isDefault, p.orderIndex));
        }

        // GAS_STATIONS
        sb.append("\nTABLE:GAS_STATIONS\n");
        sb.append("id,name,isEnabled,isDefault,orderIndex\n");
        for (GasStation g : dao.getAllGasStations()) {
            String gName = (g.name != null) ? g.name.replace("\"", "\"\"") : "";
            sb.append(String.format(Locale.US, "%d,\"%s\",%b,%b,%d\n",
                    g.id, gName, g.isEnabled, g.isDefault, g.orderIndex));
        }

        // ROUTE_POINTS
        sb.append("\nTABLE:ROUTE_POINTS\n");
        sb.append("dailyKmId,latitude,longitude,timestamp\n");
        for (RoutePoint r : dao.getAllRoutePoints()) {
            sb.append(String.format(Locale.US, "%d,%.8f,%.8f,%d\n",
                    r.dailyKmId, r.latitude, r.longitude, r.timestamp));
        }

        // ROUTE_HEADERS
        sb.append("\nTABLE:ROUTE_HEADERS\n");
        sb.append("id,name,date,isCompleted,failedCount\n");
        List<RouteHeader> routes = dao.getAllRoutes();
        countRoutes = routes.size();
        for (RouteHeader h : routes) {
            String name = (h.name != null) ? h.name.replace("\"", "\"\"") : "Sem nome";
            sb.append(String.format(Locale.US, "%d,\"%s\",%d,%b,%d\n",
                    h.id, name, h.date, h.isCompleted, h.failedCount));
        }

        // ROUTE_GROUPS
        sb.append("\nTABLE:ROUTE_GROUPS\n");
        sb.append("id,name,color,routeId\n");
        for (RouteHeader h : routes) {
            for (RouteGroup g : dao.getGroupsForRoute(h.id)) {
                String gName = (g.name != null) ? g.name.replace("\"", "\"\"") : "Grupo";
                String gColor = (g.color != null) ? g.color.replace("\"", "\"\"") : "#CCCCCC";
                sb.append(String.format(Locale.US, "%d,\"%s\",\"%s\",%d\n",
                        g.id, gName, gColor, g.routeId));
            }
        }

        // ROUTE_STOPS
        sb.append("\nTABLE:ROUTE_STOPS\n");
        sb.append("id,routeId,address,latitude,longitude,deliveryStatus,packageCount,sortOrder,groupId,sequence,allSequences,stopNumber,neighborhood,city,zipcode\n");
        for (RouteHeader h : routes) {
            List<RouteStop> stops = dao.getStopsForRoute(h.id);
            countStops += stops.size();
            for (RouteStop s : stops) {
                String addr = (s.address != null) ? s.address.replace("\"", "\"\"") : "";
                String seqs = (s.allSequences != null) ? s.allSequences.replace("\"", "\"\"") : "";
                String neigh = (s.neighborhood != null) ? s.neighborhood.replace("\"", "\"\"") : "";
                String city = (s.city != null) ? s.city.replace("\"", "\"\"") : "";
                String zip = (s.zipcode != null) ? s.zipcode.replace("\"", "\"\"") : "";
                
                sb.append(String.format(Locale.US, "%d,%d,\"%s\",%.8f,%.8f,%d,%d,%d,%s,%d,\"%s\",%d,\"%s\",\"%s\",\"%s\"\n",
                        s.id, s.routeId, addr, s.latitude, s.longitude,
                        s.deliveryStatus, s.packageCount, s.sortOrder,
                        s.groupId != null ? String.valueOf(s.groupId) : "null",
                        s.sequence, seqs, s.stopNumber, neigh, city, zip));
            }
        }

        // CORRECTED_ADDRESSES
        sb.append("\nTABLE:CORRECTED_ADDRESSES\n");
        sb.append("id,address,latitude,longitude,updatedAt\n");
        for (CorrectedAddress ca : dao.getAllCorrectedAddresses()) {
            String addr = (ca.address != null) ? ca.address.replace("\"", "\"\"") : "";
            sb.append(String.format(Locale.US, "%d,\"%s\",%.8f,%.8f,%d\n",
                    ca.id, addr, ca.latitude, ca.longitude, ca.updatedAt));
        }

        // LOADING_POINTS
        sb.append("\nTABLE:LOADING_POINTS\n");
        sb.append("id,name,latitude,longitude,platformName\n");
        for (LoadingPoint lp : dao.getAllLoadingPoints()) {
            String lpName = (lp.name != null) ? lp.name.replace("\"", "\"\"") : "";
            String platName = (lp.platformName != null) ? lp.platformName.replace("\"", "\"\"") : "";
            sb.append(String.format(Locale.US, "%d,\"%s\",%.8f,%.8f,\"%s\"\n",
                    lp.id, lpName, lp.latitude, lp.longitude, platName));
        }

        // SETTINGS
        sb.append("\nTABLE:SETTINGS\n");
        sb.append("key,value\n");
        android.content.SharedPreferences prefs = context.getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        java.util.Map<String, ?> allEntries = prefs.getAll();
        for (java.util.Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            String value = String.valueOf(entry.getValue());
            sb.append(String.format(Locale.US, "\"%s\",\"%s\"\n",
                    key.replace("\"", "\"\""),
                    value.replace("\"", "\"\"")));
            android.util.Log.d("CsvHelper", "Exportando ajuste: " + key + "=" + value);
        }

        OutputStream out = context.getContentResolver().openOutputStream(uri);
        if (out != null) {
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            out.close();
        }

        return String.format(Locale.getDefault(), 
            "%d Ganhos, %d Abastecimentos, %d Kms, %d Gravações, %d Fixados, %d Pontos GPS",
            countEarnings, countFuel, countKm, countRoutes, dao.getAllCorrectedAddresses().size(), dao.getAllRoutePoints().size());
    }

    public static java.util.Map<String, Integer> peekCsvContents(Context context, Uri uri) throws Exception {
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        InputStream in = context.getContentResolver().openInputStream(uri);
        if (in == null) return counts;

        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        String currentTable = "";
        while ((line = reader.readLine()) != null) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;
            if (trimmedLine.startsWith("TABLE:")) {
                currentTable = trimmedLine.substring(6).trim();
                reader.readLine(); // Skip header
                continue;
            }
            if (!currentTable.isEmpty()) {
                counts.put(currentTable, counts.getOrDefault(currentTable, 0) + 1);
            }
        }
        in.close();
        return counts;
    }

    public static void importFromCsv(Context context, Uri uri) throws Exception {
        importFromCsv(context, uri, null);
    }

    public static void importFromCsv(Context context, Uri uri, java.util.Set<String> tablesToImport) throws Exception {
        InputStream in = context.getContentResolver().openInputStream(uri);
        if (in == null) return;

        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        String currentTable = "";
        
        AppDao dao = AppDatabase.getInstance(context).appDao();
        
        // Se tablesToImport for null, importamos TUDO (limpamos tudo)
        if (tablesToImport == null) {
            dao.clearEarnings();
            dao.clearFuel();
            dao.clearDailyKm();
            dao.clearMaintenance();
            dao.clearRoutePoints();
            dao.clearRouteStops();
            dao.clearRouteHeaders();
            AppDatabase.getInstance(context).getOpenHelper().getWritableDatabase().execSQL("DELETE FROM route_groups");
            dao.clearRouteStops();
            AppDatabase.getInstance(context).getOpenHelper().getWritableDatabase().execSQL("DELETE FROM corrected_addresses");
            dao.clearPlatforms();
            dao.clearGasStations();
        } else {
            // Limpamos apenas o que foi selecionado
            if (tablesToImport.contains("EARNINGS")) dao.clearEarnings();
            if (tablesToImport.contains("FUEL")) dao.clearFuel();
            if (tablesToImport.contains("DAILY_KM")) dao.clearDailyKm();
            if (tablesToImport.contains("MAINTENANCE")) dao.clearMaintenance();
            if (tablesToImport.contains("ROUTE_POINTS")) dao.clearRoutePoints();
            if (tablesToImport.contains("ROUTE_HEADERS")) dao.clearRouteHeaders();
            if (tablesToImport.contains("ROUTE_GROUPS")) { 
                AppDatabase.getInstance(context).getOpenHelper().getWritableDatabase().execSQL("DELETE FROM route_groups");
            }
            if (tablesToImport.contains("ROUTE_STOPS")) dao.clearRouteStops();
            if (tablesToImport.contains("CORRECTED_ADDRESSES")) {
                AppDatabase.getInstance(context).getOpenHelper().getWritableDatabase().execSQL("DELETE FROM corrected_addresses");
            }
            if (tablesToImport.contains("PLATFORMS")) dao.clearPlatforms();
            if (tablesToImport.contains("GAS_STATIONS")) dao.clearGasStations();
        }
        
        int importedCount = 0;

        while ((line = reader.readLine()) != null) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;
            
            if (trimmedLine.startsWith("TABLE:")) {
                currentTable = trimmedLine.substring(6).trim();
                reader.readLine(); // Skip header
                continue;
            }

            // Se for restauração seletiva, verificamos se a tabela atual deve ser importada
            if (tablesToImport != null && !currentTable.equals("SETTINGS")) {
                boolean shouldImport = tablesToImport.contains(currentTable);
                
                // Vínculos automáticos: se quer Gravações, traz grupos e paradas
                if (!shouldImport && tablesToImport.contains("ROUTE_HEADERS")) {
                    if (currentTable.equals("ROUTE_GROUPS") || currentTable.equals("ROUTE_STOPS")) {
                        shouldImport = true;
                    }
                }
                // Se quer KM, traz os pontos GPS (trajetos)
                if (!shouldImport && tablesToImport.contains("DAILY_KM")) {
                    if (currentTable.equals("ROUTE_POINTS")) {
                        shouldImport = true;
                    }
                }
                
                if (!shouldImport) continue;
            }

            String[] parts = parseCsvLine(line);
            if (parts.length < 2) continue;
            
            try {
                switch (currentTable) {
                    case "EARNINGS":
                        if (parts.length >= 5) {
                            Earnings e = new Earnings();
                            e.baseValue = parts.length > 1 ? parseDouble(parts[1]) : 0;
                            e.extraValue = parts.length > 2 ? parseDouble(parts[2]) : 0;
                            e.totalValue = parts.length > 3 ? parseDouble(parts[3]) : 0;
                            e.platforms = parts.length > 4 ? parts[4] : "";
                            e.date = parts.length > 5 ? parseLong(parts[5]) : System.currentTimeMillis();
                            dao.insertEarnings(e);
                            importedCount++;
                        }
                        break;
                    case "FUEL":
                        if (parts.length >= 7) {
                            Fuel f = new Fuel();
                            f.value = parseDouble(parts[1]);
                            f.pricePerLiter = parseDouble(parts[2]);
                            f.liters = parseDouble(parts[3]);
                            f.km = (int) parseDouble(parts[4]);
                            f.kmDriven = parseDouble(parts[5]);
                            f.date = parseLong(parts[6]);
                            f.isCompleted = parts.length > 7 && Boolean.parseBoolean(parts[7]);
                            f.fuelType = parts.length > 8 ? parts[8] : "Aditivada";
                            f.gasStation = parts.length > 9 ? parts[9] : "";
                            dao.insertFuel(f);
                            importedCount++;
                        }
                        break;
                    case "DAILY_KM":
                        if (parts.length >= 8) {
                            DailyKm d = new DailyKm();
                            d.id = (int) parseDouble(parts[0]); // Mantém o ID original para preservar relação com pontos do GPS
                            d.kmStart = parseDouble(parts[1]);
                            d.kmEnd = parts.length > 2 ? parseDouble(parts[2]) : 0;
                            d.totalKm = parts.length > 3 ? parseDouble(parts[3]) : 0;
                            d.estimatedFuelCost = parts.length > 4 ? parseDouble(parts[4]) : 0;
                            d.consumptionUsed = parts.length > 5 ? parseDouble(parts[5]) : 0;
                            d.date = parts.length > 6 ? parseLong(parts[6]) : System.currentTimeMillis();
                            d.isCompleted = parts.length > 7 && Boolean.parseBoolean(parts[7]);
                            d.gpsDistance = parts.length > 8 ? parseDouble(parts[8]) : 0;
                            d.isAutomatic = parts.length > 9 && Boolean.parseBoolean(parts[9]);
                            dao.insertDailyKm(d);
                            importedCount++;
                        }
                        break;
                    case "MAINTENANCE":
                        if (parts.length >= 3) {
                            Maintenance m = new Maintenance();
                            m.description = parts[1];
                            m.value = parseDouble(parts[2]);
                            m.date = parts.length > 3 ? parseLong(parts[3]) : System.currentTimeMillis();
                            m.km = parts.length > 4 ? (int) parseDouble(parts[4]) : 0;
                            m.type = parts.length > 5 ? parts[5] : "Emergencial";
                            m.intervalKm = parts.length > 6 ? (int) parseDouble(parts[6]) : 0;
                            m.alertKm = parts.length > 7 ? (int) parseDouble(parts[7]) : 0;
                            dao.insertMaintenance(m);
                            importedCount++;
                        }
                        break;
                    case "PLATFORMS":
                        if (parts.length >= 2) {
                            Platform p = new Platform();
                            p.name = parts[1];
                            p.isEnabled = parts.length > 2 && Boolean.parseBoolean(parts[2]);
                            p.defaultValue = parts.length > 3 ? parseDouble(parts[3]) : 0;
                            p.isDefault = parts.length > 4 && Boolean.parseBoolean(parts[4]);
                            p.orderIndex = parts.length > 5 ? (int) parseDouble(parts[5]) : 0;
                            dao.insertPlatform(p);
                            importedCount++;
                        }
                        break;
                    case "GAS_STATIONS":
                        if (parts.length >= 2) {
                            GasStation g = new GasStation();
                            g.name = parts[1];
                            g.isEnabled = parts.length > 2 && Boolean.parseBoolean(parts[2]);
                            g.isDefault = parts.length > 3 && Boolean.parseBoolean(parts[3]);
                            g.orderIndex = parts.length > 4 ? (int) parseDouble(parts[4]) : 0;
                            dao.insertGasStation(g);
                            importedCount++;
                        }
                        break;
                    case "ROUTE_POINTS":
                        if (parts.length >= 4) {
                            RoutePoint r = new RoutePoint();
                            r.dailyKmId = (int) parseDouble(parts[0]);
                            r.latitude = parseDouble(parts[1]);
                            r.longitude = parseDouble(parts[2]);
                            r.timestamp = parseLong(parts[3]);
                            dao.insertRoutePoint(r);
                            importedCount++;
                        }
                        break;
                    case "ROUTE_HEADERS":
                        if (parts.length >= 2) {
                            RouteHeader h = new RouteHeader();
                            h.id = (int) parseDouble(parts[0]);
                            h.name = parts[1];
                            h.date = parts.length > 2 ? parseLong(parts[2]) : System.currentTimeMillis();
                            h.isCompleted = parts.length > 3 && Boolean.parseBoolean(parts[3]);
                            h.failedCount = parts.length > 4 ? (int) parseDouble(parts[4]) : 0;
                            dao.insertRouteHeader(h);
                            importedCount++;
                        }
                        break;
                    case "ROUTE_GROUPS":
                        if (parts.length >= 3) {
                            RouteGroup g = new RouteGroup();
                            g.id = (int) parseDouble(parts[0]);
                            g.name = parts[1];
                            g.color = parts.length > 2 ? parts[2] : "#2196F3";
                            g.routeId = parts.length > 3 ? (int) parseDouble(parts[3]) : 0;
                            dao.insertRouteGroup(g);
                            importedCount++;
                        }
                        break;
                    case "ROUTE_STOPS":
                        if (parts.length >= 5) {
                            RouteStop s = new RouteStop();
                            s.routeId = (int) parseDouble(parts[1]);
                            s.address = parts[2];
                            s.latitude = parseDouble(parts[3]);
                            s.longitude = parseDouble(parts[4]);
                            s.deliveryStatus = parts.length > 5 ? (int) parseDouble(parts[5]) : 0;
                            s.packageCount = parts.length > 6 ? (int) parseDouble(parts[6]) : 1;
                            s.sortOrder = parts.length > 7 ? (int) parseDouble(parts[7]) : 0;
                            s.groupId = parts.length > 8 && !parts[8].equals("null") ? (int) parseDouble(parts[8]) : null;
                            s.sequence = parts.length > 9 ? (int) parseDouble(parts[9]) : 0;
                            s.allSequences = parts.length > 10 ? parts[10] : "";
                            s.stopNumber = parts.length > 11 ? (int) parseDouble(parts[11]) : 0;
                            s.neighborhood = parts.length > 12 ? parts[12] : "";
                            s.city = parts.length > 13 ? parts[13] : "";
                            s.zipcode = parts.length > 14 ? parts[14] : "";
                            dao.insertRouteStop(s);
                            importedCount++;
                        }
                        break;
                    case "CORRECTED_ADDRESSES":
                        if (parts.length >= 4) {
                            CorrectedAddress ca = new CorrectedAddress();
                            ca.address = parts[1];
                            ca.latitude = parseDouble(parts[2]);
                            ca.longitude = parseDouble(parts[3]);
                            ca.updatedAt = parts.length > 4 ? parseLong(parts[4]) : System.currentTimeMillis();
                            dao.insertCorrectedAddress(ca);
                            importedCount++;
                        }
                        break;
                    case "LOADING_POINTS":
                        if (parts.length >= 4) {
                            LoadingPoint lp = new LoadingPoint();
                            lp.name = parts[1];
                            lp.latitude = parseDouble(parts[2]);
                            lp.longitude = parseDouble(parts[3]);
                            lp.platformName = parts.length > 4 ? parts[4] : "";
                            dao.insertLoadingPoint(lp);
                            importedCount++;
                        }
                        break;
                    case "SETTINGS":
                        if (parts.length >= 2) {
                            String key = parts[0];
                            String value = parts[1];
                            android.content.SharedPreferences prefs = context.getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
                            
                            android.util.Log.d("CsvHelper", "Importando ajuste: " + key + "=" + value);

                            // Mapeamento explícito de tipos para evitar ClassCastException (importante para backup/restore)
                            // Usamos .commit() em vez de .apply() para garantir que os ajustes (como Tema) estejam salvos antes de abrir a MainActivity
                            try {
                                if (key.equals("amazon_base") || key.equals("ml_base") || key.equals("shopee_base") || 
                                    key.equals("weekly_goal") || key.equals("default_consumption") || 
                                    key.equals("default_fuel_price") || key.equals("home_lat") || key.equals("home_lon")) {
                                    prefs.edit().putFloat(key, (float) parseDouble(value)).commit();
                                } else if (key.equals("app_theme") || key.equals("default_tab") || 
                                         key.equals("report_km_source") || key.equals("home_trigger_radius") ||
                                         key.equals("tracking_mode_v2") || key.equals("home_arrival_radius") ||
                                         key.equals("home_arrival_time") || key.equals("min_stop_duration_seconds") ||
                                         key.equals("tracking_short_stop_time") || key.equals("tracking_short_stop_radius") ||
                                         key.equals("tracking_medium_stop_time") || key.equals("tracking_medium_stop_radius") ||
                                         key.equals("tracking_long_stop_radius") || key.equals("loading_base_radius") ||
                                         key.equals("loading_base_time") || key.equals("home_trigger_radius")) {
                                    prefs.edit().putInt(key, (int) parseDouble(value)).commit();
                                } else if (key.startsWith("tab_") || key.equals("maps_enabled") ||
                                         key.equals("subtract_fuel") || key.equals("rest_interval_enabled") ||
                                         key.equals("auto_backup_cloud") || key.equals("tracking_auto") ||
                                         key.equals("home_tracking_enabled") || key.equals("rest_interval_enabled") ||
                                         value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                                    // Garante que chaves que são logicamente booleanas sejam salvas como Boolean
                                    boolean boolVal = value.equalsIgnoreCase("true") || value.equals("1") || value.equalsIgnoreCase("on");
                                    prefs.edit().putBoolean(key, boolVal).commit();
                                } else {
                                    // Strings e outros
                                    prefs.edit().putString(key, value).commit();
                                }
                            } catch (Exception e) {
                                android.util.Log.e("CsvHelper", "Erro ao restaurar chave de ajuste: " + key, e);
                            }
                            importedCount++;
                        }
                        break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        in.close();
        if (importedCount == 0) {
            throw new Exception("Nenhum dado válido encontrado no arquivo CSV.");
        }
    }

    private static double parseDouble(String val) {
        if (val == null || val.isEmpty() || val.equals("null")) return 0;
        try {
            return Double.parseDouble(val.replace(",", "."));
        } catch (Exception e) { return 0; }
    }

    private static long parseLong(String val) {
        if (val == null || val.isEmpty() || val.equals("null")) return 0;
        try {
            if (val.contains(".")) val = val.split("\\.")[0];
            return Long.parseLong(val);
        } catch (Exception e) { return 0; }
    }

    private static String[] parseCsvLine(String line) {
        List<String> parts = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    sb.append('\"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                parts.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        parts.add(sb.toString());
        return parts.toArray(new String[0]);
    }
}
