package cordova.plugin.mesibo;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Profile {

    private long groupId;
    private int unread;
    private long lastSeen;
    private String address;
    private String status;

    private static final String TAG = "plugin.mesibo.profile";


    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public int getUnread() {
        return unread;
    }

    public void setUnread(int unread) {
        this.unread = unread;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public JSONObject toJson() {
        JSONObject profile = new JSONObject();
        try {
            profile.put("groupId", getGroupId());
            profile.put("unread", getUnread());
            profile.put("lastSeen", getLastSeen());
            profile.put("address", getAddress());
            profile.put("status", getStatus());
        } catch (JSONException ex) {
            Log.d(TAG, ex.toString());
        }
        return profile;
    }
}
