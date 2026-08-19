package com.example.drivelog;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface AppDao {
    @Insert
    void insertFuel(Fuel fuel);

    @androidx.room.Update
    void updateFuel(Fuel fuel);

    @androidx.room.Delete
    void deleteFuel(Fuel fuel);

    @Query("SELECT * FROM earnings WHERE platforms = :platformName AND date >= :startOfDay AND date <= :endOfDay LIMIT 1")
    Earnings getEarningForPlatformToday(String platformName, long startOfDay, long endOfDay);

    @Query("SELECT * FROM fuel ORDER BY date DESC LIMIT 1")
    Fuel getLastFuel();

    @Query("SELECT * FROM fuel WHERE isCompleted = 1 ORDER BY date DESC LIMIT 1")
    Fuel getLastCompletedFuel();

    @Query("SELECT * FROM fuel WHERE isCompleted = 1 AND gasStation = :stationName ORDER BY date DESC LIMIT 1")
    Fuel getLastCompletedFuelByStation(String stationName);

    @Query("SELECT * FROM fuel ORDER BY date DESC LIMIT 10")
    List<Fuel> getRecentFuel();

    @Query("SELECT * FROM fuel ORDER BY date DESC")
    List<Fuel> getAllFuel();

    @Query("SELECT * FROM fuel ORDER BY date DESC")
    androidx.lifecycle.LiveData<List<Fuel>> getAllFuelLive();

    @Insert
    void insertEarnings(Earnings earnings);

    @androidx.room.Update
    void updateEarnings(Earnings earnings);

    @androidx.room.Delete
    void deleteEarnings(Earnings earnings);

    @Query("SELECT * FROM earnings ORDER BY date DESC LIMIT 10")
    List<Earnings> getRecentEarnings();

    @Query("SELECT * FROM earnings ORDER BY date DESC")
    List<Earnings> getAllEarnings();

    @Query("SELECT * FROM earnings ORDER BY date DESC")
    androidx.lifecycle.LiveData<List<Earnings>> getAllEarningsLive();

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    long insertDailyKm(DailyKm dailyKm);

    @androidx.room.Update
    void updateDailyKm(DailyKm dailyKm);

    @androidx.room.Delete
    void deleteDailyKm(DailyKm dailyKm);

    @Query("SELECT * FROM daily_km ORDER BY date DESC LIMIT 1")
    DailyKm getLastDailyKm();

    @Query("SELECT * FROM daily_km WHERE isCompleted = 0 AND isAutomatic = 0 ORDER BY date DESC LIMIT 1")
    DailyKm getLastPendingDailyKm();

    @Query("SELECT * FROM daily_km WHERE isCompleted = 0 AND isAutomatic = 1 ORDER BY date DESC LIMIT 1")
    DailyKm getLastPendingAutomaticKm();

    @Query("SELECT * FROM daily_km WHERE isCompleted = 0 AND isAutomatic = 0 ORDER BY date DESC")
    List<DailyKm> getAllPendingDailyKm();

    @Query("SELECT * FROM daily_km WHERE isCompleted = 0 AND isAutomatic = 0 ORDER BY date DESC")
    androidx.lifecycle.LiveData<List<DailyKm>> getAllPendingDailyKmLive();

    @Query("SELECT * FROM daily_km WHERE isAutomatic = 0 ORDER BY date DESC")
    List<DailyKm> getAllDailyKm();

    @Query("SELECT * FROM daily_km WHERE isAutomatic = 0 ORDER BY date DESC")
    androidx.lifecycle.LiveData<List<DailyKm>> getAllDailyKmLive();

    @Query("SELECT * FROM daily_km ORDER BY date DESC")
    androidx.lifecycle.LiveData<List<DailyKm>> getAllKmAnyLive();

    @Query("SELECT * FROM daily_km WHERE isAutomatic = 1 ORDER BY date DESC")
    androidx.lifecycle.LiveData<List<DailyKm>> getAllAutomaticRoutesLive();

    @Query("SELECT * FROM daily_km WHERE id = :id LIMIT 1")
    DailyKm getDailyKmById(int id);

    @Query("SELECT * FROM daily_km ORDER BY date DESC LIMIT 10")
    List<DailyKm> getRecentDailyKm();

    @Insert
    void insertMaintenance(Maintenance maintenance);

    @androidx.room.Update
    void updateMaintenance(Maintenance maintenance);

    @androidx.room.Delete
    void deleteMaintenance(Maintenance maintenance);

    @Query("SELECT * FROM maintenance ORDER BY date DESC")
    List<Maintenance> getAllMaintenance();

    @Query("SELECT * FROM maintenance ORDER BY date DESC")
    androidx.lifecycle.LiveData<List<Maintenance>> getAllMaintenanceLive();

    @Query("SELECT COUNT(*) FROM earnings WHERE date >= :start AND date <= :end")
    androidx.lifecycle.LiveData<Integer> getEarningsCountToday(long start, long end);

    @Query("SELECT COUNT(*) FROM daily_km WHERE date >= :start AND date <= :end")
    androidx.lifecycle.LiveData<Integer> getKmCountToday(long start, long end);

    @Query("SELECT * FROM earnings WHERE date >= :start AND date <= :end")
    androidx.lifecycle.LiveData<List<Earnings>> getTodayEarningsEntriesLive(long start, long end);

    @Query("SELECT * FROM daily_km WHERE date >= :start AND date <= :end")
    androidx.lifecycle.LiveData<List<DailyKm>> getTodayKmEntriesLive(long start, long end);

    @Query("DELETE FROM earnings")
    void clearEarnings();

    @Query("DELETE FROM fuel")
    void clearFuel();

    @Query("DELETE FROM daily_km")
    void clearDailyKm();

    @Query("DELETE FROM maintenance")
    void clearMaintenance();

    @Query("DELETE FROM platforms")
    void clearPlatforms();

    @Query("DELETE FROM gas_stations")
    void clearGasStations();

    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    void insertPlatform(Platform platform);

    @androidx.room.Update
    void updatePlatform(Platform platform);

    @androidx.room.Delete
    void deletePlatform(Platform platform);

    @Query("SELECT * FROM platforms ORDER BY orderIndex ASC")
    java.util.List<Platform> getAllPlatforms();

    @Query("SELECT * FROM platforms ORDER BY orderIndex ASC")
    androidx.lifecycle.LiveData<java.util.List<Platform>> getAllPlatformsLive();

    @Query("SELECT COUNT(*) FROM platforms")
    int getPlatformCount();

    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    void insertGasStation(GasStation station);

    @androidx.room.Update
    void updateGasStation(GasStation station);

    @androidx.room.Delete
    void deleteGasStation(GasStation station);

    @Query("SELECT * FROM gas_stations ORDER BY orderIndex ASC")
    java.util.List<GasStation> getAllGasStations();

    @Query("SELECT * FROM gas_stations ORDER BY orderIndex ASC")
    androidx.lifecycle.LiveData<java.util.List<GasStation>> getAllGasStationsLive();

    // Route Points
    @Insert
    void insertRoutePoint(RoutePoint point);

    @Insert
    void insertRoutePoints(java.util.List<RoutePoint> points);

    @Query("SELECT * FROM route_points WHERE dailyKmId = :kmId ORDER BY timestamp ASC")
    java.util.List<RoutePoint> getRoutePointsForKm(int kmId);

    @Query("SELECT * FROM route_points ORDER BY dailyKmId, timestamp ASC")
    java.util.List<RoutePoint> getAllRoutePoints();

    @Query("DELETE FROM route_points WHERE dailyKmId = :kmId")
    void deleteRoutePointsForKm(int kmId);

    @Query("DELETE FROM route_points")
    void clearRoutePoints();

    // Route Stops
    @Insert
    void insertRouteStop(RouteStop stop);

    @Insert
    void insertRouteStops(java.util.List<RouteStop> stops);

    @androidx.room.Update
    void updateRouteStop(RouteStop stop);

    @androidx.room.Update
    void updateRouteStops(java.util.List<RouteStop> stops);

    @androidx.room.Delete
    void deleteRouteStop(RouteStop stop);

    @Query("SELECT * FROM route_stops WHERE routeId = :routeId ORDER BY sortOrder ASC, id ASC")
    androidx.lifecycle.LiveData<java.util.List<RouteStop>> getStopsForRouteLive(int routeId);

    @Query("SELECT * FROM route_stops WHERE routeId = :routeId ORDER BY sortOrder ASC, id ASC")
    java.util.List<RouteStop> getStopsForRoute(int routeId);

    @Query("SELECT COALESCE(MAX(stopNumber), 0) + 1 FROM route_stops WHERE routeId = :routeId")
    int getNextStopNumber(int routeId);

    @Query("DELETE FROM route_stops")
    void clearRouteStops();

    @Query("DELETE FROM route_stops WHERE routeId = :routeId")
    void clearRouteStopsByRoute(int routeId);

    @Query("DELETE FROM route_groups WHERE routeId = :routeId")
    void deleteGroupsForRoute(int routeId);

    @Query("DELETE FROM route_headers")
    void clearRouteHeaders();

    // Route Groups
    @Insert
    long insertRouteGroup(RouteGroup group);

    @androidx.room.Update
    void updateRouteGroup(RouteGroup group);

    @androidx.room.Delete
    void deleteRouteGroup(RouteGroup group);

    @Query("SELECT * FROM route_groups WHERE routeId = :routeId")
    java.util.List<RouteGroup> getGroupsForRoute(int routeId);

    @Query("SELECT * FROM route_groups WHERE routeId = :routeId")
    androidx.lifecycle.LiveData<java.util.List<RouteGroup>> getGroupsForRouteLive(int routeId);

    // Route Headers
    @Insert
    long insertRouteHeader(RouteHeader header);

    @androidx.room.Update
    void updateRouteHeader(RouteHeader header);

    @androidx.room.Delete
    void deleteRouteHeader(RouteHeader header);

    @Query("SELECT * FROM route_headers ORDER BY date DESC")
    java.util.List<RouteHeader> getAllRoutes();

    @Query("SELECT * FROM route_headers ORDER BY date DESC")
    androidx.lifecycle.LiveData<java.util.List<RouteHeader>> getAllRoutesLive();

    @Query("SELECT * FROM route_headers WHERE id = :id LIMIT 1")
    RouteHeader getRouteById(int id);

    @Query("SELECT * FROM route_headers WHERE id = :id LIMIT 1")
    androidx.lifecycle.LiveData<RouteHeader> getRouteByIdLive(int id);

    // Corrected Addresses
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    long insertCorrectedAddress(CorrectedAddress correctedAddress);
    
    @androidx.room.Update
    void updateCorrectedAddress(CorrectedAddress correctedAddress);

    @Query("SELECT * FROM corrected_addresses WHERE address = :addressText LIMIT 1")
    CorrectedAddress getCorrectedAddress(String addressText);

    @Query("SELECT * FROM corrected_addresses ORDER BY updatedAt DESC")
    java.util.List<CorrectedAddress> getAllCorrectedAddresses();

    @Query("SELECT * FROM corrected_addresses ORDER BY updatedAt DESC")
    androidx.lifecycle.LiveData<java.util.List<CorrectedAddress>> getAllCorrectedAddressesLive();

    @androidx.room.Delete
    void deleteCorrectedAddress(CorrectedAddress correctedAddress);

    // Loading Points
    @Insert
    long insertLoadingPoint(LoadingPoint point);

    @androidx.room.Update
    void updateLoadingPoint(LoadingPoint point);

    @androidx.room.Delete
    void deleteLoadingPoint(LoadingPoint point);

    @Query("SELECT * FROM loading_points ORDER BY id ASC")
    List<LoadingPoint> getAllLoadingPoints();

    @Query("SELECT * FROM loading_points ORDER BY id ASC")
    androidx.lifecycle.LiveData<List<LoadingPoint>> getAllLoadingPointsLive();

    // Settings
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    void insertSetting(SettingEntry setting);

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    void insertSettings(List<SettingEntry> settings);

    @Query("SELECT * FROM settings")
    List<SettingEntry> getAllSettings();

    @Query("DELETE FROM settings")
    void clearSettings();
}
