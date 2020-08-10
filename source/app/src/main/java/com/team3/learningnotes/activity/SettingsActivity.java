package com.team3.learningnotes.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.team3.learningnotes.R;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    @SuppressWarnings("deprecation")
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // 添加偏好
        addPreferencesFromResource(R.xml.settings_preferences);

        // 将EditText/List/Dialog/Ringtone首选项的摘要绑定到它们的值 其值更改时摘要将更新
        bindPreferenceSummaryToValue(findPreference("theme"));
        bindPreferenceSummaryToValue(findPreference("font_size"));
        bindPreferenceSummaryToValue(findPreference("sort_by"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            addPreferencesFromResource(R.xml.settings_preferences_md);
            SharedPreferences pref = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
            findPreference("direct_edit").setOnPreferenceChangeListener(this);
            findPreference("direct_edit").setEnabled(!pref.getBoolean("markdown", false));

            findPreference("markdown").setOnPreferenceChangeListener(this);
            findPreference("markdown").setEnabled(!pref.getBoolean("direct_edit", false));
        }
    }

    // 首选项值更改侦听器，用于更新首选项的摘要以反映其新值
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = (preference, value) -> {
        String stringValue = value.toString();

        if (preference instanceof ListPreference) {
            // 对于列表首选项，在首选项的“条目”列表中查找正确的显示值
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);

            // 设置摘要以反映新值
            preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
        } else {
            // 对于所有其他首选项，将摘要设置为该值的简单字符串表示形式
            preference.setSummary(stringValue);
        }

        return true;
    };

    /**
     * 将首选项的摘要绑定到其值。
     * 更具体地说，当首选项的值更改时，其摘要（首选项标题下方的文本行）将更新以反映该值。
     * 调用此方法后，摘要也会立即更新。
     * 确切的显示格式取决于首选项的类型。
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // 设置侦听器以监视值的变化。
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // 立即使用首选项的当前值触发侦听器。
        sBindPreferenceSummaryToValueListener.onPreferenceChange(
                preference,
                PreferenceManager.getDefaultSharedPreferences(
                        preference.getContext()).getString(preference.getKey(),
                        ""));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        switch (preference.getKey()) {
            case "direct_edit":
                findPreference("markdown").setEnabled(!(Boolean) value);
                break;
            case "markdown":
                findPreference("direct_edit").setEnabled(!(Boolean) value);
                break;
        }

        return true;
    }
}
