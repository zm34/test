package edu.northwestern.cbits.purple_robot_manager.activities.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import edu.northwestern.cbits.anthracite.LogService;
import edu.northwestern.cbits.purple_robot_manager.ManagerService;
import edu.northwestern.cbits.purple_robot_manager.PersistentService;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.RobotContentProvider;
import edu.northwestern.cbits.purple_robot_manager.activities.TestActivity;
import edu.northwestern.cbits.purple_robot_manager.config.LegacyJSONConfigFile;
import edu.northwestern.cbits.purple_robot_manager.db.DistancesProvider;
import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.plugins.HttpUploadPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPluginManager;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;

public class RobotPreferenceListener implements Preference.OnPreferenceClickListener,
        Preference.OnPreferenceChangeListener
{
    private Context _context = null;

    public RobotPreferenceListener(Context context)
    {
        this._context = context;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onPreferenceClick(Preference preference)
    {
        if (BaseSettingsActivity.HAPTIC_PATTERN_KEY.equals(preference.getKey()))
        {
            ListPreference listPref = (ListPreference) preference;

            String pattern = listPref.getValue();

            Intent intent = new Intent(ManagerService.HAPTIC_PATTERN_INTENT);
            intent.putExtra(ManagerService.HAPTIC_PATTERN_NAME, pattern);
            intent.setClass(this._context, ManagerService.class);

            this._context.startService(intent);

            return true;
        }
        else if (BaseSettingsActivity.PROBES_SCREEN_KEY.equals(preference.getKey()))
            return true;
        else if (BaseSettingsActivity.MANUAL_REFRESH_KEY.equals(preference.getKey()))
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context.getApplicationContext());
            SharedPreferences.Editor editor = prefs.edit();

            editor.putLong(LegacyJSONConfigFile.JSON_LAST_UPDATE, 0);
            editor.putString(LegacyJSONConfigFile.JSON_LAST_HASH, "");

            editor.commit();
            LegacyJSONConfigFile.update(this._context, true);

            ProbeManager.nudgeProbes(this._context);
            TriggerManager.getInstance(this._context).refreshTriggers(this._context);

            return true;
        }
        else if (BaseSettingsActivity.LOG_REFRESH_KEY.equals(preference.getKey()))
        {
            try
            {
                PackageInfo info = this._context.getPackageManager().getPackageInfo(this._context.getPackageName(), 0);

                Intent refreshIntent = new Intent(info.packageName + ".UPLOAD_LOGS_INTENT");
                refreshIntent.putExtra(LogService.LOG_FORCE_UPLOAD, true);
                refreshIntent.setClass(this._context, ManagerService.class);

                this._context.startService(refreshIntent);
            }
            catch (PackageManager.NameNotFoundException e)
            {
                LogManager.getInstance(this._context).logException(e);
            }

            return true;
        }
        else if (BaseSettingsActivity.ZIP_ARCHIVES_KEY.equals(preference.getKey()))
        {
            HttpUploadPlugin plugin = (HttpUploadPlugin) OutputPluginManager.sharedInstance.pluginForClass(this._context,
                    HttpUploadPlugin.class);

            if (plugin != null)
            {
                plugin.mailArchiveFiles(this._context);

                return true;
            }
        }
        else if (BaseSettingsActivity.DELETE_ARCHIVES_KEY.equals(preference.getKey()))
        {
            HttpUploadPlugin plugin = (HttpUploadPlugin) OutputPluginManager.sharedInstance.pluginForClass(this._context,
                    HttpUploadPlugin.class);

            if (plugin != null)
            {
                plugin.deleteArchiveFiles(this._context);

                return true;
            }
        }
        else if (BaseSettingsActivity.RUN_TESTS_KEY.equals(preference.getKey()))
        {
            Intent intent = new Intent(this._context, TestActivity.class);
            this._context.startActivity(intent);
        }
        else if (BaseSettingsActivity.DUMP_JSON_KEY.equals(preference.getKey()))
        {
            try
            {
                JSONObject root = new JSONObject();

                ApplicationInfo info = this._context.getApplicationInfo();
                root.put("name", this._context.getString(info.labelRes));

                PackageInfo pkgInfo = this._context.getPackageManager().getPackageInfo(info.packageName, 0);

                root.put("package_name", pkgInfo.packageName);
                root.put("version", pkgInfo.versionCode);
                root.put("version_name", pkgInfo.versionName);

                JSONObject config = null;

                if (this._context instanceof LegacySettingsActivity)
                {
                    LegacySettingsActivity settingsActivity = (LegacySettingsActivity) this._context;

                    config = this.dumpJson(settingsActivity.getPreferenceScreen());
                }
                else if (this._context instanceof SettingsActivity)
                {
                    SettingsActivity settingsActivity = (SettingsActivity) this._context;

                    config = this.dumpJson(settingsActivity.getPreferenceScreen());
                }

                root.put("configuration", config);

                File cacheDir = this._context.getExternalCacheDir();
                File configJsonFile = new File(cacheDir, "config.json");

                FileOutputStream fout = new FileOutputStream(configJsonFile);

                fout.write(root.toString(2).getBytes(Charset.defaultCharset().name()));

                fout.flush();
                fout.close();

                Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,
                        this._context.getString(R.string.message_mail_app_schema));
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                        this._context.getString(R.string.subject_mail_app_schema));
                emailIntent.setType("text/plain");

                Uri uri = Uri.fromFile(configJsonFile);
                emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
                this._context.startActivity(emailIntent);
            }
            catch (JSONException e)
            {
                LogManager.getInstance(this._context).logException(e);
            }
            catch (PackageManager.NameNotFoundException e)
            {
                LogManager.getInstance(this._context).logException(e);
            }
            catch (FileNotFoundException e)
            {
                LogManager.getInstance(this._context).logException(e);
            }
            catch (IOException e)
            {
                LogManager.getInstance(this._context).logException(e);
            }
        }
        else if (BaseSettingsActivity.RESET_KEY.equals(preference.getKey()))
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this._context);

            builder = builder.setTitle(R.string.title_clear_configuration);
            builder = builder.setMessage(R.string.message_clear_configuration);

            final RobotPreferenceListener me = this;

            builder = builder.setPositiveButton(R.string.button_clear_yes, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(me._context);
                    SharedPreferences.Editor e = prefs.edit();

                    Map<String, ?> prefMap = prefs.getAll();

                    for (String key : prefMap.keySet())
                    {
                        e.remove(key);
                    }

                    e.commit();

                    Intent intent = new Intent(PersistentService.NUDGE_PROBES);
                    intent.setClass(me._context, PersistentService.class);

                    me._context.startService(intent);

                    TriggerManager.getInstance(me._context).removeAllTriggers();
                    TriggerManager.getInstance(me._context).refreshTriggers(me._context);
                    HttpUploadPlugin.clearFiles(me._context);

                    String where = "_id != -1";

                    me._context.getContentResolver().delete(RobotContentProvider.RECENT_PROBE_VALUES, where, null);
                    me._context.getContentResolver().delete(RobotContentProvider.SNAPSHOTS, where, null);
                    me._context.getContentResolver().delete(DistancesProvider.CONTENT_URI, where, null);

                    ProbeValuesProvider.getProvider(me._context).clear(me._context);

                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            });

            builder = builder.setNegativeButton(R.string.button_clear_no, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    // TODO Auto-generated method stub
                }
            });

            builder.create().show();
        }
        else if (BaseSettingsActivity.PROBES_DISABLE_EACH_KEY.equals(preference.getKey()))
        {
            ProbeManager.disableEachProbe(this._context);

            Toast.makeText(this._context, R.string.message_disable_each_probe, Toast.LENGTH_LONG).show();
        }

        return false;
    }

    private JSONObject dumpJson(Preference preference) throws JSONException
    {
        JSONObject prefJson = new JSONObject();

        if (preference.getKey() != null)
            prefJson.put("key", preference.getKey());

        if (preference.getTitle() != null)
            prefJson.put("title", preference.getTitle());

        Log.e("PR", "PREF THUS FAR: " + prefJson.toString(2));

        try {
            if (preference.getSummary() != null)
                prefJson.put("summary", preference.getSummary());
        }
        catch (ArrayIndexOutOfBoundsException e) // Lollipop bug?
        {
            LogManager.getInstance(preference.getContext()).logException(e);
        }

        if (preference instanceof PreferenceGroup)
        {
            if ("config_settings_trigger_category".equals(preference.getKey()))
                return null;
            else
            {
                PreferenceGroup group = (PreferenceGroup) preference;

                if (group.getPreferenceCount() == 0)
                    return null;

                prefJson.put("type", "group");

                JSONArray children = new JSONArray();

                for (int i = 0; i < group.getPreferenceCount(); i++)
                {
                    JSONObject child = this.dumpJson(group.getPreference(i));

                    if (child != null)
                        children.put(child);
                }

                prefJson.put("children", children);
            }
        }
        else if (preference instanceof CheckBoxPreference)
            prefJson.put("type", "boolean");
        else if (preference instanceof EditTextPreference)
            prefJson.put("type", "string");
        else if (preference instanceof ListPreference)
        {
            ListPreference list = (ListPreference) preference;
            prefJson.put("type", "list");

            JSONArray entries = new JSONArray();

            for (CharSequence cs : list.getEntries())
                entries.put(cs);

            prefJson.put("labels", entries);

            JSONArray values = new JSONArray();

            for (CharSequence cs : list.getEntryValues())
                values.put(cs);

            prefJson.put("values", values);
        }
        else
            prefJson = null;

        return prefJson;
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object value)
    {
        if (BaseSettingsActivity.CHECK_UPDATES_KEY.equals(pref.getKey()))
        {
            Toast.makeText(this._context, R.string.message_update_check, Toast.LENGTH_LONG).show();

            return true;
        }
        else if (BaseSettingsActivity.RINGTONE_KEY.equals(pref.getKey()))
        {
            String name = ManagerService.soundNameForPath(this._context, value.toString());

            Intent playIntent = new Intent(ManagerService.RINGTONE_INTENT);

            if (name != null)
                playIntent.putExtra(BaseSettingsActivity.RINGTONE_KEY, name);

            playIntent.setClass(this._context, ManagerService.class);

            this._context.startService(playIntent);

            return true;
        }
        else if (LogManager.ENABLED.equals(pref.getKey()))
        {
            LogManager.getInstance(this._context).setEnabled(((Boolean) value));

            return true;
        }
        else if (LogManager.URI.equals(pref.getKey()))
        {
            LogManager.getInstance(this._context).setEndpoint(value.toString());

            return true;
        }
        else if (LogManager.INCLUDE_LOCATION.equals(pref.getKey()))
        {
            LogManager.getInstance(this._context).setIncludeLocation(((Boolean) value));

            return true;
        }
        else if (LogManager.UPLOAD_INTERVAL.equals(pref.getKey()))
        {
            LogManager.getInstance(this._context).setUploadInterval(Long.parseLong(value.toString()));

            return true;
        }
        else if (LogManager.WIFI_ONLY.equals(pref.getKey()))
        {
            LogManager.getInstance(this._context).setWifiOnly(((Boolean) value));

            return true;
        }
        else if (LogManager.LIBERAL_SSL.equals(pref.getKey()))
        {
            LogManager.getInstance(this._context).setLiberalSsl(((Boolean) value));

            return true;
        }
        else if (LogManager.HEARTBEAT.equals(pref.getKey()))
        {
            LogManager.getInstance(this._context).setHeartbeat(((Boolean) value));

            return true;
        }

        return false;
    }
}