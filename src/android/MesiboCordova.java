package cordova.plugin.mesibo;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import java.util.ArrayList;
import java.util.Date;

import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import com.mesibo.api.Mesibo;
import com.mesibo.api.MesiboCore;
import com.mesibo.calls.MesiboCall;


public class MesiboCordova extends CordovaPlugin implements Mesibo.MessageListener, Mesibo.UserProfileLookupListener {

    private static Context context = null;

    private Mesibo.ReadDbSession readDbSession = null;

    // Callbacks para los eventos de mesibo
    private static CallbackContext callbackOnMessage = null;
    private static CallbackContext callbackOnMessageStatus = null;
    private static CallbackContext callbackOnActivity = null;
    private static CallbackContext callbackOnUserProfile = null;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        MesiboCordova.context = this.cordova.getActivity().getApplicationContext();
        Mesibo.getInstance().init(MesiboCordova.context);
        MesiboCall.getInstance().init(MesiboCordova.context);
        Mesibo.addListener(this);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        // Setea el token para acceder a los servicios de mesibo (token del usuario actual)
        if (action.equals("setAccessToken")) {
            String token = args.getJSONObject(0).getString("token");
            this.setAccessToken(token, callbackContext);
            return true;
        }

        // Setea el token para acceder a los servicios de mesibo (token del usuario actual)
        if (action.equals("stopMesibo")) {
            this.stopMesibo(callbackContext);
            return true;
        }

        // Obtiene de la base de datos local, una cantidad dinámica de mensajes de un "peer"
        if (action.equals("readProfileMessages")) {
            String address = args.getJSONObject(0).getString("address");
            int messageCount = Integer.parseInt(args.getJSONObject(0).getString("messageCount"));
            long groupId = Long.parseLong(args.getJSONObject(0).getString("groupId"));
            boolean enableFifo = Boolean.parseBoolean(args.getJSONObject(0).getString("enableFifo"));
            boolean enableReadReceipt = Boolean.parseBoolean(args.getJSONObject(0).getString("enableReadReceipt"));
            this.readProfileMessages(address, groupId, messageCount, enableFifo, enableReadReceipt, callbackContext);
            return true;
        }

        // Obtiene de la base de datos local, una cantidad dinámica de mensajes de un "peer"
        if (action.equals("readRoomMessages")) {
            String address = args.getJSONObject(0).getString("address");
            int messageCount = Integer.parseInt(args.getJSONObject(0).getString("messageCount"));
            long groupId = Long.parseLong(args.getJSONObject(0).getString("groupId"));
            boolean enableFifo = Boolean.parseBoolean(args.getJSONObject(0).getString("enableFifo"));
            boolean enableReadReceipt = Boolean.parseBoolean(args.getJSONObject(0).getString("enableReadReceipt"));
            this.readRoomMessages(address, groupId, messageCount, enableFifo, enableReadReceipt, callbackContext);
            return true;
        }

        // Elimina una sesión de lectura
        if (action.equals("stopReadDbSession")) {
            this.stopReadDbSession(callbackContext);
            return true;
        }

        // Elimina todos los mensajes de una conversación
        if (action.equals("deleteRoomMessages")) {
            String peer = args.getJSONObject(0).getString("peer");
            int groupId = Integer.parseInt(args.getJSONObject(0).getString("groupId"));
            this.deleteRoomMessages(peer, groupId, callbackContext);
            return true;
        }

        // Envía un mensaje a una sala
        if (action.equals("sendMessage")) {
            String peer = args.getJSONObject(0).getString("peer");
            String groupId = args.getJSONObject(0).getString("groupId");
            String message = args.getJSONObject(0).getString("message");
            String sender = args.getJSONObject(0).getString("sender");
            String type = args.getJSONObject(0).getString("type");
            this.sendMessage(peer, Long.parseLong(groupId), Integer.parseInt(type), message, sender, callbackContext);
            return true;
        }

