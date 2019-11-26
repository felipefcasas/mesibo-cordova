package cordova.plugin.mesibo;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;


public class File {

    private String url;
    private String size;
    private String type;
    private String name;

    private static final String TAG = "plugin.mesibo.file";


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JSONObject toJson() {
        JSONObject file = new JSONObject();
        try {
            file.put("url", getUrl());
            file.put("size", getSize());
            file.put("type", getType());
            file.put("name", getName());
        } catch (JSONException ex) {
            Log.d(TAG, ex.toString());
        }
        return file;
    }
}
