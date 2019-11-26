package cordova.plugin.mesibo;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class Message {
    private long id;
    private long groupId;
    private long time;
    /*
        Types -> 1 = normal, 2 = file, 3 = location, 4 = sticker, 5 = event_log
    */
    private int type;
    private boolean resent;
    private boolean isIncoming;
    private String content;
    private String peer;
    private String sender;
    private Message reference;
    private File file;
    private Status status;

    private static final String TAG = "plugin.mesibo.message";

    public Message() { }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isResent() {
        return resent;
    }

    public void setResent(boolean resent) {
        this.resent = resent;
    }

    public boolean isIncoming() {
        return isIncoming;
    }

    public void setIncoming(boolean incoming) {
        isIncoming = incoming;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPeer() {
        return peer;
    }

    public void setPeer(String peer) {
        this.peer = peer;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public Message getReference() {
        return reference;
    }

    public void setReference(Message reference) {
        this.reference = reference;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public JSONObject toJson() {
        JSONObject message = new JSONObject();
        try {
            message.put("id", getId());
            message.put("content", getContent());
            message.put("type", getType());
            message.put("reference", getReference() != null ? getReference().toJson() : new JSONObject());
            message.put("resent", isResent());
            message.put("status", (getStatus() != null && !getStatus().getCode().equals("")) ?  getStatus().toJson() : new JSONObject());
            message.put("isIncoming", isIncoming());
            message.put("file", getFile() != null ? getFile().toJson() : new JSONObject());
            message.put("time", getTime());
            message.put("peer", getPeer());
            message.put("sender", getSender());
            message.put("groupId", getGroupId());
        } catch (JSONException ex) {
            Log.d(TAG, ex.toString());
        }
        return message;
    }
}