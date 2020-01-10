    import mesibo;
    // import mesibocall;
    
    /*
     * Nota: '@objc' indica que tanto la clase como los métodos deben ser expuestos a Cordova.
     */
    @objc(MesiboCordova) class MesiboCordova : CDVPlugin, MesiboDelegate /*, MesiboCallDelegate */ {
        
        // Callbacks para los eventos de mesibo
        private static var callbackOnMessage: CDVInvokedUrlCommand? = nil;
        private static var callbackOnMessageStatus: CDVInvokedUrlCommand? = nil;
        private static var callbackOnActivity: CDVInvokedUrlCommand? = nil;
        private static var callbackOnUserProfile: CDVInvokedUrlCommand? = nil;
        
        private static var readDbSession: MesiboReadSession? = nil;
        
        
        override func pluginInitialize() {
            /* MesiboCall.sharedInstance().setListener(self)
             MesiboCall.sharedInstance().showInProgress() */
        }
        
        // Escucha los mensajes entrantes
        @objc(onMessage:)
        func onMessage(command: CDVInvokedUrlCommand) {
            if(MesiboCordova.callbackOnMessage == nil) {
                MesiboCordova.callbackOnMessage = command;
            }
        }
        
        // Escucha las actividades entrantes
        @objc(onActivity:)
        func onActivity(command: CDVInvokedUrlCommand) {
            if(MesiboCordova.callbackOnActivity == nil) {
                MesiboCordova.callbackOnActivity = command;
            }
        }
        
        // Escucha los cambios de estados de los mensajes
        @objc(onMessageStatus:)
        func onMessageStatus(command: CDVInvokedUrlCommand) {
            if(MesiboCordova.callbackOnMessageStatus == nil) {
                MesiboCordova.callbackOnMessageStatus = command;
            }
        }
        
        // Asigna el token para e inicia la sesión del usuario actual en mesibo
        @objc(setAccessToken:)
        func setAccessToken(command: CDVInvokedUrlCommand) {
            Mesibo.getInstance()?.addListener(self)
            var result = ["message": "Error al setear el token."] as [AnyHashable : Any]
            
            // Obtenemos los parámetros del JSON
            let params = command.arguments[0] as? [String:Any]
            // Buscamos el atributo 'token'
            let token = params?["token"] as! String
            
            if(token != "") {
                let tokenStatus = Mesibo.getInstance().setAccessToken(token);
                let databaseSuccess = Mesibo.getInstance().setDatabase("mesibochat.db", resetTables: 0);
                Mesibo.getInstance().setSecureConnection(true);
                let startStatus = Mesibo.getInstance().start()
                /* MesiboCall.sharedInstance().start() */
                
                // Editamos la respuesta para cordova
                result = ["databaseSuccess": databaseSuccess, "startStatus": startStatus, "tokenStatus": tokenStatus] as [AnyHashable : Any]
            } else {
                result = ["message": "No se envió ningún token."] as [AnyHashable : Any]
            }
            // Enviamos la respuesta a cordova
            let pluginResult = CDVPluginResult (status: CDVCommandStatus_OK, messageAs: result);
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
        }
        
        // Elimina todos los mensajes de una conversación
        @objc(deleteRoomMessages:)
        func deleteRoomMessages(command: CDVInvokedUrlCommand) {
            let params = command.arguments[0] as? [String:Any]
            let peer = params?["peer"] as! String
            let groupId = params?["groupId"] as! UInt32
            let deleted = Mesibo.getInstance()?.deleteMessages(peer, groupid: groupId, ts: Mesibo.getInstance().getTimestamp())
            let result = ["count": (deleted == true ? 0 : -1) ] as [AnyHashable : Any]
            let pluginResult = CDVPluginResult (status: CDVCommandStatus_OK, messageAs: result);
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
        }
        
        // Detiene todas las conexiones con Mesibo
        @objc(stopMesibo:)
        func stopMesibo(command: CDVInvokedUrlCommand) {
            
            let status = Mesibo.getInstance().stop();
            
            MesiboCordova.callbackOnMessage = nil;
            MesiboCordova.callbackOnMessageStatus = nil;
            MesiboCordova.callbackOnActivity = nil;
            MesiboCordova.callbackOnUserProfile = nil;
            
            let result = ["status": status] as [AnyHashable : Any]
            let pluginResult = CDVPluginResult (status: CDVCommandStatus_OK, messageAs: result);
            // Enviamos la respuesta a cordova
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
        }
        
        // Obtiene una lista de mensajes de un "profile" en particular
        @objc(readRoomMessages:)
        func readRoomMessages(command: CDVInvokedUrlCommand) {
            
            var result = ["message": "Debe proveer un groupid o address."] as [AnyHashable : Any]
            
            // Obtenemos los parámetros del JSON
            let params = command.arguments[0] as? [String:Any]
            let messageCount = params?["messageCount"] as! Int32
            
            if(MesiboCordova.readDbSession == nil) {
                let address = params?["address"] as! String
                let groupid = params?["groupId"] as! UInt32
                let enableFifo = params?["enableFifo"] as! Bool
                let enableReadReceipt = params?["enableReadReceipt"] as! Bool
                
                MesiboCordova.readDbSession = MesiboReadSession()
                MesiboCordova.readDbSession?.enableFifo(enableFifo)
                
                if(enableReadReceipt == true) {
                    MesiboCordova.readDbSession?.enableReadReceipt(true)
                } else {
                    MesiboCordova.readDbSession?.disableReadReceipt(true)
                }
                
                // Si groupid es mayor a 0, estamos leyendo mensajes de un grupo
                if(groupid > 0) {
                    MesiboCordova.readDbSession?.initSession(nil, groupid: groupid, query: nil, delegate: self)
                }
                    // Si no es grupo, verificamos que sea chat 1 a 1
                else if(address != "") {
                    MesiboCordova.readDbSession?.initSession(address, groupid: 0, query: nil, delegate: self)
                }
                    // Si no tenemos ninguno de los 2, retornamos el error y paramos la ejecución del método
                else {
                    // Enviamos la respuesta a cordova
                    let pluginResult = CDVPluginResult (status: CDVCommandStatus_ERROR, messageAs: result);
                    self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
                    return;
                }
                
                MesiboCordova.readDbSession?.enableReadReceipt(true)
                let status = MesiboCordova.readDbSession?.read(messageCount)
                
                result = ["success": true, "status": status as Any] as [AnyHashable : Any]
                let pluginResult = CDVPluginResult (status: CDVCommandStatus_OK, messageAs: result);
                self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
            } else {
                let status = MesiboCordova.readDbSession?.read(messageCount)
                result = ["success": true, "status": status as Any] as [AnyHashable : Any]
                let pluginResult = CDVPluginResult (status: CDVCommandStatus_OK, messageAs: result);
                self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
            }
        }
        
        // Elimina la sesión de lectura actual
        @objc(stopReadDbSession:)
        func stopReadDbSession(command: CDVInvokedUrlCommand) {
            
            var result = ["message": "No ha sido posible detener la sesión de lectura."] as [AnyHashable : Any]
            
            if(MesiboCordova.readDbSession != nil) {
                MesiboCordova.readDbSession?.stop();
                MesiboCordova.readDbSession = nil;
            }
            
            result = ["stopped": true] as [AnyHashable : Any]
            let pluginResult = CDVPluginResult (status: CDVCommandStatus_OK, messageAs: result);
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
        }
        
        // Obtiene una lista de mensajes de un perfil en particular
        // La diferencia con readRoomMessages es que la sesión de lectura dura solamente la ejecución del método
        // Generalmente se usa para leer el último mensaje de cada conversación
        @objc(readProfileMessages:)
        func readProfileMessages(command: CDVInvokedUrlCommand) {
            
            // Declaramos un resultado para Cordova (Asumimos que fallará)
            var result = ["message": "Debe proveer un groupid o address."] as [AnyHashable : Any]
            
            let params = command.arguments[0] as? [String:Any]
            let address = params?["address"] as! String
            let groupid = params?["groupId"] as! UInt32
            let messageCount = params?["messageCount"] as! Int32
            let enableFifo = params?["enableFifo"] as! Bool
            let enableReadReceipt = params?["enableReadReceipt"] as! Bool
            
            let readDbSession = MesiboReadSession()
            readDbSession.enableFifo(enableFifo)
            
            if(enableReadReceipt == true) {
                readDbSession.enableReadReceipt(true)
            } else {
                readDbSession.disableReadReceipt(true)
            }
            
            // Si groupid es mayor a 0, estamos leyendo mensajes de un grupo
            if(groupid > 0) {
                readDbSession.initSession(nil, groupid: groupid, query: nil, delegate: self)
            }
                // Si no es grupo, verificamos que sea chat 1 a 1
            else if(address != "") {
                readDbSession.initSession(address, groupid: 0, query: nil, delegate: self)
            }
                // Si no tenemos ninguno de los 2, retornamos el error y paramos la ejecución del método
            else {
                let pluginResult = CDVPluginResult (status: CDVCommandStatus_ERROR, messageAs: result);
                self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
                return;
            }
            
            let status = readDbSession.read(messageCount)
            
            result = ["status": status] as [AnyHashable : Any]
            let pluginResult = CDVPluginResult (status: CDVCommandStatus_ERROR, messageAs: result);
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
        }
        
        // Envía una actividad a un grupo o usuario
        @objc(sendActivity:)
        func sendActivity(command: CDVInvokedUrlCommand) {
            
            // Declaramos un resultado para Cordova (Asumimos que fallará)
            var result = ["message": "Debe proveer un groupId o peer."] as [AnyHashable : Any]
            
            // Obtenemos los parámetros del JSON
            let params = command.arguments[0] as? [String:Any]
            
            let peer = params?["peer"] as! String
            let groupId = params?["groupId"] as! UInt32
            let activity = params?["activity"] as! Int32
            let roomId = params?["roomId"] as! UInt32
            let sender = params?["sender"] as! String
            
            let messageParams = MesiboParams()
            messageParams.ts = Mesibo.getInstance().getTimestamp()
            
            // Actividad 3 -> Usuario eliminado de un grupo
            // Por lo cual debemos setear el peer del usuario solamente
            if (activity == 3) {
                if (peer != "") {
                    messageParams.peer = peer;
                } else {
                    result = ["message": "Debe proveer un peer."] as [AnyHashable : Any]
                    let pluginResult = CDVPluginResult (status: CDVCommandStatus_ERROR, messageAs: result);
                    self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
                    return;
                }
            } else {
                // Si groupId es mayor a 0, estamos enviando un mensaje a un grupo
                if(groupId > 0) {
                    messageParams.setGroup(groupId);
                }
                    // Si no es grupo, verificamos que sea chat 1 a 1
                else if(peer != "") {
                    messageParams.peer = peer;
                }
                    // Si no tenemos ninguno de los 2, retornamos el error y paramos la ejecución del método
                else {
                    // Enviamos la respuesta a cordova
                    let pluginResult = CDVPluginResult (status: CDVCommandStatus_ERROR, messageAs: result);
                    self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
                    return;
                }
            }
            
            var selfProfile = Mesibo.getInstance().getSelfProfile()
            
            if(selfProfile == nil) {
                selfProfile = MesiboUserProfile()
                selfProfile?.address = sender
            }
            
            let mesiboUserProfile = MesiboUserProfile()
            mesiboUserProfile.name = selfProfile?.name;
            mesiboUserProfile.address = selfProfile?.address;
            mesiboUserProfile.groupid = selfProfile!.groupid;
            mesiboUserProfile.status = selfProfile?.status;
            mesiboUserProfile.picturePath = selfProfile?.picturePath;
            mesiboUserProfile.unread = selfProfile!.unread;
            mesiboUserProfile.draft = selfProfile?.draft;
            mesiboUserProfile.flag = roomId;
            mesiboUserProfile.lastActiveTime = selfProfile!.lastActiveTime;
            
            let messageId = Mesibo.getInstance().random();
            
            let send = Mesibo.getInstance()?.sendActivity(messageParams, msgid: messageId, activity: activity, interval: 0)
            
            var pluginResult = CDVPluginResult (status: CDVCommandStatus_OK, messageAs: result);
            
            if(send == 0) {
                result = [
                    "id": messageId,
                    "time": Mesibo.getInstance().getTimestamp(),
                    "groupId": groupId,
                    "activity": activity,
                    "peer": peer,
                    "profile": [
                        "groupId": mesiboUserProfile.groupid,
                        "unread": mesiboUserProfile.unread,
                        "lastSeen": mesiboUserProfile.lastActiveTime,
                        "address": mesiboUserProfile.address,
                        "status": mesiboUserProfile.status
                        ] as [AnyHashable: Any]
                    ] as [AnyHashable : Any]
            } else {
                result = ["message": "Actividad no enviada."] as [AnyHashable : Any]
                pluginResult = CDVPluginResult (status: CDVCommandStatus_ERROR, messageAs: result);
            }
            
            // Enviamos la respuesta a cordova
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
        }
        
        // Envía un mensaje a un grupo o usuario
        @objc(sendMessage:)
        func sendMessage(command: CDVInvokedUrlCommand) {
            
            // Declaramos un resultado para Cordova (Asumimos que fallará)
            var result = ["message": "Debe proveer un groupId o peer."] as [AnyHashable : Any]
            
            // Obtenemos los parámetros del JSON
            let params = command.arguments[0] as? [String:Any]
            
            // Buscamos el atributo 'address' y 'messageCount'
            let peer = params?["peer"] as! String
            let groupId = params?["groupId"] as! UInt32
            let message = params?["message"] as! String
            let sender = params?["sender"] as! String
            let type = params?["type"] as! Int32
            
            let messageParams = MesiboParams()
            
            messageParams.type = type
            
            // Si groupId es mayor a 0, estamos enviando un mensaje a un grupo
            if(groupId > 0) {
                messageParams.setGroup(groupId);
            }
                // Si no es grupo, verificamos que sea chat 1 a 1
            else if(peer != "") {
                messageParams.peer = peer;
            }
                // Si no tenemos ninguno de los 2, retornamos el error y paramos la ejecución del método
            else {
                let pluginResult = CDVPluginResult (status: CDVCommandStatus_ERROR, messageAs: result);
                self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
                return;
            }
            
            var selfProfile = Mesibo.getInstance().getSelfProfile()
            
            if(selfProfile == nil) {
                selfProfile = MesiboUserProfile()
                selfProfile?.address = sender
            }
            
            let mesiboUserProfile = MesiboUserProfile()
            mesiboUserProfile.name = selfProfile?.name;
            mesiboUserProfile.address = selfProfile?.address;
            mesiboUserProfile.groupid = selfProfile!.groupid;
            mesiboUserProfile.status = selfProfile?.status;
            mesiboUserProfile.picturePath = selfProfile?.picturePath;
            mesiboUserProfile.unread = selfProfile!.unread;
            mesiboUserProfile.draft = selfProfile?.draft;
            mesiboUserProfile.flag = selfProfile!.flag;
            mesiboUserProfile.lastActiveTime = selfProfile!.lastActiveTime;
            
            let messageId = Mesibo.getInstance().random();
            
            do {
                let data = message.data(using: .utf8)!
                let json = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any]
                print(json as Any)
                let sent = Mesibo.getInstance()?.sendMessage(messageParams, msgid: messageId, string: message)
                var file: Any = ""
                
                if(sent == 0) {
                    if(type == 2) {
                        do {
                            let fileData = json!["file"] as! NSDictionary
                            
                            file = [
                                "url": fileData["url"] as Any,
                                "size": fileData["size"] as Any,
                                "type": fileData["type"] as Any,
                                "name": fileData["name"] as Any
                                ] as [AnyHashable : Any]
                        } catch let parsingError {
                            print("Error", parsingError)
                            result = ["message": parsingError] as [AnyHashable : Any]
                            let pluginResult = CDVPluginResult (status: CDVCommandStatus_ERROR, messageAs: result);
                            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
                        }
                    }
                    
                    let status: Status = self.findStatusByValue(value: sent!);
                    
                    result = [
                        "id": messageId,
                        "content": json!["content"] as! String,
                        "type": type,
                        "reference": "",
                        "resent": json!["resent"] as! Bool,
                        "status": [
                            "value": status.value as Any,
                            "label": status.label as Any,
                            "code": status.code as Any,
                            "icon": status.icon,
                            "cssClass": status.cssClass as Any
                            ] as [AnyHashable : Any],
                        "isIncoming": false,
                        "file": file,
                        "time": Mesibo.getInstance().getTimestamp(),
                        "peer": peer,
                        "sender": sender,
                        "groupId": groupId
                        ] as [AnyHashable : Any]
                    
                    let pluginResult = CDVPluginResult (status: CDVCommandStatus_OK, messageAs: result);
                    self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
                }
            } catch let parsingError {
                print("Error", parsingError)
                result = ["message": parsingError] as [AnyHashable : Any]
                let pluginResult = CDVPluginResult (status: CDVCommandStatus_ERROR, messageAs: result);
                self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
            }
        }

        @objc(saveCustomMessage:)
        func saveCustomMessage(command: CDVInvokedUrlCommand) {
            
            // Declaramos un resultado para Cordova (Asumimos que fallará)
            var result = ["message": "Debe proveer un groupId o peer."] as [AnyHashable : Any]
            
            // Obtenemos los parámetros del JSON
            let params = command.arguments[0] as? [String:Any]
            
            var id = 0

            do {
                id = try params?["id"] as! UInt32
            } catch let error { id = 0 }
            
            let peer = params?["peer"] as! String
            let groupId = params?["groupId"] as! UInt32
            let message = params?["message"] as! String
            let sender = params?["sender"] as! String
            let type = params?["type"] as! Int32
            
            let messageParams = MesiboParams()
            
            messageParams.type = type
            
            // Si groupId es mayor a 0, estamos enviando un mensaje a un grupo
            if(groupId > 0) {
                messageParams.setGroup(groupId);
            }
                // Si no es grupo, verificamos que sea chat 1 a 1
            else if(peer != "") {
                messageParams.peer = peer;
            }
                // Si no tenemos ninguno de los 2, retornamos el error y paramos la ejecución del método
            else {
                let pluginResult = CDVPluginResult (status: CDVCommandStatus_ERROR, messageAs: result);
                self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
                return;
            }
            
            var selfProfile = Mesibo.getInstance().getSelfProfile()
            
            if(selfProfile == nil) {
                selfProfile = MesiboUserProfile()
                selfProfile?.address = sender
            }
            
            let mesiboUserProfile = MesiboUserProfile()
            mesiboUserProfile.name = selfProfile?.name;
            mesiboUserProfile.address = selfProfile?.address;
            mesiboUserProfile.groupid = selfProfile!.groupid;
            mesiboUserProfile.status = selfProfile?.status;
            mesiboUserProfile.picturePath = selfProfile?.picturePath;
            mesiboUserProfile.unread = selfProfile!.unread;
            mesiboUserProfile.draft = selfProfile?.draft;
            mesiboUserProfile.flag = selfProfile!.flag;
            mesiboUserProfile.lastActiveTime = selfProfile!.lastActiveTime;
            
            let messageId = (id > 0 ? id : Mesibo.getInstance().random());
            
            do {
                let data = message.data(using: .utf8)!
                let json = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any]
                let saved = Mesibo.getInstance()?.saveCustomMessage(messageParams, msgid: messageId, string: message)
                var file: Any = ""
                
                if(saved == 0) {
                    if(type == 2) {
                        do {
                            let fileData = json!["file"] as! NSDictionary
                            
                            file = [
                                "url": fileData["url"] as Any,
                                "size": fileData["size"] as Any,
                                "type": fileData["type"] as Any,
                                "name": fileData["name"] as Any
                            ] as [AnyHashable : Any]
                        } catch let parsingError {
                            print("Error", parsingError)
                            result = ["message": parsingError] as [AnyHashable : Any]
                            let pluginResult = CDVPluginResult (status: CDVCommandStatus_ERROR, messageAs: result);
                            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
                        }
                    }
                    
                    let status: Status = self.findStatusByValue(value: saved!);
                    
                    result = [
                        "id": messageId,
                        "content": json!["content"] as! String,
                        "type": type,
                        "reference": "",
                        "resent": json!["resent"] as! Bool,
                        "status": [
                            "value": status.value as Any,
                            "label": status.label as Any,
                            "code": status.code as Any,
                            "icon": status.icon,
                            "cssClass": status.cssClass as Any
                            ] as [AnyHashable : Any],
                        "isIncoming": false,
                        "file": file,
                        "time": Mesibo.getInstance().getTimestamp(),
                        "peer": peer,
                        "sender": sender,
                        "groupId": groupId
                        ] as [AnyHashable : Any]
                    
                    let pluginResult = CDVPluginResult (status: CDVCommandStatus_OK, messageAs: result);
                    self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
                }
            } catch let parsingError {
                print("Error", parsingError)
                result = ["message": parsingError] as [AnyHashable : Any]
                let pluginResult = CDVPluginResult (status: CDVCommandStatus_ERROR, messageAs: result);
                self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
            }
        }
        
        // Realiza una llamada (normal o de video)
        @objc(call:)
        func call(command: CDVInvokedUrlCommand) {
            
            // Obtenemos los parámetros 'JSON'
            /*let params = command.arguments[0] as? [String:Any]
             
             // Buscamos el atributo 'is_video'
             let is_video = params?["is_video"] as! Bool
             // Buscamos el atributo 'peer'
             let peer = params?["peer"] as! String*/
            
            /* MesiboCall.sharedInstance().call("", callid: 0, address: "TEST", video: true, incoming: false) */
            
            
            
            // Mesibo.getInstance().call("TEST", video: true)
            
            var pluginResult = CDVPluginResult (status: CDVCommandStatus_ERROR, messageAs: "The Plugin Failed");
            // Set the plugin result to succeed.
            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "The plugin succeeded");
            // Send the function result back to Cordova.
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
        }
        
        // Retorna un arreglo de perfiles
        @objc(getProfiles:)
        func getProfiles(command: CDVInvokedUrlCommand) {
            // Declaramos un resultado para Cordova (Asumimos que fallará)
            let result = ["message": "Método en desarrollo."] as [AnyHashable : Any]
            
            let pluginResult = CDVPluginResult (status: CDVCommandStatus_OK, messageAs: result);
            // Enviamos la respuesta a cordova
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
        }
        
        // Retorna un perfil
        @objc(getProfile:)
        func getProfile(command: CDVInvokedUrlCommand) {
            // Declaramos un resultado para Cordova (Asumimos que fallará)
            let result = ["message": "Método en desarrollo."] as [AnyHashable : Any]
            
            let pluginResult = CDVPluginResult (status: CDVCommandStatus_OK, messageAs: result);
            // Enviamos la respuesta a cordova
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
        }
        
        // Eventos de mesibo
        func mesibo_(on message: MesiboMessage!) {}
        
        func mesibo_(onMessage params: MesiboParams!, data: Data!) {
            if(MesiboCordova.callbackOnMessage?.callbackId != nil) {
                let message = (data != nil ? String(data: data, encoding: String.Encoding.utf8) : "")
                
                var pluginResult = CDVPluginResult (status: CDVCommandStatus_ERROR, messageAs: "The Plugin Failed");
                
                do {
                    let data = message!.data(using: .utf8)!
                    let json = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any]
                    var file: Any = ""
                    
                    if(params.type == 2) {
                        do {
                            let fileData = json!["file"] as! NSDictionary
                            
                            file = [
                                "url": fileData["url"] as Any,
                                "size": fileData["size"] as Any,
                                "type": fileData["type"] as Any,
                                "name": fileData["name"] as Any
                            ] as [AnyHashable : Any]
                        } catch let parsingError {
                            print("Error", parsingError)
                        }
                    }
                    
                    let status: Status = self.findStatusByValue(value: params.status);
                    
                    let result = [
                        "id": params.mid,
                        "content": json!["content"] as! String,
                        "type": params.type,
                        "reference": "",
                        "resent": json!["resent"] as! Bool,
                        "status": [
                            "value": status.value as Any,
                            "label": status.label as Any,
                            "code": status.code as Any,
                            "icon": status.icon,
                            "cssClass": status.cssClass as Any
                            ] as [AnyHashable : Any],
                        "isIncoming": params.isIncoming(),
                        "file": file,
                        "time": params.ts,
                        "peer": params.peer,
                        "sender": json!["sender"] as! String,
                        "groupId": params.groupid
                        ] as [AnyHashable : Any]
                    
                    pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result);
                    pluginResult?.setKeepCallbackAs(true)
                    
                    self.commandDelegate!.send(pluginResult, callbackId: MesiboCordova.callbackOnMessage?.callbackId);
                    
                } catch let parsingError {
                    print("Error", parsingError)
                }
            }
        }
        
        func mesibo_(onFile params: MesiboParams!, file: MesiboFileInfo!) { }
        
        func mesibo_(onMessageStatus params: MesiboParams!) {
            if(MesiboCordova.callbackOnMessageStatus?.callbackId != nil) {
                var pluginResult = CDVPluginResult (status: CDVCommandStatus_ERROR, messageAs: "The Plugin Failed");
                
                let status: Status = self.findStatusByValue(value: params.status);
                
                let result = [
                    "id": params.mid,
                    "type": params.type,
                    "status": [
                        "value": status.value as Any,
                        "label": status.label as Any,
                        "code": status.code as Any,
                        "icon": status.icon,
                        "cssClass": status.cssClass as Any
                    ] as [AnyHashable : Any],
                    "time": params.ts,
                    "peer": params.peer,
                    "groupId": params.groupid
                    ] as [AnyHashable : Any]
                
                pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result);
                pluginResult?.setKeepCallbackAs(true)
                
                self.commandDelegate!.send(pluginResult, callbackId: MesiboCordova.callbackOnMessageStatus?.callbackId);
            }
        }
        
        func mesibo_(onActivity params: MesiboParams!, activity: Int32) {
            if(MesiboCordova.callbackOnActivity?.callbackId != nil) {
                
                var pluginResult = CDVPluginResult (status: CDVCommandStatus_ERROR, messageAs: "The Plugin Failed");
                
                var userProfile = MesiboUserProfile();
                
                if(params.profile != nil) {
                    userProfile = params.profile;
                } else {
                    userProfile.address = params.peer;
                    userProfile.groupid = params.groupid;
                }
                
                let result = [
                    "id": params.mid,
                    "time": Mesibo.getInstance().getTimestamp(),
                    "groupId": params.groupid,
                    "activity": activity,
                    "peer": params.peer,
                    "profile": [
                        "groupId": userProfile.groupid,
                        "unread": userProfile.unread,
                        "lastSeen": userProfile.lastActiveTime,
                        "address": userProfile.address,
                        "status": userProfile.status
                        ] as [AnyHashable: Any]
                    ] as [AnyHashable : Any]
                
                pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result);
                pluginResult?.setKeepCallbackAs(true)
                
                self.commandDelegate!.send(pluginResult, callbackId: MesiboCordova.callbackOnActivity?.callbackId);
            }
        }
        
        func findStatusByValue(value: Int32) -> Status {
            let statuses = [
                Status(iValue: 136, iLabel: "Bloqueado", iCode: "BLOCKED", iIcon: "", iCssClass: ""),
                Status(iValue: 22, iLabel: "LLamada entrante", iCode: "CALLINCOMING", iIcon: "", iCssClass: ""),
                Status(iValue: 21, iLabel: "LLamada perdida", iCode: "CALLMISSED", iIcon: "", iCssClass: ""),
                Status(iValue: 23, iLabel: "Llamando", iCode: "CALLOUTGOING", iIcon: "", iCssClass: ""),
                            Status(iValue: 32, iLabel: "Personalizado", iCode: "CUSTOM", iIcon: "", iCssClass: ""),
                            Status(iValue: 2, iLabel: "Entregado", iCode: "DELIVERED", iIcon: "md-done-all", iCssClass: "msg-delivered"),
                            Status(iValue: 132, iLabel: "Expirado", iCode: "EXPIRED",iIcon: "icon: md-time", iCssClass: "msg-sending"),
                            Status(iValue: 128, iLabel: "Fallido", iCode: "FAIL", iIcon: "", iCssClass: "msg-failed"),
                            Status(iValue: 130, iLabel: "Buzón lleno", iCode: "INBOXFULL", iIcon: "", iCssClass: ""),
                            Status(iValue: 131, iLabel: "Destino inválido", iCode: "INVALIDDEST", iIcon: "", iCssClass: ""),
                            Status(iValue: 0, iLabel: "Enviando", iCode: "OUTBOX", iIcon: "md-time", iCssClass: "msg-sending"),
                            Status(iValue: 3, iLabel: "Leído", iCode: "READ", iIcon: "md-done-all", iCssClass: "msg-read"),
                            Status(iValue: 18, iLabel: "Noticia recibida", iCode: "RECEIVEDNEW", iIcon: "", iCssClass: ""),
                            Status(iValue: 19, iLabel: "Leído", iCode: "RECEIVEDREAD", iIcon: "md-done-all", iCssClass: "msg-read"),
                            Status(iValue: 1, iLabel: "Enviado", iCode: "SENT", iIcon: "md-checkmark", iCssClass: "msg-sent"),
                            Status(iValue: 129, iLabel: "Usuario desconectado", iCode: "USEROFFLINE", iIcon: "", iCssClass: "")
            ];
            
            var obj = Status(iValue: -1, iLabel: "", iCode: "", iIcon: "", iCssClass: "");
            
            if let found = statuses.first(where: {$0.value == value}) {
                obj = found;
            }
            
            return obj;
        }
    }
    
    
    class Status {
        var memberName: String?
        
        var value: Int32?;
        var label: String?
        var code: String?
        var icon: String
        var cssClass: String?
        
        init(iValue: Int32, iLabel: String, iCode: String, iIcon: String, iCssClass: String) {
            value = iValue
            label = iLabel
            code = iCode
            icon = iIcon
            cssClass = iCssClass
        }
    }
