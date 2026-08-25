package com.example.drivelog;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EarningsFragment extends Fragment implements EarningsAdapter.OnEarningsClickListener {

    private TextInputEditText editDate, editExtra, editTotal;
    private TextInputLayout layoutExtra;
    private RadioGroup rgPlatforms;
    private Button btnSave, btnCancel;
    private Calendar selectedDate = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private SharedPreferences sharedPreferences;

    private RecyclerView recyclerHistory;
    private EarningsAdapter adapter;
    private int editingEarningsId = -1;
    private boolean isUpdatingUI = false;
    private double currentBase = 0;
    private List<Platform> dbPlatforms = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_earnings, container, false);

        editDate = view.findViewById(R.id.editEarningsDate);
        rgPlatforms = view.findViewById(R.id.rgPlatforms);
        layoutExtra = view.findViewById(R.id.layoutExtra);
        editExtra = view.findViewById(R.id.editExtraValue);
        editTotal = view.findViewById(R.id.editEarningsTotal);
        btnSave = view.findViewById(R.id.btnSaveEarnings);
        btnCancel = view.findViewById(R.id.btnCancelEdit);

        recyclerHistory = view.findViewById(R.id.recyclerEarningsHistory);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EarningsAdapter(new ArrayList<>(), this);
        recyclerHistory.setAdapter(adapter);

        recyclerHistory.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull android.view.MotionEvent e) {
                if (e.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    rv.getParent().requestDisallowInterceptTouchEvent(true);
                }
                return false;
            }
            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull android.view.MotionEvent e) {}
            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
        });

        setupSwipeActions();

        sharedPreferences = requireActivity().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);

        updateDateLabel();
        editDate.setOnClickListener(v -> showDatePicker());

        rgPlatforms.setOnCheckedChangeListener((group, checkedId) -> {
            Platform selected = null;
            for (Platform p : dbPlatforms) {
                if (p.id == checkedId) {
                    selected = p;
                    break;
                }
            }

            if (selected != null) {
                // Shopee e outras liberam valor extra. 99 e Folga não.
                boolean allowExtra = !selected.name.equalsIgnoreCase("99") && 
                                   !selected.name.toLowerCase().contains("folga");
                
                layoutExtra.setVisibility(allowExtra ? View.VISIBLE : View.GONE);
                if (!allowExtra) editExtra.setText("");

                if (!isUpdatingUI) {
                    currentBase = selected.defaultValue;
                }
            } else {
                layoutExtra.setVisibility(View.GONE);
                editExtra.setText("");
            }

            if (editingEarningsId == -1 && selected != null && selected.name.equalsIgnoreCase("99")) {
                editTotal.setText("");
            }
            updateTotalFromCurrentSelection();
        });

        editExtra.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTotalFromCurrentSelection();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnSave.setOnClickListener(v -> saveEarnings());
        btnCancel.setOnClickListener(v -> cancelEdit());

        loadPlatforms();
        updateHistory();

        return view;
    }

    private void loadPlatforms() {
        new Thread(() -> {
            dbPlatforms = AppDatabase.getInstance(getContext()).appDao().getAllPlatforms();
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::populatePlatformRadioGroup);
            }
        }).start();
    }

    private void populatePlatformRadioGroup() {
        rgPlatforms.removeAllViews();
        for (Platform p : dbPlatforms) {
            if (p.isEnabled) {
                RadioButton rb = new RadioButton(getContext());
                rb.setId(p.id);
                rb.setText(p.name);
                rgPlatforms.addView(rb);
            }
        }
    }

    private void setupSwipeActions() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                Earnings earnings = adapter.getEarningsAt(position);
                if (direction == ItemTouchHelper.LEFT) {
                    onEarningsClick(earnings);
                    adapter.notifyItemChanged(position);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    showDeleteConfirmation(earnings, position);
                }
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerHistory);
    }

    private void showDeleteConfirmation(Earnings earnings, int position) {
        UiHelper.showBottomSheetConfirm(
                requireContext(),
                "Excluir Ganho?",
                "Deseja realmente excluir este registro de ganho?",
                "EXCLUIR",
                () -> {
                    new Thread(() -> {
                        AppDatabase.getInstance(getContext()).appDao().deleteEarnings(earnings);
                        updateHistory();
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Registro excluído", Toast.LENGTH_SHORT).show();
                                CloudSyncHelper.syncNow(requireContext(), "Ganho excluído");
                            });
                        }
                    }).start();
                }
        );
    }

    private void updateHistory() {
        new Thread(() -> {
            List<Earnings> recent = AppDatabase.getInstance(getContext()).appDao().getRecentEarnings();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> adapter.setEarningsList(recent));
            }
        }).start();
    }

    @Override
    public void onEarningsClick(Earnings earnings) {
        isUpdatingUI = true;
        editingEarningsId = earnings.id;
        currentBase = earnings.baseValue;
        selectedDate.setTimeInMillis(earnings.date);
        updateDateLabel();

        // Procura a plataforma salva para marcar o RadioButton
        for (Platform p : dbPlatforms) {
            if (p.name.equalsIgnoreCase(earnings.platforms)) {
                rgPlatforms.check(p.id);
                break;
            }
        }

        // Regra de exibição do campo extra
        boolean isService = !earnings.platforms.equalsIgnoreCase("99") && 
                           !earnings.platforms.toLowerCase().contains("folga");
        layoutExtra.setVisibility(isService ? View.VISIBLE : View.GONE);

        editExtra.setText(String.format(Locale.getDefault(), "%.2f", earnings.extraValue));
        editTotal.setText(String.format(Locale.getDefault(), "%.2f", earnings.totalValue));

        btnSave.setText("Atualizar Ganho");
        btnCancel.setVisibility(View.VISIBLE);
        isUpdatingUI = false;
        Toast.makeText(getContext(), "Modo de edição ativado", Toast.LENGTH_SHORT).show();
    }

    @Override public void onEarningsLongClick(Earnings earnings) {}

    @Override
    public void onSaveEdit(Earnings earnings, int position) {
        new Thread(() -> {
            AppDatabase.getInstance(getContext()).appDao().updateEarnings(earnings);
            updateHistory();
        }).start();
        Toast.makeText(getContext(), "Ganho atualizado!", Toast.LENGTH_SHORT).show();
    }

    private void cancelEdit() {
        editingEarningsId = -1;
        rgPlatforms.clearCheck();
        editExtra.setText("");
        editTotal.setText("");
        selectedDate = Calendar.getInstance();
        updateDateLabel();
        btnSave.setText("Salvar Ganhos");
        btnCancel.setVisibility(View.GONE);
    }

    private void showDatePicker() {
        new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            selectedDate.set(Calendar.YEAR, year);
            selectedDate.set(Calendar.MONTH, month);
            selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateLabel();
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateLabel() {
        editDate.setText(dateFormat.format(selectedDate.getTime()));
    }

    private void updateTotalFromCurrentSelection() {
        if (isUpdatingUI) return;
        int checkedId = rgPlatforms.getCheckedRadioButtonId();
        if (checkedId == -1) return;

        double extra = parseDouble(editExtra.getText().toString());
        editTotal.setText(String.format(Locale.getDefault(), "%.2f", currentBase + extra));
    }

    private void saveEarnings() {
        int checkedId = rgPlatforms.getCheckedRadioButtonId();
        if (checkedId == -1) {
            Toast.makeText(getContext(), "Selecione uma plataforma", Toast.LENGTH_SHORT).show();
            return;
        }

        Platform selected = null;
        for (Platform p : dbPlatforms) {
            if (p.id == checkedId) {
                selected = p;
                break;
            }
        }

        if (selected == null) return;

        double extra = parseDouble(editExtra.getText().toString());
        double total = parseDouble(editTotal.getText().toString());

        Earnings earnings = new Earnings();
        if (editingEarningsId != -1) earnings.id = editingEarningsId;
        
        earnings.baseValue = currentBase;
        earnings.extraValue = extra;
        earnings.totalValue = total;
        earnings.platforms = selected.name;
        earnings.date = selectedDate.getTimeInMillis();

        new Thread(() -> {
            if (editingEarningsId == -1) {
                AppDatabase.getInstance(getContext()).appDao().insertEarnings(earnings);
            } else {
                AppDatabase.getInstance(getContext()).appDao().updateEarnings(earnings);
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Ganho salvo!", Toast.LENGTH_SHORT).show();
                    cancelEdit();
                    updateHistory();
                    CloudSyncHelper.syncNow(requireContext(), editingEarningsId == -1 ? "Novo Ganho" : "Ganho Editado");
                });
            }
        }).start();
    }

    private double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (Exception e) { return 0; }
    }
}
