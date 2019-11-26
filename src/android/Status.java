package cordova.plugin.mesibo;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Status {
    private int value;
    private String label;
    private String code;
    private String icon;
    private String cssClass;

    private static final String TAG = "plugin.mesibo.status";

    public Status(int value, String label, String code, String icon, String cssClass) {
        this.value = value;
        this.label = label;
        this.code = code;
        this.icon = icon;
        this.cssClass = cssClass;
    }

    public Status() {
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public JSONObject toJson() {
        JSONObject message = new JSONObject();
        try {
            message.put("value", getValue());
            message.put("label", getLabel());
            message.put("code", getCode());
            message.put("icon", getIcon());
            message.put("cssClass", getCssClass());
        } catch (JSONException ex) {
            Log.d(TAG, ex.toString());
        }
        return message;
    }

    public static ArrayList<Status> getStatuses() {
        ArrayList<Status> statuses = new ArrayList<Status>();
        statuses.add(new Status(136, "Bloqueado", "BLOCKED", "", ""));
        statuses.add(new Status(22, "LLamada entrante", "CALLINCOMING", "", ""));
        statuses.add(new Status(21, "LLamada perdida", "CALLMISSED", "", ""));
        statuses.add(new Status(23, "Llamando", "CALLOUTGOING", "", ""));
        statuses.add(new Status(32, "Personalizado", "CUSTOM", "", ""));
        statuses.add(new Status(2, "Entregado", "DELIVERED", "md-done-all", "msg-delivered"));
        statuses.add(new Status(132, "Expirado", "EXPIRED","md-time", "msg-sending"));
        statuses.add(new Status(128, "Fallido", "FAIL", "", "msg-failed"));
        statuses.add(new Status(130, "Buzón lleno", "INBOXFULL", "", ""));
        statuses.add(new Status(131, "Destino inválido", "INVALIDDEST", "", ""));
        statuses.add(new Status(0, "Enviando", "OUTBOX", "md-time", "msg-sending"));
        statuses.add(new Status(3, "Leído", "READ", "md-done-all", "msg-read"));
        statuses.add(new Status(18, "Noticia recibida", "RECEIVEDNEW", "md-done-all", "msg-delivered"));
        statuses.add(new Status(19, "Leído", "RECEIVEDREAD", "md-done-all","msg-read"));
        statuses.add(new Status(1, "Enviado", "SENT", "md-checkmark", "msg-sent"));
        statuses.add(new Status(129, "Usuario desconectado", "USEROFFLINE", "", ""));
        return statuses;
    }

    public  static Status findStatus(int value) {
        Status status = new Status();
        ArrayList<Status> statuses = Status.getStatuses();
        for(int i = 0; i < statuses.size(); i++) {
            Status temp = statuses.get(i);

            if(temp != null && temp.value == (value)) {
                status = temp;
                break;
            }
        }
        return status;
    }
}
