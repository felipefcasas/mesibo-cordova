package cordova.plugin.mesibo;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Activity {

    private long id;
    private long time;
    private long groupId;
    private int activity;
    private String peer;
    private Profile profile;

    private static final String TAG = "plugin.mesibo.activity";


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public int getActivity() {
        return activity;
    }

    public void setActivity(int activity) {
        this.activity = activity;
    }

    public String getPeer() {
        return peer;
    }

    public void setPeer(String peer) {
        this.peer = peer;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public JSONObject toJson() {
        JSONObject activity = new JSONObject();
        try {
            activity.put("id", getId());
            activity.put("time", getTime());
            activity.put("groupId", getGroupId());
            activity.put("activity", getActivity());
            activity.put("peer", getPeer());
            activity.put("profile", getProfile().toJson());
        } catch (JSONException ex) {
            Log.d(TAG, ex.toString());
        }
        return activity;
    }
}
