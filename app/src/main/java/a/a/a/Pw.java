package a.a.a;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.besome.sketch.beans.ProjectFileBean;
import pro.sketchware.R;
import pro.sketchware.databinding.PropertyPopupSelectorSingleBinding;

import java.util.ArrayList;

import mod.hey.studios.util.Helper;

@SuppressLint("ViewConstructor")
public class Pw extends RelativeLayout implements View.OnClickListener {

    private String key = "";
    private String value = "";
    private TextView tvName;
    private TextView tvValue;
    private ImageView imgLeftIcon;
    private int f;
    private View propertyItem;
    private View propertyMenuItem;
    private Kw propertyValueChangeListener;
    private ArrayList<ProjectFileBean> customViews;

    public Pw(Context context, boolean idk) {
        super(context);
        a(idk);
    }

    private RadioButton a(String fileName) {
        RadioButton radioButton = new RadioButton(getContext());
        radioButton.setText(fileName);
        radioButton.setTag(fileName);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, (int) (wB.a(getContext(), 1.0F) * 40.0F));
        radioButton.setGravity(19);
        radioButton.setLayoutParams(layoutParams);
        return radioButton;
    }

    private void a() {
        aB dialog = new aB((Activity) getContext());
        dialog.b(Helper.getText(tvName));
        dialog.a(f);
        PropertyPopupSelectorSingleBinding propertyBinding = PropertyPopupSelectorSingleBinding.inflate(((Activity) getContext()).getLayoutInflater());
        RadioGroup rgContent = propertyBinding.rgContent;
        rgContent.addView(a("none"));

        for (ProjectFileBean projectFileBean : customViews) {
            RadioButton var4 = a(projectFileBean.fileName);
            propertyBinding.rgContent.addView(var4);
        }

        ((RadioButton) rgContent.getChildAt(0)).setChecked(true);

        for (int i = 0, childCount = rgContent.getChildCount(); i < childCount; i++) {
            RadioButton radioButton = (RadioButton) rgContent.getChildAt(i);
            if (radioButton.getTag().toString().equals(value)) {
                radioButton.setChecked(true);
            }
        }

        dialog.a(propertyBinding.getRoot());
        dialog.b(xB.b().a(getContext(), R.string.common_word_select), v -> {
            for (int i = 0, childCount = rgContent.getChildCount(); i < childCount; i++) {
                RadioButton radioButton = (RadioButton) rgContent.getChildAt(i);

                if (radioButton.isChecked()) {
                    setValue(radioButton.getTag().toString());
                }
            }
            if (propertyValueChangeListener != null) {
                propertyValueChangeListener.a(key, value);
            }
            dialog.dismiss();
        });
        dialog.a(xB.b().a(getContext(), R.string.common_word_cancel), Helper.getDialogDismissListener(dialog));
        dialog.show();
    }

    private void a(boolean var2) {
        wB.a(getContext(), this, R.layout.property_selector_item);
        tvName = (TextView) findViewById(R.id.tv_name);
        tvValue = (TextView) findViewById(R.id.tv_value);
        propertyItem = findViewById(R.id.property_item);
        propertyMenuItem = findViewById(R.id.property_menu_item);
        imgLeftIcon = (ImageView) findViewById(R.id.img_left_icon);
        if (var2) {
            setOnClickListener(this);
            setSoundEffectsEnabled(true);
        }

    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void onClick(View var1) {
        if (!mB.a()) {
            if ("property_custom_view_listview".equals(key)) {
                a();
            }
        }
    }

    public void setCustomView(ArrayList<ProjectFileBean> customView) {
        customViews = customView;
    }

    public void setKey(String key) {
        this.key = key;
        int var2 = getResources().getIdentifier(key, "string", getContext().getPackageName());
        if (var2 > 0) {
            tvName.setText(xB.b().a(getResources(), var2));
            f = R.drawable.ic_mtrl_interface;
            if (propertyMenuItem.getVisibility() == View.VISIBLE) {
                ImageView var3 = (ImageView) findViewById(R.id.img_icon);
                TextView var4 = (TextView) findViewById(R.id.tv_title);
                var3.setImageResource(f);
                var4.setText(xB.b().a(getContext(), var2));
            } else {
                imgLeftIcon.setImageResource(f);
            }
        }

    }

    public void setOnPropertyValueChangeListener(Kw var1) {
        propertyValueChangeListener = var1;
    }

    public void setOrientationItem(int orientationItem) {
        if (orientationItem == 0) {
            propertyItem.setVisibility(View.GONE);
            propertyMenuItem.setVisibility(View.VISIBLE);
        } else {
            propertyItem.setVisibility(View.VISIBLE);
            propertyMenuItem.setVisibility(View.GONE);
        }

    }

    public void setValue(String value) {
        if (TextUtils.isEmpty(value)) {
            value = "none";
        }
        this.value = value;
        tvValue.setText(value);
    }
}