        // Envía actividad a una sala
        if (action.equals("sendActivity")) {
            String peer = args.getJSONObject(0).getString("peer");
            String groupId = args.getJSONObject(0).getString("groupId");
            int activity = Integer.parseInt(args.getJSONObject(0).getString("activity"));
            int roomId = Integer.parseInt(args.getJSONObject(0).getString("roomId"));
            String sender = args.getJSONObject(0).getString("sender");
            this.sendActivity(peer, Long.parseLong(groupId), sender, activity, roomId, callbackContext);
            return true;
        }

        // Obtiene la lista de perfiles
        if (action.equals("getProfiles")) {
            this.getProfiles(callbackContext);
            return true;
        }

        // Obtiene un perfil por su id
        if (action.equals("getProfile")) {
            String peer = args.getJSONObject(0).getString("peer");
            long groupId = Long.parseLong(args.getJSONObject(0).getString("groupId"));
            boolean isGroup = Boolean.parseBoolean(args.getJSONObject(0).getString("isGroup"));
            this.getProfile(peer, groupId, isGroup, callbackContext);
            return true;
        }

        // Realiza una llamada (de voz o video) a un "peer"
        if (action.equals("call")) {
            String peer = args.getJSONObject(0).getString("peer");
            boolean isVideoCall = Boolean.parseBoolean(args.getJSONObject(0).getString("isVideoCall"));
            long groupId = Long.parseLong(args.getJSONObject(0).getString("groupId"));
            this.call(peer, groupId, isVideoCall, callbackContext);
            return true;
        }

        // Suscribe a un evento que escucha cuando llegan nuevos mensajes
        if (action.equals("onMessage")) {
            MesiboCordova.callbackOnMessage = callbackContext;
            return true;
        }

        // Suscribe a un evento que escucha cuando un mensaje cambia de estado
        if (action.equals("onMessageStatus")) {
            MesiboCordova.callbackOnMessageStatus = callbackContext;
            return true;
        }

        // Suscribe a un evento que escucha nuevas actividades
        if (action.equals("onActivity")) {
            MesiboCordova.callbackOnActivity = callbackContext;
            return true;
        }

        // Suscribe a un evento que escucha cuando llega un mensaje con ubicación (NO IMPLEMENTADO)
        if (action.equals("onLocation")) {
            return true;
        }

        // Suscribe a un evento que escucha cuando llega un mensaje con archivo (NO IMPLEMENTADO)
        if (action.equals("onFile")) {
            return true;
        }

