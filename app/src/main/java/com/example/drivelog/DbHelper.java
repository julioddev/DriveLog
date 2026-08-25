package com.example.drivelog;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class DbHelper {

    public static Map<String, Integer> peekDbContents(Context context, Uri uri) throws Exception {
        Map<String, Integer> counts = new LinkedHashMap<>();
        File tempFile = new File(context.getCacheDir(), "temp_peek.db");
        copyUriToFile(context, uri, tempFile);

        SQLiteDatabase db = SQLiteDatabase.openDatabase(tempFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        String[] tables = {"earnings", "fuel", "daily_km", "maintenance", "route_headers", "route_points", "corrected_addresses", "loading_points", "settings", "platforms", "gas_stations"};
        for (String table : tables) {
            try {
                Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + table, null);
                if (c.moveToFirst()) counts.put(table.toUpperCase(), c.getInt(0));
                c.close();
            } catch (Exception ignored) {}
        }
        db.close();
        tempFile.delete();
        return counts;
    }

    public static void importFromDb(Context context, Uri uri, Set<String> tablesToImport, boolean isMerge) throws Exception {
        File tempFile = new File(context.getCacheDir(), "temp_import.db");
        copyUriToFile(context, uri, tempFile);
        SQLiteDatabase externalDb = SQLiteDatabase.openDatabase(tempFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        AppDao dao = AppDatabase.getInstance(context).appDao();

        if (tablesToImport.contains("EARNINGS")) {
            if (!isMerge) dao.clearEarnings();
            Cursor c = externalDb.rawQuery("SELECT * FROM earnings", null);
            while (c.moveToNext()) {
                Earnings e = new Earnings();
                e.baseValue = c.getDouble(c.getColumnIndexOrThrow("baseValue"));
                e.extraValue = c.getDouble(c.getColumnIndexOrThrow("extraValue"));
                e.totalValue = c.getDouble(c.getColumnIndexOrThrow("totalValue"));
                e.platforms = c.getString(c.getColumnIndexOrThrow("platforms"));
                e.date = c.getLong(c.getColumnIndexOrThrow("date"));
                dao.insertEarnings(e);
            }
            c.close();
        }

        if (tablesToImport.contains("FUEL")) {
            if (!isMerge) dao.clearFuel();
            Cursor c = externalDb.rawQuery("SELECT * FROM fuel", null);
            while (c.moveToNext()) {
                Fuel f = new Fuel();
                f.value = c.getDouble(c.getColumnIndexOrThrow("value"));
                f.pricePerLiter = c.getDouble(c.getColumnIndexOrThrow("pricePerLiter"));
                f.liters = c.getDouble(c.getColumnIndexOrThrow("liters"));
                f.km = c.getInt(c.getColumnIndexOrThrow("km"));
                f.kmDriven = c.getDouble(c.getColumnIndexOrThrow("kmDriven"));
                f.date = c.getLong(c.getColumnIndexOrThrow("date"));
                f.isCompleted = c.getInt(c.getColumnIndexOrThrow("isCompleted")) == 1;
                f.fuelType = c.getString(c.getColumnIndexOrThrow("fuelType"));
                f.gasStation = c.getString(c.getColumnIndexOrThrow("gasStation"));
                dao.insertFuel(f);
            }
            c.close();
        }

        // Mapeamento para Points (Vínculo com DailyKm)
        java.util.Map<Integer, Integer> kmIdMap = new java.util.HashMap<>();

        if (tablesToImport.contains("DAILY_KM")) {
            if (!isMerge) dao.clearDailyKm();
            try (Cursor c = externalDb.rawQuery("SELECT * FROM daily_km", null)) {
                while (c.moveToNext()) {
                    int oldId = c.getInt(c.getColumnIndexOrThrow("id"));
                    DailyKm d = new DailyKm();
                    d.kmStart = c.getDouble(c.getColumnIndexOrThrow("kmStart"));
                    d.kmEnd = c.getDouble(c.getColumnIndexOrThrow("kmEnd"));
                    d.totalKm = c.getDouble(c.getColumnIndexOrThrow("totalKm"));
                    d.estimatedFuelCost = c.getDouble(c.getColumnIndexOrThrow("estimatedFuelCost"));
                    d.consumptionUsed = c.getDouble(c.getColumnIndexOrThrow("consumptionUsed"));
                    d.date = c.getLong(c.getColumnIndexOrThrow("date"));
                    d.isCompleted = c.getInt(c.getColumnIndexOrThrow("isCompleted")) == 1;
                    d.gpsDistance = c.getDouble(c.getColumnIndexOrThrow("gpsDistance"));
                    d.isAutomatic = c.getInt(c.getColumnIndexOrThrow("isAutomatic")) == 1;
                    
                    long newId = dao.insertDailyKm(d);
                    kmIdMap.put(oldId, (int) newId);
                }
            }
        }

        if (tablesToImport.contains("ROUTE_HEADERS")) {
            if (!isMerge) {
                dao.clearRouteHeaders();
                dao.clearRouteStops();
                dao.clearRoutePoints();
            }
            
            // Mapeamento de IDs Antigos -> Novos para manter as chaves estrangeiras
            java.util.Map<Integer, Integer> routeIdMap = new java.util.HashMap<>();

            // 1. Importar Headers
            try (Cursor c = externalDb.rawQuery("SELECT * FROM route_headers", null)) {
                while (c.moveToNext()) {
                    int oldId = c.getInt(c.getColumnIndexOrThrow("id"));
                    RouteHeader h = new RouteHeader();
                    h.name = c.getString(c.getColumnIndexOrThrow("name"));
                    h.date = c.getLong(c.getColumnIndexOrThrow("date"));
                    h.isCompleted = c.getInt(c.getColumnIndexOrThrow("isCompleted")) == 1;
                    h.failedCount = c.getInt(c.getColumnIndexOrThrow("failedCount"));
                    try {
                        h.isActive = c.getInt(c.getColumnIndexOrThrow("isActive")) == 1;
                        h.startTime = c.getLong(c.getColumnIndexOrThrow("startTime"));
                        h.endTime = c.getLong(c.getColumnIndexOrThrow("endTime"));
                        h.totalPausedMs = c.getLong(c.getColumnIndexOrThrow("totalPausedMs"));
                        h.lastPauseStartTime = c.getLong(c.getColumnIndexOrThrow("lastPauseStartTime"));
                    } catch (Exception ignored) {}
                    
                    long newId = dao.insertRouteHeader(h);
                    routeIdMap.put(oldId, (int) newId);
                }
            }

            // 2. Importar Stops
            try (Cursor cStops = externalDb.rawQuery("SELECT * FROM route_stops", null)) {
                while (cStops.moveToNext()) {
                    int oldRouteId = cStops.getInt(cStops.getColumnIndexOrThrow("routeId"));
                    if (!routeIdMap.containsKey(oldRouteId)) continue; 

                    RouteStop s = new RouteStop();
                    Integer mappedId = routeIdMap.get(oldRouteId);
                    if (mappedId == null) continue;
                    s.routeId = mappedId;
                    s.address = cStops.getString(cStops.getColumnIndexOrThrow("address"));
                    s.latitude = cStops.getDouble(cStops.getColumnIndexOrThrow("latitude"));
                    s.longitude = cStops.getDouble(cStops.getColumnIndexOrThrow("longitude"));
                    s.deliveryStatus = cStops.getInt(cStops.getColumnIndexOrThrow("deliveryStatus"));
                    s.sortOrder = cStops.getInt(cStops.getColumnIndexOrThrow("sortOrder"));
                    s.packageCount = cStops.getInt(cStops.getColumnIndexOrThrow("packageCount"));
                    try {
                        s.buyerCount = cStops.getInt(cStops.getColumnIndexOrThrow("buyerCount"));
                        s.sequence = cStops.getInt(cStops.getColumnIndexOrThrow("sequence"));
                        s.allSequences = cStops.getString(cStops.getColumnIndexOrThrow("allSequences"));
                        s.stopNumber = cStops.getInt(cStops.getColumnIndexOrThrow("stopNumber"));
                        s.neighborhood = cStops.getString(cStops.getColumnIndexOrThrow("neighborhood"));
                        s.spxTn = cStops.getString(cStops.getColumnIndexOrThrow("spxTn"));
                    } catch (Exception ignored) {}
                    dao.insertRouteStop(s);
                }
            }

            // 3. Importar Groups
            try (Cursor cGroups = externalDb.rawQuery("SELECT * FROM route_groups", null)) {
                while (cGroups.moveToNext()) {
                    int oldRouteId = cGroups.getInt(cGroups.getColumnIndexOrThrow("routeId"));
                    Integer mappedId = routeIdMap.get(oldRouteId);
                    if (mappedId == null) continue;

                    RouteGroup g = new RouteGroup();
                    g.routeId = mappedId;
                    g.name = cGroups.getString(cGroups.getColumnIndexOrThrow("name"));
                    g.color = cGroups.getString(cGroups.getColumnIndexOrThrow("color"));
                    dao.insertRouteGroup(g);
                }
            }

            // 4. Importar Points
            try (Cursor cPoints = externalDb.rawQuery("SELECT * FROM route_points", null)) {
                while (cPoints.moveToNext()) {
                    int oldKmId = cPoints.getInt(cPoints.getColumnIndexOrThrow("dailyKmId"));
                    RoutePoint p = new RoutePoint();
                    
                    // Tenta mapear o vínculo com o registro diário novo
                    if (kmIdMap.containsKey(oldKmId)) {
                        Integer newKmId = kmIdMap.get(oldKmId);
                        if (newKmId != null) p.dailyKmId = newKmId;
                        else p.dailyKmId = oldKmId; // Fallback se falhar
                    } else {
                        p.dailyKmId = oldKmId; // Mantém original se for merge ou ID preservado
                    }

                    p.latitude = cPoints.getDouble(cPoints.getColumnIndexOrThrow("latitude"));
                    p.longitude = cPoints.getDouble(cPoints.getColumnIndexOrThrow("longitude"));
                    p.timestamp = cPoints.getLong(cPoints.getColumnIndexOrThrow("timestamp"));
                    dao.insertRoutePoint(p);
                }
            }
        }

        if (tablesToImport.contains("CORRECTED_ADDRESSES")) {
            if (!isMerge) AppDatabase.getInstance(context).getOpenHelper().getWritableDatabase().execSQL("DELETE FROM corrected_addresses");
            try (Cursor c = externalDb.rawQuery("SELECT * FROM corrected_addresses", null)) {
                while (c.moveToNext()) {
                    CorrectedAddress ca = new CorrectedAddress();
                    ca.address = c.getString(c.getColumnIndexOrThrow("address"));
                    ca.neighborhood = c.getString(c.getColumnIndexOrThrow("neighborhood"));
                    ca.latitude = c.getDouble(c.getColumnIndexOrThrow("latitude"));
                    ca.longitude = c.getDouble(c.getColumnIndexOrThrow("longitude"));
                    ca.updatedAt = c.getLong(c.getColumnIndexOrThrow("updatedAt"));
                    dao.insertCorrectedAddress(ca);
                }
            } catch (Exception e) {
                Log.e("DbHelper", "Error importing corrected_addresses", e);
            }
        }

        if (tablesToImport.contains("SETTINGS")) {
            if (!isMerge) dao.clearSettings();
            try (Cursor c = externalDb.rawQuery("SELECT * FROM settings", null)) {
                while (c.moveToNext()) {
                    SettingEntry se = new SettingEntry();
                    se.key = c.getString(c.getColumnIndexOrThrow("key"));
                    se.value = c.getString(c.getColumnIndexOrThrow("value"));
                    se.type = c.getString(c.getColumnIndexOrThrow("type"));
                    dao.insertSetting(se);
                }
                SettingsSyncHelper.loadPrefsFromDb(context);
            } catch (Exception ignored) {}
        }

        if (tablesToImport.contains("LOADING_POINTS")) {
            if (!isMerge) dao.clearLoadingPoints();
            try (Cursor c = externalDb.rawQuery("SELECT * FROM loading_points", null)) {
                while (c.moveToNext()) {
                    LoadingPoint lp = new LoadingPoint();
                    lp.name = c.getString(c.getColumnIndexOrThrow("name"));
                    lp.latitude = c.getDouble(c.getColumnIndexOrThrow("latitude"));
                    lp.longitude = c.getDouble(c.getColumnIndexOrThrow("longitude"));
                    lp.platformName = c.getString(c.getColumnIndexOrThrow("platformName"));
                    dao.insertLoadingPoint(lp);
                }
            } catch (Exception ignored) {}
        }

        if (tablesToImport.contains("PLATFORMS")) {
            if (!isMerge) dao.clearPlatforms();
            try (Cursor c = externalDb.rawQuery("SELECT * FROM platforms", null)) {
                while (c.moveToNext()) {
                    Platform p = new Platform();
                    p.name = c.getString(c.getColumnIndexOrThrow("name"));
                    p.isEnabled = c.getInt(c.getColumnIndexOrThrow("isEnabled")) == 1;
                    p.defaultValue = c.getDouble(c.getColumnIndexOrThrow("defaultValue"));
                    p.orderIndex = c.getInt(c.getColumnIndexOrThrow("orderIndex"));
                    p.isDefault = c.getInt(c.getColumnIndexOrThrow("isDefault")) == 1;
                    dao.insertPlatform(p);
                }
            } catch (Exception ignored) {}
        }

        if (tablesToImport.contains("GAS_STATIONS")) {
            if (!isMerge) dao.clearGasStations();
            try (Cursor c = externalDb.rawQuery("SELECT * FROM gas_stations", null)) {
                while (c.moveToNext()) {
                    GasStation gs = new GasStation();
                    gs.name = c.getString(c.getColumnIndexOrThrow("name"));
                    gs.isEnabled = c.getInt(c.getColumnIndexOrThrow("isEnabled")) == 1;
                    gs.orderIndex = c.getInt(c.getColumnIndexOrThrow("orderIndex"));
                    gs.isDefault = c.getInt(c.getColumnIndexOrThrow("isDefault")) == 1;
                    dao.insertGasStation(gs);
                }
            } catch (Exception ignored) {}
        }

        externalDb.close();
        tempFile.delete();
    }

    private static void copyUriToFile(Context context, Uri uri, File dest) throws Exception {
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(dest)) {
            if (in == null) throw new Exception("Erro ao abrir arquivo");
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        }
    }
}
