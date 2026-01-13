package mod.hilal.saif.activities.tools;

import android.content.Context;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.besome.sketch.editor.manage.library.LibraryItemView;
import com.google.android.material.checkbox.MaterialCheckBox;

public class AppSettingsToggleItemView extends LibraryItemView {
    private final MaterialCheckBox checkBox;
    private boolean suppressListener = false;
    private CompoundButton.OnCheckedChangeListener listener;

    public AppSettingsToggleItemView(Context context) {
        super(context);
        setHideEnabled();

        checkBox = new MaterialCheckBox(context);
        checkBox.setLayoutParams(createCheckboxLayoutParams(context));
        checkBox.setClickable(false);
        checkBox.setFocusable(false);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressListener || listener == null) {
                return;
            }
            listener.onCheckedChanged(buttonView, isChecked);
        });
        container.addView(checkBox);

        container.setOnClickListener(v -> checkBox.toggle());
    }

    public void setChecked(boolean checked) {
        checkBox.setChecked(checked);
    }

    public void setCheckedSilently(boolean checked) {
        suppressListener = true;
        checkBox.setChecked(checked);
        suppressListener = false;
    }

    public boolean isChecked() {
        return checkBox.isChecked();
    }

    public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    private ConstraintLayout.LayoutParams createCheckboxLayoutParams(Context context) {
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());
        params.setMarginEnd(margin);
        return params;
    }
}
