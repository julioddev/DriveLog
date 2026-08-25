package com.example.drivelog;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;

public class PlatformSettingsFragment extends Fragment {

    private PlatformAdapter platformAdapter;
    private GasStationAdapter stationAdapter;
    private AppDao dao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_platform_settings, container, false);

        dao = AppDatabase.getInstance(getContext()).appDao();
        
        RecyclerView rvPlatforms = view.findViewById(R.id.recyclerPlatforms);
        rvPlatforms.setLayoutManager(new LinearLayoutManager(getContext()));
        platformAdapter = new PlatformAdapter(requireContext(), new ArrayList<>(), dao);
        rvPlatforms.setAdapter(platformAdapter);
        setupDragAndDrop(rvPlatforms, true);

        RecyclerView rvStations = view.findViewById(R.id.recyclerGasStations);
        rvStations.setLayoutManager(new LinearLayoutManager(getContext()));
        stationAdapter = new GasStationAdapter(requireContext(), new ArrayList<>(), dao);
        rvStations.setAdapter(stationAdapter);
        setupDragAndDrop(rvStations, false);

        FloatingActionButton fab = view.findViewById(R.id.fabAddOption);

        dao.getAllPlatformsLive().observe(getViewLifecycleOwner(), platformAdapter::setPlatforms);
        dao.getAllGasStationsLive().observe(getViewLifecycleOwner(), stationAdapter::setStations);

        fab.setOnClickListener(v -> showAddDialog());

        return view;
    }

    private void setupDragAndDrop(RecyclerView rv, boolean isPlatform) {
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                if (isPlatform) {
                    platformAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                } else {
                    stationAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                }
                return true;
            }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
        });
        helper.attachToRecyclerView(rv);
    }

    private void showAddDialog() {
        String[] options = {"Nova Plataforma", "Novo Posto de Combustível"};
        new AlertDialog.Builder(getContext())
                .setTitle("O que deseja adicionar?")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showAddPlatformDialog();
                    else showAddStationDialog();
                })
                .show();
    }

    private void showAddPlatformDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_platform, null);
        EditText editName = dialogView.findViewById(R.id.editNewPlatformName);
        EditText editValue = dialogView.findViewById(R.id.editNewPlatformValue);

        builder.setView(dialogView)
                .setTitle("Adicionar Plataforma")
                .setPositiveButton("Salvar", (dialog, which) -> {
                    String name = editName.getText().toString();
                    String valStr = editValue.getText().toString();
                    if (!name.isEmpty()) {
                        double val = valStr.isEmpty() ? 0 : Double.parseDouble(valStr.replace(",", "."));
                        new Thread(() -> {
                            int nextIndex = dao.getAllPlatforms().size();
                            dao.insertPlatform(new Platform(name, true, val, nextIndex));
                            
                            // Trigger auto cloud sync if enabled
                            CloudSyncHelper.syncNow(requireContext(), "Nova Plataforma");
                        }).start();
                    } else {
                        Toast.makeText(getContext(), "Nome é obrigatório", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showAddStationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        EditText input = new EditText(getContext());
        input.setHint("Nome do Posto");
        
        builder.setView(input)
                .setTitle("Adicionar Posto")
                .setPositiveButton("Salvar", (dialog, which) -> {
                    String name = input.getText().toString();
                    if (!name.isEmpty()) {
                        new Thread(() -> {
                            int nextIndex = dao.getAllGasStations().size();
                            dao.insertGasStation(new GasStation(name, nextIndex));
                            
                            // Trigger auto cloud sync if enabled
                            CloudSyncHelper.syncNow(requireContext());
                        }).start();
                    } else {
                        Toast.makeText(getContext(), "Nome é obrigatório", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
