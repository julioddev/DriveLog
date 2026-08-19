package com.example.drivelog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

public class UiHelper {

    public interface OnConfirmListener {
        void onConfirm();
    }

    public static void showBottomSheetConfirm(Context context, String title, String message, String positiveBtn, OnConfirmListener listener) {
        showBottomSheetConfirm(context, title, message, positiveBtn, listener, null);
    }

    public static void showBottomSheetConfirm(Context context, String title, String message, String positiveBtn, OnConfirmListener listener, Runnable onDismiss) {
        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.AppBottomSheetDialogTheme);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_confirm, null);
        dialog.setContentView(view);

        TextView txtTitle = view.findViewById(R.id.textSheetTitle);
        TextView txtMessage = view.findViewById(R.id.textSheetMessage);
        MaterialButton btnPositive = view.findViewById(R.id.btnSheetPositive);
        MaterialButton btnNegative = view.findViewById(R.id.btnSheetNegative);

        txtTitle.setText(title);
        txtMessage.setText(message);
        if (positiveBtn != null) btnPositive.setText(positiveBtn);

        btnPositive.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) listener.onConfirm();
        });

        btnNegative.setOnClickListener(v -> dialog.dismiss());

        if (onDismiss != null) {
            dialog.setOnDismissListener(d -> onDismiss.run());
        }

        dialog.show();
    }
}
