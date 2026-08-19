package com.example.drivelog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.viewpager2.widget.ViewPager2;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DevRouteDetectionFragment extends Fragment {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    private RecyclerView recyclerView, recyclerLogs;
    private NotificationAdapter adapter;
    private LogAdapter adapterLogs;
    private List<DetectedNotification> notifications = new ArrayList<>();
    private List<DetectedNotification> scannerLogs = new ArrayList<>();
    private MaterialButton btnRequestAccess;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            long time = intent.getLongExtra("time", System.currentTimeMillis());

            if ("com.example.drivelog.SCANNER_DEBUG_LOG".equals(action)) {
                String logText = intent.getStringExtra("text");
                if (logText != null && !logText.trim().isEmpty()) {
                    addLog(new DetectedNotification("REGISTRO", logText, time));
                }
                return;
            }

            String title = getString(R.string.det_notification);
            String text = "";

            if (NotificationService.ACTION_NOTIFICATION_RECEIVED.equals(action)) {
                title = intent.getStringExtra("title");
                text = intent.getStringExtra("text");
            } else if (ScannerService.ACTION_ROUTE_DETECTED.equals(action)) {
                String extraTitle = intent.getStringExtra("title");
                title = extraTitle != null ? extraTitle : getString(R.string.det_route_screen);
                text = intent.getStringExtra("message");
            }
            
            notifications.add(0, new DetectedNotification(title, text, time));
            if (adapter != null) {
                adapter.notifyItemInserted(0);
                if (recyclerView != null) recyclerView.scrollToPosition(0);
            }
        }
    };

    private void addLog(DetectedNotification log) {
        if (!scannerLogs.isEmpty() && scannerLogs.get(0).text.equals(log.text)) return;

        scannerLogs.add(0, log);
        if (scannerLogs.size() > 50) scannerLogs.remove(50);
        if (adapterLogs != null) {
            adapterLogs.notifyDataSetChanged();
            if (recyclerLogs != null) recyclerLogs.scrollToPosition(0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 🔥 Carrega alertas persistentes (do disco) ao abrir a tela
        notifications.clear();
        for (ScannerAlertManager.AlertModel am : ScannerAlertManager.getAlerts(requireContext())) {
            notifications.add(new DetectedNotification(am.title, am.message, am.timestamp));
        }

        View view = inflater.inflate(R.layout.fragment_dev_route_detection, container, false);

        tabLayout = view.findViewById(R.id.tabLayoutDev);
        viewPager = view.findViewById(R.id.viewPagerDev);

        setupViewPager();

        return view;
    }

    private void setupViewPager() {
        viewPager.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v;
                if (viewType == 0) {
                    v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_dev_detection_list, parent, false);
                } else {
                    v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_dev_detection_logs, parent, false);
                }
                return new RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                if (position == 0) {
                    setupDetectionView(holder.itemView);
                } else {
                    setupLogsView(holder.itemView);
                }
            }

            @Override
            public int getItemCount() { return 2; }

            @Override
            public int getItemViewType(int position) { return position; }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? getString(R.string.tab_alerts) : getString(R.string.tab_logs));
        }).attach();
    }

    private void setupDetectionView(View view) {
        recyclerView = view.findViewById(R.id.recyclerDetectedNotifications);
        btnRequestAccess = view.findViewById(R.id.btnRequestNotificationAccess);
        MaterialButton btnHelp = view.findViewById(R.id.btnHelpAccessibility);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(notifications);
        recyclerView.setAdapter(adapter);

        if (btnRequestAccess != null) {
            btnRequestAccess.setOnClickListener(v -> {
                String listeners = Settings.Secure.getString(requireContext().getContentResolver(), "enabled_notification_listeners");
                boolean hasNotifAccess = listeners != null && listeners.contains(requireContext().getPackageName());

                if (!hasNotifAccess) {
                    try {
                        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                        startActivity(intent);
                    } catch (Exception e) {
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    }
                } else {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    try {
                        startActivity(intent);
                        Toast.makeText(getContext(), R.string.msg_look_for_drivelog, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), R.string.error_opening_settings, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (btnHelp != null) {
            btnHelp.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(R.string.msg_samsung_android13_help_title)
                    .setMessage(R.string.msg_samsung_android13_help_content)
                    .setPositiveButton(R.string.btn_open_details, (d, w) -> {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                        startActivity(intent);
                    })
                    .setNegativeButton(R.string.btn_back, null)
                    .show();
            });
        }
        
        checkNotificationAccess();
    }

    private void setupLogsView(View view) {
        recyclerLogs = view.findViewById(R.id.recyclerScannerLogs);
        recyclerLogs.setLayoutManager(new LinearLayoutManager(getContext()));
        adapterLogs = new LogAdapter(scannerLogs);
        recyclerLogs.setAdapter(adapterLogs);
    }

    @Override
    public void onResume() {
        super.onResume();
        checkNotificationAccess();
        IntentFilter filter = new IntentFilter();
        filter.addAction(NotificationService.ACTION_NOTIFICATION_RECEIVED);
        filter.addAction(ScannerService.ACTION_ROUTE_DETECTED);
        filter.addAction("com.example.drivelog.SCANNER_DEBUG_LOG");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // APIs 26-32 suportam o flag se necessário, mas o sistema costuma aceitar sem.
            // Forçamos NOT_EXPORTED para segurança.
            requireContext().registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(receiver, filter);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        requireContext().unregisterReceiver(receiver);
    }

    private void checkNotificationAccess() {
        String listeners = Settings.Secure.getString(requireContext().getContentResolver(), "enabled_notification_listeners");
        boolean hasNotifAccess = listeners != null && listeners.contains(requireContext().getPackageName());
        
        String accessibility = Settings.Secure.getString(requireContext().getContentResolver(), "enabled_accessibility_services");
        boolean hasScannerAccess = accessibility != null && accessibility.contains(requireContext().getPackageName());

        if (btnRequestAccess != null) {
            btnRequestAccess.setVisibility((hasNotifAccess && hasScannerAccess) ? View.GONE : View.VISIBLE);
            if (!hasNotifAccess) btnRequestAccess.setText(R.string.btn_activate_notif_access);
            else if (!hasScannerAccess) btnRequestAccess.setText(R.string.btn_activate_scanner_access);
        }
    }

    private static class DetectedNotification {
        String title, text;
        long time;
        DetectedNotification(String t, String txt, long ti) { this.title = t; this.text = txt; this.time = ti; }
    }

    private static class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {
        private final List<DetectedNotification> list;
        private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        LogAdapter(List<DetectedNotification> list) { this.list = list; }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_scanner_log, p, false));
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            DetectedNotification n = list.get(pos);
            h.time.setText("[" + sdf.format(new Date(n.time)) + "]");
            h.content.setText(n.text);
        }

        @Override public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView content, time;
            ViewHolder(View v) { 
                super(v); 
                time = v.findViewById(R.id.textLogTime); 
                content = v.findViewById(R.id.textLogContent); 
            }
        }
    }

    private static class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
        private final List<DetectedNotification> list;
        private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        NotificationAdapter(List<DetectedNotification> list) { this.list = list; }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_detected_notification, p, false));
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            DetectedNotification n = list.get(pos);
            h.title.setText(n.title);
            h.content.setText(n.text);
            h.time.setText(sdf.format(new Date(n.time)));
        }

        @Override public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, content, time;
            ViewHolder(View v) { super(v); title = v.findViewById(R.id.textNotifTitle); content = v.findViewById(R.id.textNotifContent); time = v.findViewById(R.id.textNotifTime); }
        }
    }
}