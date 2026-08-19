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

        if (tablesToImport.contains("DAILY_KM")) {
            if (!isMerge) dao.clearDailyKm();
            Cursor c = externalDb.rawQuery("SELECT * FROM daily_km", null);
            while (c.moveToNext()) {
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
                dao.insertDailyKm(d);
            }
            c.close();
        }
        
        if (tablesToImport.contains("MAINTENANCE")) {
            if (!isMerge) dao.clearMaintenance();
            Cursor c = externalDb.rawQuery("SELECT * FROM maintenance", null);
            while (c.moveToNext()) {
                Maintenance m = new Maintenance();
                m.description = c.getString(c.getColumnIndexOrThrow("description"));
                m.value = c.getDouble(c.getColumnIndexOrThrow("value"));
                m.date = c.getLong(c.getColumnIndexOrThrow("date"));
                m.km = c.getInt(c.getColumnIndexOrThrow("km"));
                m.type = c.getString(c.getColumnIndexOrThrow("type"));
                m.intervalKm = c.getInt(c.getColumnIndexOrThrow("intervalKm"));
                m.alertKm = c.getInt(c.getColumnIndexOrThrow("alertKm"));
                dao.insertMaintenance(m);
            }
            c.close();
        }

        if (tablesToImport.contains("ROUTE_HEADERS")) {
            if (!isMerge) {
                dao.clearRouteHeaders();
                dao.clearRouteStops();
                dao.clearRoutePoints();
            }
            
            // 1. Importar Headers
            Cursor c = externalDb.rawQuery("SELECT * FROM route_headers", null);
            while (c.moveToNext()) {
                RouteHeader h = new RouteHeader();
                h.id = c.getInt(c.getColumnIndexOrThrow("id")); // Importante manter o ID para os Stops/Points
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
                dao.insertRouteHeader(h);
            }
            c.close();

            // 2. Importar Stops
            try (Cursor cStops = externalDb.rawQuery("SELECT * FROM route_stops", null)) {
                while (cStops.moveToNext()) {
                    RouteStop s = new RouteStop();
                    s.routeId = cStops.getInt(cStops.getColumnIndexOrThrow("routeId"));
                    s.address = cStops.getString(cStops.getColumnIndexOrThrow("address"));
                    s.latitude = cStops.getDouble(cStops.getColumnIndexOrThrow("latitude"));
                    s.longitude = cStops.getDouble(cStops.getColumnIndexOrThrow("longitude"));
                    s.deliveryStatus = cStops.getInt(cStops.getColumnIndexOrThrow("deliveryStatus"));
                    s.sortOrder = cStops.getInt(cStops.getColumnIndexOrThrow("sortOrder"));
                    s.packageCount = cStops.getInt(cStops.getColumnIndexOrThrow("packageCount"));
                    try {
                        s.buyerCount = cStops.getInt(cStops.getColumnIndexOrThrow("buyerCount"));
                    } catch (Exception ignored) {}
                    dao.insertRouteStop(s);
                }
            } catch (Exception e) {
                Log.e("DbHelper", "Error importing route_stops", e);
            }

            // 3. Importar Groups
            try (Cursor cGroups = externalDb.rawQuery("SELECT * FROM route_groups", null)) {
                while (cGroups.moveToNext()) {
                    RouteGroup g = new RouteGroup();
                    g.routeId = cGroups.getInt(cGroups.getColumnIndexOrThrow("routeId"));
                    g.name = cGroups.getString(cGroups.getColumnIndexOrThrow("name"));
                    g.color = cGroups.getString(cGroups.getColumnIndexOrThrow("color"));
                    dao.insertRouteGroup(g);
                }
            } catch (Exception e) {
                Log.e("DbHelper", "Error importing route_groups", e);
            }

            // 4. Importar Points (Traçado do GPS - No banco de dados antigo isso estava ligado ao DailyKm)
            try (Cursor cPoints = externalDb.rawQuery("SELECT * FROM route_points", null)) {
                while (cPoints.moveToNext()) {
                    RoutePoint p = new RoutePoint();
                    p.dailyKmId = cPoints.getInt(cPoints.getColumnIndexOrThrow("dailyKmId"));
                    p.latitude = cPoints.getDouble(cPoints.getColumnIndexOrThrow("latitude"));
                    p.longitude = cPoints.getDouble(cPoints.getColumnIndexOrThrow("longitude"));
                    p.timestamp = cPoints.getLong(cPoints.getColumnIndexOrThrow("timestamp"));
                    dao.insertRoutePoint(p);
                }
            } catch (Exception e) {
                Log.e("DbHelper", "Error importing route_points", e);
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
            try {
                Cursor c = externalDb.rawQuery("SELECT * FROM settings", null);
                while (c.moveToNext()) {
                    SettingEntry se = new SettingEntry();
                    se.key = c.getString(c.getColumnIndexOrThrow("key"));
                    se.value = c.getString(c.getColumnIndexOrThrow("value"));
                    se.type = c.getString(c.getColumnIndexOrThrow("type"));
                    dao.insertSetting(se);
                }
                c.close();
                SettingsSyncHelper.loadPrefsFromDb(context);
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
