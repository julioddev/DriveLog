package com.example.drivelog;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Fuel.class, Earnings.class, DailyKm.class, Maintenance.class, Platform.class, GasStation.class, RoutePoint.class, RouteStop.class, CorrectedAddress.class, RouteHeader.class, RouteGroup.class, LoadingPoint.class, SettingEntry.class}, version = 42, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract AppDao appDao();

    static final Migration MIGRATION_41_42 = new Migration(41, 42) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Verifica se a coluna já existe antes de tentar adicionar (mais seguro em caso de falha anterior)
            try {
                database.execSQL("ALTER TABLE `earnings` ADD COLUMN `isCompleted` INTEGER NOT NULL DEFAULT 0");
            } catch (Exception ignored) {}
        }
    };

    static final Migration MIGRATION_40_41 = new Migration(40, 41) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            try {
                database.execSQL("ALTER TABLE `earnings` ADD COLUMN `isCompleted` INTEGER NOT NULL DEFAULT 0");
            } catch (Exception ignored) {}
        }
    };

    static final Migration MIGRATION_36_37 = new Migration(36, 37) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `route_stops` ADD COLUMN `originalLatitude` REAL NOT NULL DEFAULT 0.0");
            database.execSQL("ALTER TABLE `route_stops` ADD COLUMN `originalLongitude` REAL NOT NULL DEFAULT 0.0");
        }
    };

    static final Migration MIGRATION_37_38 = new Migration(37, 38) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `route_headers` ADD COLUMN `startTime` INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE `route_headers` ADD COLUMN `endTime` INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_38_39 = new Migration(38, 39) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `route_stops` ADD COLUMN `deliveryTimestamp` INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_39_40 = new Migration(39, 40) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `route_headers` ADD COLUMN `totalPausedMs` INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE `route_headers` ADD COLUMN `lastPauseStartTime` INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_35_36 = new Migration(35, 36) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `corrected_addresses` ADD COLUMN `creatorId` TEXT");
        }
    };

    static final Migration MIGRATION_34_35 = new Migration(34, 35) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `corrected_addresses` ADD COLUMN `notes` TEXT");
            database.execSQL("ALTER TABLE `corrected_addresses` ADD COLUMN `isNotePublic` INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_33_34 = new Migration(33, 34) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `route_stops` ADD COLUMN `buyerCount` INTEGER NOT NULL DEFAULT 1");
        }
    };

    static final Migration MIGRATION_32_33 = new Migration(32, 33) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `route_stops` ADD COLUMN `allAddresses` TEXT");
        }
    };

    static final Migration MIGRATION_31_32 = new Migration(31, 32) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `corrected_addresses` ADD COLUMN `city` TEXT");
        }
    };

    static final Migration MIGRATION_30_31 = new Migration(30, 31) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `corrected_addresses` ADD COLUMN `neighborhood` TEXT");
        }
    };

    static final Migration MIGRATION_29_30 = new Migration(29, 30) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `settings` (`key` TEXT NOT NULL, `value` TEXT, `type` TEXT, PRIMARY KEY(`key`))");
        }
    };

    static final Migration MIGRATION_28_29 = new Migration(28, 29) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `loading_points` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `platformName` TEXT)");
        }
    };

    static final Migration MIGRATION_15_16 = new Migration(15, 16) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DELETE FROM platforms WHERE id NOT IN (SELECT MIN(id) FROM platforms GROUP BY name)");
            database.execSQL("DELETE FROM gas_stations WHERE id NOT IN (SELECT MIN(id) FROM gas_stations GROUP BY name)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_platforms_name` ON `platforms` (`name`) ");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_gas_stations_name` ON `gas_stations` (`name`) ");
        }
    };

    static final Migration MIGRATION_16_17 = new Migration(16, 17) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE daily_km ADD COLUMN gpsDistance REAL NOT NULL DEFAULT 0.0");
        }
    };

    static final Migration MIGRATION_17_18 = new Migration(17, 18) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `route_stops_temp` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `atId` TEXT, `sequence` INTEGER NOT NULL DEFAULT 0, `stopNumber` INTEGER NOT NULL DEFAULT 0, `spxTn` TEXT, `address` TEXT, `neighborhood` TEXT, `city` TEXT, `zipcode` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `isDelivered` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)");
            database.execSQL("DROP TABLE IF EXISTS `route_stops` ");
            database.execSQL("ALTER TABLE `route_stops_temp` RENAME TO `route_stops` ");
        }
    };

    static final Migration MIGRATION_18_19 = new Migration(18, 19) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE IF EXISTS `route_stops` ");
            database.execSQL("CREATE TABLE IF NOT EXISTS `route_stops` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `atId` TEXT, `sequence` INTEGER NOT NULL DEFAULT 0, `stopNumber` INTEGER NOT NULL DEFAULT 0, `spxTn` TEXT, `address` TEXT, `neighborhood` TEXT, `city` TEXT, `zipcode` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `isDelivered` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)");
        }
    };

    static final Migration MIGRATION_19_20 = new Migration(19, 20) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `route_stops_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `atId` TEXT, `sequence` INTEGER NOT NULL, `stopNumber` INTEGER NOT NULL, `spxTn` TEXT, `address` TEXT, `neighborhood` TEXT, `city` TEXT, `zipcode` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `deliveryStatus` INTEGER NOT NULL DEFAULT 0, `createdAt` INTEGER NOT NULL)");
            database.execSQL("DROP TABLE IF EXISTS `route_stops` ");
            database.execSQL("ALTER TABLE `route_stops_new` RENAME TO `route_stops` ");
        }
    };

    static final Migration MIGRATION_20_21 = new Migration(20, 21) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `corrected_addresses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `address` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `updatedAt` INTEGER NOT NULL)");
        }
    };

    static final Migration MIGRATION_21_22 = new Migration(21, 22) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `route_headers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `date` INTEGER NOT NULL, `isActive` INTEGER NOT NULL DEFAULT 1)");
            database.execSQL("CREATE TABLE IF NOT EXISTS `route_stops_v22` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `routeId` INTEGER NOT NULL, `atId` TEXT, `sequence` INTEGER NOT NULL, `stopNumber` INTEGER NOT NULL, `spxTn` TEXT, `address` TEXT, `neighborhood` TEXT, `city` TEXT, `zipcode` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `deliveryStatus` INTEGER NOT NULL DEFAULT 0, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`routeId`) REFERENCES `route_headers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_route_stops_routeId` ON `route_stops_v22` (`routeId`) ");
            database.execSQL("INSERT INTO route_headers (name, date, isActive) SELECT 'Rota Inicial', " + System.currentTimeMillis() + ", 1");
            database.execSQL("INSERT INTO route_stops_v22 (routeId, atId, sequence, stopNumber, spxTn, address, neighborhood, city, zipcode, latitude, longitude, deliveryStatus, createdAt) " +
                           "SELECT (SELECT id FROM route_headers LIMIT 1), atId, sequence, stopNumber, spxTn, address, neighborhood, city, zipcode, latitude, longitude, deliveryStatus, createdAt FROM route_stops");
            database.execSQL("DROP TABLE `route_stops` ");
            database.execSQL("ALTER TABLE `route_stops_v22` RENAME TO `route_stops` ");
        }
    };

    static final Migration MIGRATION_22_23 = new Migration(22, 23) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `route_stops` ADD COLUMN `packageCount` INTEGER NOT NULL DEFAULT 1");
        }
    };

    static final Migration MIGRATION_23_24 = new Migration(23, 24) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `route_stops` ADD COLUMN `allSequences` TEXT");
        }
    };

    static final Migration MIGRATION_24_25 = new Migration(24, 25) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `route_stops` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE `route_stops` ADD COLUMN `groupId` INTEGER");
            database.execSQL("CREATE TABLE IF NOT EXISTS `route_groups` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `color` TEXT, `routeId` INTEGER NOT NULL)");
        }
    };

    static final Migration MIGRATION_25_26 = new Migration(25, 26) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `daily_km` ADD COLUMN `isAutomatic` INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_26_27 = new Migration(26, 27) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `route_headers` ADD COLUMN `isCompleted` INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_27_28 = new Migration(27, 28) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `route_headers` ADD COLUMN `failedCount` INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static synchronized void forceCloseInstance() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }

    public static synchronized AppDatabase getInstance(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        String userId = "local";
        try {
            userId = prefs.getString("current_user_id", "local");
        } catch (ClassCastException e) {
            // Se houver erro de tipo, tenta recuperar o valor como string de qualquer forma ou limpa
            Object val = prefs.getAll().get("current_user_id");
            if (val != null) userId = String.valueOf(val);
            // Corrige para o futuro
            prefs.edit().putString("current_user_id", userId).apply();
        }
        
        // Gera um nome de arquivo seguro (ex: entregas_db_usuario_gmail_com)
        String dbName = "entregas_db_" + userId.replaceAll("[^a-zA-Z0-9]", "_");

        // --- Lógica de Migração de Nome de Arquivo Legado ---
        // Se o novo arquivo não existe, mas o antigo "entregas_db" existe, renomeia o antigo.
        java.io.File oldFile = context.getDatabasePath("entregas_db");
        java.io.File newFile = context.getDatabasePath(dbName);
        if (oldFile.exists() && !newFile.exists()) {
            oldFile.renameTo(newFile);
            // Também renomeia arquivos auxiliares do SQLite se existirem (-shm e -wal)
            new java.io.File(oldFile.getPath() + "-shm").renameTo(new java.io.File(newFile.getPath() + "-shm"));
            new java.io.File(oldFile.getPath() + "-wal").renameTo(new java.io.File(newFile.getPath() + "-wal"));
        }

        // Se o usuário mudou, fechamos a instância antiga para abrir a nova
        if (instance != null && !instance.getOpenHelper().getDatabaseName().equals(dbName)) {
            instance.close();
            instance = null;
        }

        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, dbName)
                    .addMigrations(MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33, MIGRATION_33_34, MIGRATION_34_35, MIGRATION_35_36, MIGRATION_36_37, MIGRATION_37_38, MIGRATION_38_39, MIGRATION_39_40, MIGRATION_40_41, MIGRATION_41_42)
                    .fallbackToDestructiveMigration()
                    .addCallback(new Callback() {
                        @Override
                        public void onOpen(@NonNull SupportSQLiteDatabase db) {
                            super.onOpen(db);
                            new Thread(() -> {
                                try {
                                    db.execSQL("DELETE FROM platforms WHERE id NOT IN (SELECT MIN(id) FROM platforms GROUP BY name)");
                                    db.execSQL("DELETE FROM gas_stations WHERE id NOT IN (SELECT MIN(id) FROM gas_stations GROUP BY name)");

                                    AppDao dao = getInstance(context).appDao();
                                    
                                    if (dao.getPlatformCount() == 0) {
                                        dao.insertPlatform(new Platform("Amazon", true, 250.0, 0));
                                        dao.insertPlatform(new Platform("Mercado Livre", true, 218.0, 1));
                                        dao.insertPlatform(new Platform("Shopee", true, 229.0, 2));
                                        dao.insertPlatform(new Platform("99", true, 0.0, 3));
                                        dao.insertPlatform(new Platform("Folga / Não trabalhei", true, 0.0, 4));
                                    }
                                    
                                    if (dao.getAllGasStations().isEmpty()) {
                                        GasStation ipiranga = new GasStation("Ipiranga", 0);
                                        ipiranga.isDefault = true;
                                        dao.insertGasStation(ipiranga);
                                        dao.insertGasStation(new GasStation("Petrobras", 1));
                                        dao.insertGasStation(new GasStation("Shell", 2));
                                        dao.insertGasStation(new GasStation("Outro", 3));
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        }
                    })
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }
}
