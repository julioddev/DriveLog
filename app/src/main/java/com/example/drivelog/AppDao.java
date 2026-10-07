package com.example.drivelog;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AppDao {
    @Insert
    void insertFuel(Fuel fuel);

    @Update
    void updateFuel(Fuel fuel);

    @Delete
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
    LiveData<List<Fuel>> getAllFuelLive();

    @Insert
    void insertEarnings(Earnings earnings);

    @Update
    void updateEarnings(Earnings earnings);

    @Delete
    void deleteEarnings(Earnings earnings);

    @Query("SELECT * FROM earnings ORDER BY date DESC LIMIT 10")
    List<Earnings> getRecentEarnings();

    @Query("SELECT * FROM earnings ORDER BY date DESC")
    List<Earnings> getAllEarnings();

    @Query("SELECT * FROM earnings ORDER BY date DESC")
    LiveData<List<Earnings>> getAllEarningsLive();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertDailyKm(DailyKm dailyKm);

    @Update
    void updateDailyKm(DailyKm dailyKm);

    @Delete
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
    LiveData<List<DailyKm>> getAllPendingDailyKmLive();

    @Query("SELECT * FROM daily_km WHERE isAutomatic = 0 ORDER BY date DESC")
    List<DailyKm> getAllDailyKm();

    @Query("SELECT * FROM daily_km WHERE isAutomatic = 0 ORDER BY date DESC")
    LiveData<List<DailyKm>> getAllDailyKmLive();

    @Query("SELECT * FROM daily_km ORDER BY date DESC")
    LiveData<List<DailyKm>> getAllKmAnyLive();

    @Query("SELECT * FROM daily_km WHERE isAutomatic = 1 ORDER BY date DESC")
    LiveData<List<DailyKm>> getAllAutomaticRoutesLive();

    @Query("SELECT * FROM daily_km WHERE id = :id LIMIT 1")
    DailyKm getDailyKmById(int id);

    @Query("SELECT * FROM daily_km ORDER BY date DESC LIMIT 10")
    List<DailyKm> getRecentDailyKm();

    @Insert
    void insertMaintenance(Maintenance maintenance);

    @Update
    void updateMaintenance(Maintenance maintenance);

    @Delete
    void deleteMaintenance(Maintenance maintenance);

    @Query("SELECT * FROM maintenance ORDER BY date DESC")
    List<Maintenance> getAllMaintenance();

    @Query("SELECT * FROM maintenance ORDER BY date DESC")
    LiveData<List<Maintenance>> getAllMaintenanceLive();

    @Query("SELECT COUNT(*) FROM earnings WHERE date >= :start AND date <= :end")
    LiveData<Integer> getEarningsCountToday(long start, long end);

    @Query("SELECT COUNT(*) FROM daily_km WHERE date >= :start AND date <= :end")
    LiveData<Integer> getKmCountToday(long start, long end);

    @Query("SELECT * FROM earnings WHERE date >= :start AND date <= :end")
    LiveData<List<Earnings>> getTodayEarningsEntriesLive(long start, long end);

    @Query("SELECT * FROM daily_km WHERE date >= :start AND date <= :end")
    LiveData<List<DailyKm>> getTodayKmEntriesLive(long start, long end);

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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertPlatform(Platform platform);

    @Update
    void updatePlatform(Platform platform);

    @Delete
    void deletePlatform(Platform platform);

    @Query("SELECT * FROM platforms ORDER BY orderIndex ASC")
    List<Platform> getAllPlatforms();

    @Query("SELECT * FROM platforms ORDER BY orderIndex ASC")
    LiveData<List<Platform>> getAllPlatformsLive();

    @Query("SELECT COUNT(*) FROM platforms")
    int getPlatformCount();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertGasStation(GasStation station);

    @Update
    void updateGasStation(GasStation station);

    @Delete
    void deleteGasStation(GasStation station);

    @Query("SELECT * FROM gas_stations ORDER BY orderIndex ASC")
    List<GasStation> getAllGasStations();

    @Query("SELECT * FROM gas_stations ORDER BY orderIndex ASC")
    LiveData<List<GasStation>> getAllGasStationsLive();

    // Route Points
    @Insert
    void insertRoutePoint(RoutePoint point);

    @Insert
    void insertRoutePoints(List<RoutePoint> points);

    @Query("SELECT * FROM route_points WHERE dailyKmId = :kmId ORDER BY timestamp ASC")
    List<RoutePoint> getRoutePointsForKm(int kmId);

    @Query("SELECT * FROM route_points ORDER BY dailyKmId, timestamp ASC")
    List<RoutePoint> getAllRoutePoints();

    @Query("DELETE FROM route_points WHERE dailyKmId = :kmId")
    void deleteRoutePointsForKm(int kmId);

    @Query("DELETE FROM route_points")
    void clearRoutePoints();

    // Route Stops
    @Insert
    void insertRouteStop(RouteStop stop);

    @Insert
    void insertRouteStops(List<RouteStop> stops);

    @Update
    void updateRouteStop(RouteStop stop);

    @Update
    void updateRouteStops(List<RouteStop> stops);

    @Delete
    void deleteRouteStop(RouteStop stop);

    @Query("SELECT * FROM route_stops WHERE routeId = :routeId ORDER BY sortOrder ASC, id ASC")
    LiveData<List<RouteStop>> getStopsForRouteLive(int routeId);

    @Query("SELECT * FROM route_stops WHERE routeId = :routeId ORDER BY sortOrder ASC, id ASC")
    List<RouteStop> getStopsForRoute(int routeId);

    @Query("SELECT * FROM route_stops")
    List<RouteStop> getAllRouteStops();

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

    @Update
    void updateRouteGroup(RouteGroup group);

    @Delete
    void deleteRouteGroup(RouteGroup group);

    @Query("SELECT * FROM route_groups WHERE routeId = :routeId")
    List<RouteGroup> getGroupsForRoute(int routeId);

    @Query("SELECT * FROM route_groups WHERE routeId = :routeId")
    LiveData<List<RouteGroup>> getGroupsForRouteLive(int routeId);

    // Route Headers
    @Insert
    long insertRouteHeader(RouteHeader header);

    @Update
    void updateRouteHeader(RouteHeader header);

    @Delete
    void deleteRouteHeader(RouteHeader header);

    @Query("SELECT * FROM route_headers ORDER BY date DESC")
    List<RouteHeader> getAllRoutes();

    @Query("SELECT * FROM route_headers ORDER BY date DESC")
    LiveData<List<RouteHeader>> getAllRoutesLive();

    @Query("SELECT * FROM route_headers WHERE id = :id LIMIT 1")
    RouteHeader getRouteById(int id);

    @Query("SELECT * FROM route_headers WHERE id = :id LIMIT 1")
    LiveData<RouteHeader> getRouteByIdLive(int id);

    // Corrected Addresses
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertCorrectedAddress(CorrectedAddress correctedAddress);
    
    @Update
    void updateCorrectedAddress(CorrectedAddress correctedAddress);

    @Query("SELECT * FROM corrected_addresses WHERE address = :addressText LIMIT 1")
    CorrectedAddress getCorrectedAddress(String addressText);

    @Query("SELECT * FROM corrected_addresses ORDER BY updatedAt DESC")
    List<CorrectedAddress> getAllCorrectedAddresses();

    @Query("SELECT * FROM corrected_addresses ORDER BY updatedAt DESC")
    LiveData<List<CorrectedAddress>> getAllCorrectedAddressesLive();

    @Delete
    void deleteCorrectedAddress(CorrectedAddress correctedAddress);

    // Corrected Quadras
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertCorrectedQuadra(CorrectedQuadra quadra);

    @Update
    void updateCorrectedQuadra(CorrectedQuadra quadra);

    @Delete
    void deleteCorrectedQuadra(CorrectedQuadra quadra);

    @Query("DELETE FROM corrected_quadras WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND (:neighborhood IS NULL OR :neighborhood = '' OR LOWER(TRIM(neighborhood)) = LOWER(TRIM(:neighborhood)))")
    void deleteCorrectedQuadraByNameAndNeighborhood(String name, String neighborhood);

    @Query("SELECT * FROM corrected_quadras ORDER BY name ASC")
    List<CorrectedQuadra> getAllCorrectedQuadras();

    @Query("SELECT * FROM corrected_quadras ORDER BY name ASC")
    LiveData<List<CorrectedQuadra>> getAllCorrectedQuadrasLive();

    @Query("SELECT * FROM corrected_quadras WHERE id = :id LIMIT 1")
    CorrectedQuadra getCorrectedQuadraById(int id);

    // Loading Points
    @Insert
    long insertLoadingPoint(LoadingPoint point);

    @Update
    void updateLoadingPoint(LoadingPoint point);

    @Delete
    void deleteLoadingPoint(LoadingPoint point);

    @Query("DELETE FROM loading_points")
    void clearLoadingPoints();

    @Query("SELECT * FROM loading_points ORDER BY id ASC")
    List<LoadingPoint> getAllLoadingPoints();

    @Query("SELECT * FROM loading_points ORDER BY id ASC")
    LiveData<List<LoadingPoint>> getAllLoadingPointsLive();

    // Settings
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSetting(SettingEntry setting);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSettings(List<SettingEntry> settings);

    @Query("SELECT * FROM settings")
    List<SettingEntry> getAllSettings();

    @Query("DELETE FROM settings")
    void clearSettings();
}
