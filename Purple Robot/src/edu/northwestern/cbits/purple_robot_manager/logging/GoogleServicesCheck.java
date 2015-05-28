package edu.northwestern.cbits.purple_robot_manager.logging;

import android.app.PendingIntent;
import android.content.Context;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import edu.northwestern.cbits.purple_robot_manager.R;

public class GoogleServicesCheck extends SanityCheck
{
    private int _status = 0;

    public String name(Context context)
    {
        return context.getString(R.string.name_sanity_google_play);
    }

    public Runnable getAction(final Context context)
    {
        Runnable action = null;

        final GoogleServicesCheck me = this;

        switch (this._status)
        {
            case ConnectionResult.SERVICE_INVALID:
            case ConnectionResult.SERVICE_DISABLED:
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
            case ConnectionResult.SERVICE_MISSING:
                action = new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            PendingIntent intent = GooglePlayServicesUtil.getErrorPendingIntent(me._status, context, 0);
                            intent.send();
                        }
                        catch (PendingIntent.CanceledException e)
                        {
                            LogManager.getInstance(context).logException(e);
                        }
                    }
                };

                break;
            case ConnectionResult.SUCCESS:
                action = null;

                break;
        }

        return action;
    }

    public void runCheck(Context context)
    {
        this._status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);

        switch (this._status)
        {
            case ConnectionResult.SUCCESS:
                this._errorLevel = SanityCheck.OK;
                this._errorMessage = context.getString(R.string.name_sanity_google_play_update_ok);

                break;
            case ConnectionResult.SERVICE_INVALID:
                this._errorLevel = SanityCheck.ERROR;
                this._errorMessage = context.getString(R.string.name_sanity_google_play_update_invalid);

                break;
            case ConnectionResult.SERVICE_DISABLED:
                this._errorLevel = SanityCheck.ERROR;
                this._errorMessage = context.getString(R.string.name_sanity_google_play_update_disabled);

                break;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                this._errorLevel = SanityCheck.ERROR;
                this._errorMessage = context.getString(R.string.name_sanity_google_play_update_required);

                break;
            case ConnectionResult.SERVICE_MISSING:
                this._errorLevel = SanityCheck.ERROR;
                this._errorMessage = context.getString(R.string.name_sanity_google_play_missing);

                break;
        }
    }
}