        // Suscribe a un evento que escucha cuando cambia un perfil de usuario
        if (action.equals("onUserProfile")) {
            MesiboCordova.callbackOnUserProfile = callbackContext;
            return true;
        }
        return false;
    }

    // Asigna el token para e inicia la sesión del usuario actual en mesibo
    private void setAccessToken(String token, CallbackContext callbackContext) throws JSONException {
        JSONObject result = new JSONObject();
        try {
            int tokenStatus = Mesibo.setAccessToken(token);
            boolean databaseSuccess = Mesibo.setDatabase("mesibochat.db", 0);
            int startStatus = Mesibo.start();

            result.put("tokenStatus", tokenStatus);
            result.put("startStatus", startStatus);
            result.put("databaseSuccess", databaseSuccess);
            callbackContext.success(result);
        } catch (Exception ex) {
            result.put("message", ex.toString());
            callbackContext.error(result);
        }
    }

    // Elimina los mensajes de una conversación
    private void deleteRoomMessages(String peer, long groupId, CallbackContext callbackContext) throws JSONException {
        JSONObject result = new JSONObject();
        try {
            int count = Mesibo.deleteMessages(peer, groupId, new Date().getTime());
            result.put("count", count);
            callbackContext.success(result);
        } catch (Exception ex) {
            result.put("message", ex.toString());
            callbackContext.error(result);
        }
    }

    // Detiene todas las conexiones con Mesibo
    private void stopMesibo(CallbackContext callbackContext) throws JSONException {
        JSONObject result = new JSONObject();
        try {
            int status = Mesibo.stop(true);

            result.put("status", status);
            callbackContext.success(result);
        } catch (Exception ex) {
            result.put("message", ex.toString());
            callbackContext.error(result);
        }
    }

    // Obtiene una lista de mensajes de una sala en particular
    private boolean readRoomMessages(String address, long groupId, int messageCount, boolean enableFifo, boolean enableReadReceipt, CallbackContext callbackContext) throws JSONException {
        JSONObject result = new JSONObject();
        try {
            if (this.readDbSession == null) {
                Mesibo.UserProfile profile = MesiboCordova.getProfile(address, groupId);

                if (profile.address == null && profile.groupid < 1) {
                    result.put("message", "Debe proveer un groupid o peer.");
                    callbackContext.error(result);
                    return false;
                }

                this.readDbSession = new Mesibo.ReadDbSession(profile.address, profile.groupid, null, this);
                this.readDbSession.enableReadReceipt(enableReadReceipt);
                this.readDbSession.enableFifo(enableFifo);
            }
            // Los mensajes leídos, serán enviados a cordova a través del método "Mesibo_onMessage"
            int status = this.readDbSession.read(messageCount);

            result.put("success", true);
            result.put("status", status);
            callbackContext.success(result);
            return true;
        } catch (Exception ex) {
            result.put("message", ex.toString());
            callbackContext.error(result);
            return false;
        }
    }

    // Elimina la sesión de lectura actual
    private boolean stopReadDbSession(CallbackContext callbackContext) throws JSONException {
        JSONObject result = new JSONObject();
        try {
            if (this.readDbSession != null) {
                this.readDbSession.stop();
                this.readDbSession = null;
            }
            result.put("stoped", true);
            callbackContext.success(result);
            return true;
        } catch (JSONException ex) {
            this.readDbSession = null;
            result.put("stopped", false);
            result.put("message", ex.toString());
            callbackContext.error(result);
            return false;
        }
    }

    // Obtiene una lista de mensajes de un perfil en particular
    // La diferencia con readRoomMessages es que la sesión de lectura dura solamente la ejecución del método
    // Generalmente se usa para leer el último mensaje de cada conversación
    private boolean readProfileMessages(String address, long groupId, int messageCount, boolean enableFifo, boolean enableReadReceipt, CallbackContext callbackContext) throws JSONException {
        JSONObject result = new JSONObject();
        try {
            Mesibo.UserProfile profile = MesiboCordova.getProfile(address, groupId);

            if (profile.address == null && profile.groupid < 1) {
                result.put("message", "Debe proveer un groupid o peer.");
                callbackContext.error(result);
                return false;
            }

            Mesibo.ReadDbSession readDbSession = new Mesibo.ReadDbSession(profile.address, profile.groupid, null, this);
            readDbSession.enableReadReceipt(enableReadReceipt);
            readDbSession.enableFifo(enableFifo);

            // Los mensajes leídos, serán enviados a cordova a través del método "Mesibo_onMessage"
            int status = readDbSession.read(messageCount);

            result.put("status", status);
            callbackContext.success(result);
            return true;
        } catch (Exception ex) {
            result.put("message", ex.toString());
            callbackContext.error(result);
            return false;
        }
    }

    private boolean sendActivity(String peer, long groupId, String sender, int activity, int roomId, CallbackContext callbackContext) throws JSONException {
        JSONObject result = new JSONObject();
        try {
            // Creamos la instancia de mensaje
            Mesibo.MessageParams messageParams = new Mesibo.MessageParams();
            messageParams.ts = new Date().getTime();

            // Actividad 3 -> Usuario eliminado de un grupo
            // Por lo cual debemos setear el peer del usuario solamente
            if (activity == 3) {
                if (!(peer == null || peer.equals(""))) {
                    messageParams.setPeer(peer);
                }
                // Si no tenemos peer, retornamos el error y paramos la ejecución del método
                else {
                    result.put("message", "Debe proveer un peer.");
                    callbackContext.error(result);
                    return false;
                }
            } else {
                // Si groupId es mayor a 0, estamos enviando la actividad a un grupo
                if (groupId > 0) {
                    messageParams.setGroup(groupId);
                }
                // Si no es grupo, verificamos que sea chat 1 a 1
                else if (!(peer == null || peer.equals(""))) {
                    messageParams.setPeer(peer);
                }
                // Si no tenemos ninguno de los 2, retornamos el error y paramos la ejecución del método
                else {
                    result.put("message", "Debe proveer un peer o groupId.");
                    callbackContext.error(result);
                    return false;
                }
            }

            Mesibo.UserProfile selfProfile = Mesibo.getSelfProfile();

            if (selfProfile == null) {
                selfProfile = new Mesibo.UserProfile();
                selfProfile.address = sender;
            }

            Profile profile = new Profile();
            profile.setAddress(selfProfile.address);
            profile.setGroupId(selfProfile.groupid);
            profile.setLastSeen(selfProfile.lastActiveTime);
            profile.setStatus(selfProfile.status);
            profile.setUnread(selfProfile.unread);

            long messageId = Mesibo.random();
            int send = Mesibo.sendActivity(messageParams, roomId, messageId, activity, 0);

            Activity sentActivity = null;

            if (send == 0) {
                sentActivity = new Activity();
                sentActivity.setActivity(activity);
                sentActivity.setGroupId(groupId);
                sentActivity.setId(messageId);
                sentActivity.setPeer(peer);
                sentActivity.setTime(messageParams.ts);
                sentActivity.setProfile(profile);
            }

            if (sentActivity == null) {
                result.put("message", "Actividad no enviada.");
                callbackContext.error(result);
                return false;
            }

            callbackContext.success(sentActivity.toJson());
            return true;
        } catch (Exception ex) {
            result.put("message", ex.toString());
            callbackContext.error(result);
            return false;
        }
    }

    // Envía un mensaje a un grupo o usuario
    private boolean sendMessage(String peer, long groupId, int type, String message, String sender, CallbackContext callbackContext) throws JSONException {
        JSONObject result = new JSONObject();
        try {
            Mesibo.MessageParams messageParams = new Mesibo.MessageParams();
            messageParams.enableReadReceipt(true);
            messageParams.ts = new Date().getTime();
            messageParams.setType(type);

            if (groupId > 0) {
                messageParams.setGroup(groupId);
            } else if (!(peer == null || peer.equals(""))) {
                messageParams.setPeer(peer);
            } else {
                result.put("message", "Debe proveer un groupid o peer.");
                callbackContext.error(result);
                return false;
            }

            Mesibo.UserProfile selfProfile = Mesibo.getSelfProfile();

            if (selfProfile == null) {
                selfProfile = new Mesibo.UserProfile();
                selfProfile.address = sender;
            }

            Profile profile = new Profile();
            profile.setAddress(selfProfile.address);
            profile.setGroupId(selfProfile.groupid);
            profile.setLastSeen(selfProfile.lastActiveTime);
            profile.setStatus(selfProfile.status);
            profile.setUnread(selfProfile.unread);

            long messageId = Mesibo.random();
            int sent = Mesibo.sendMessage(messageParams, messageId, message);

            Message sentMessage = null;
            File file = new File();

            try {
                JSONObject json = new JSONObject(message);

                if (sent == 0) {
                    sentMessage = new Message();

                    if (type == 2) {
                        JSONObject jsonFile = new JSONObject(json.getString("file"));
                        file.setUrl(jsonFile.getString("url"));
                        file.setType(jsonFile.getString("type"));
                        file.setName(jsonFile.getString("name"));
                        file.setSize(jsonFile.getString("size"));
                    }

                    Message reference = new Message();

                    sentMessage.setContent(json.getString("content"));
                    sentMessage.setFile(file);
                    sentMessage.setGroupId(groupId);
                    sentMessage.setId(messageId);
                    sentMessage.setIncoming(false);
                    sentMessage.setPeer(peer);
                    sentMessage.setSender(sender);
                    sentMessage.setResent(Boolean.parseBoolean(json.getString("resent")));
                    sentMessage.setReference(reference);
                    sentMessage.setTime(messageParams.ts < 1 ? new Date().getTime() : messageParams.ts);
                    sentMessage.setStatus(Status.findStatus(sent));
                    sentMessage.setType(type);
                }

                if (sentMessage == null) {
                    result.put("message", "Mensaje no enviado.");
                    result.put("status", sent);
                    callbackContext.error(result);
                    return false;
                }

                callbackContext.success(sentMessage.toJson());
                return true;

            } catch (JSONException ex) {
                result.put("message", ex.toString());
                callbackContext.error(result);
                return false;
            }
        } catch (Exception ex) {
            result.put("message", ex.toString());
            callbackContext.error(result);
            return false;
        }
    }

    // Realiza una llamada (normal o de video)
    private void call(String peer, long groupId, boolean isVideoCall, CallbackContext callbackContext) {
        try {
            Handler refresh = new Handler(context.getMainLooper());
            refresh.post(new Runnable() {
                @Override
                public void run() {

                    Mesibo.UserProfile profile = null;

                    if (groupId == 0) {
                        profile = Mesibo.getUserProfile(peer);
                    } else {
                        profile = Mesibo.getUserProfile(groupId);
                    }

                    if (profile != null) {
                        // Iniciamos la llamada
                        MesiboCall.getInstance().call(context, Mesibo.random(), profile, isVideoCall);

                        callbackContext.success("CALLING_OK " + groupId);
                        return;
                    }
                    callbackContext.error("CALLING_ERROR " + profile);
                }
            });
        } catch (Exception ex) {
            callbackContext.error("CALLING_ERROR " + ex);
        }
    }

    // Obtiene los perfiles de usuarios y los retorna en un arreglo del tipo JSON
    private void getProfiles(CallbackContext callbackContext) {
        try {
            HashMap<String, Mesibo.UserProfile> userProfiles;
            userProfiles = Mesibo.getUserProfiles();

            JSONArray result = new JSONArray();

            // Recorremos los perfiles de usuarios
            for (Map.Entry<String, Mesibo.UserProfile> userProfileEntry : userProfiles.entrySet()) {
                Mesibo.UserProfile mesiboUserProfile = userProfileEntry.getValue();
                Profile profile = new Profile();

                profile.setAddress(mesiboUserProfile.address);
                profile.setUnread(mesiboUserProfile.unread);
                profile.setStatus(mesiboUserProfile.status);
                profile.setLastSeen(mesiboUserProfile.lastActiveTime);
                profile.setGroupId(mesiboUserProfile.groupid);

                if (userProfileEntry.getKey() != null && mesiboUserProfile != null) {
                    result.put(profile.toJson());
                }
            }

            callbackContext.success(result);
        } catch (Exception ex) {
            callbackContext.error("Error al obtener los perfiles: " + ex);
        }
    }

    private static Mesibo.UserProfile getProfile(String address, long groupId) {
        Mesibo.UserProfile profile = new Mesibo.UserProfile();

        if (groupId > 0) {
            profile.groupid = groupId;
        } else if (address != null && address != "") {
            profile.address = address;
        }
        return profile;
    }

    // Obtiene y retorna un perfil de grupo o usuario
    private void getProfile(String peer, long groupid, boolean is_group, CallbackContext callbackContext) {
        try {
            // Creamos un objeto tipo JSON
            JSONObject result = new JSONObject();

            // Obtenemos el perfil
            Mesibo.UserProfile tempProfile = (!is_group ? Mesibo.getUserProfile(peer) : Mesibo.getUserProfile(groupid));

            if (tempProfile == null) {
                tempProfile = Mesibo.createUserProfile(peer, groupid, peer);
            }

            Profile profile = new Profile();

            profile.setAddress(tempProfile.address);
            profile.setUnread(tempProfile.unread);
            profile.setStatus(tempProfile.status);
            profile.setLastSeen(tempProfile.lastActiveTime);
            profile.setGroupId(tempProfile.groupid);

            result = profile.toJson();

            // Enviamos un resultado a cordova
            callbackContext.success(result);
        } catch (Exception ex) {
            callbackContext.error("Error al obtener el perfil: " + ex);
        }
    }

    /* EVENTOS MESIBO (LISTENERS) */

    // Escucha los mensajes que le llegan a este usuario y los envía a javascript
    @Override
    public boolean Mesibo_onMessage(Mesibo.MessageParams params, byte[] bytes) {
        JSONObject result = new JSONObject();
        try {

            File file = new File();

            JSONObject json = new JSONObject(new String(bytes));

            Message message = new Message();

            if (params.type == 2) {
                JSONObject jsonFile = new JSONObject(json.getString("file"));
                file.setUrl(jsonFile.getString("url"));
                file.setType(jsonFile.getString("type"));
                file.setName(jsonFile.getString("name"));
                file.setSize(jsonFile.getString("size"));
            }

            Message reference = new Message();

            message.setContent(json.getString("content"));
            message.setFile(file);
            message.setGroupId(params.groupid);
            message.setId(params.mid);
            message.setIncoming(params.isIncoming());
            message.setPeer(params.peer);
            message.setSender(json.getString("sender"));
            message.setResent(Boolean.parseBoolean(json.getString("resent")));
            message.setReference(reference);
            message.setTime(params.ts);
            message.setStatus(Status.findStatus(params.getStatus()));
            message.setType(params.type);

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, message.toJson());
            pluginResult.setKeepCallback(true);

            if (MesiboCordova.callbackOnMessage != null) {
                MesiboCordova.callbackOnMessage.sendPluginResult(pluginResult);
                return true;
            }
            return false;

        } catch (JSONException ex) {
            try {
                result.put("message", ex.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            MesiboCordova.callbackOnMessage.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, result));
            return false;
        }
    }

    // Escucha los cambios de estados en los mensajes y los envía a javascript
    @Override
    public void Mesibo_onMessageStatus(Mesibo.MessageParams params) {
        try {
            Message message = new Message();

            message.setId(params.mid);
            message.setPeer(params.peer);
            message.setGroupId(params.groupid);
            message.setStatus(Status.findStatus(params.getStatus()));
            message.setTime(params.ts);

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, message.toJson());
            pluginResult.setKeepCallback(true);

            // Si el callback de estados de mensajes existe
            if (MesiboCordova.callbackOnMessageStatus != null) {
                MesiboCordova.callbackOnMessageStatus.sendPluginResult(pluginResult);
            }
        } catch (Exception ex) {
            System.out.println("Mesibo_onMessageStatus Callback context null");
        }
    }

    // Escucha los actividades y las envía a javascript
    @Override
    public void Mesibo_onActivity(Mesibo.MessageParams params, int mActivity) {
        try {
            Profile profile = new Profile();
            profile.setAddress(params.profile.address);
            profile.setUnread(params.profile.unread);
            profile.setStatus(params.profile.status);
            profile.setLastSeen(params.profile.lastActiveTime);
            profile.setGroupId(params.profile.groupid);

            Activity activity = new Activity();

            activity.setProfile(profile);
            activity.setTime(params.ts);
            activity.setPeer(params.peer);
            activity.setId(params.mid);
            activity.setGroupId(params.groupid);
            activity.setActivity(mActivity);

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, activity.toJson());
            pluginResult.setKeepCallback(true);

            // Si el callback de activity existe
            if (MesiboCordova.callbackOnActivity != null) {
                MesiboCordova.callbackOnActivity.sendPluginResult(pluginResult);
            }
        } catch (Exception ex) {
            System.out.println("Mesibo_onActivity Callback context null");
        }
    }

    @Override
    public void Mesibo_onLocation(Mesibo.MessageParams messageParams, Mesibo.Location location) {

    }

    @Override
    public void Mesibo_onFile(Mesibo.MessageParams messageParams, Mesibo.FileInfo fileInfo) {

    }


    @Override
    public boolean Mesibo_onUpdateUserProfiles(Mesibo.UserProfile mProfile) {
        Log.i("Mesibo_onUpdateUserProf", mProfile == null ? "profile.address" : mProfile.address);

        if (mProfile != null) {
            Profile profile = new Profile();

            profile.setAddress(mProfile.address);
            profile.setUnread(mProfile.unread);
            profile.setStatus(mProfile.status);
            profile.setLastSeen(mProfile.lastActiveTime);
            profile.setGroupId(mProfile.groupid);

            // Creamos un resultado para cordova
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, profile.toJson());

            if (MesiboCordova.callbackOnUserProfile != null) {
                MesiboCordova.callbackOnUserProfile.sendPluginResult(pluginResult);
            }
            return true;
        }
        return false;
    }
}
