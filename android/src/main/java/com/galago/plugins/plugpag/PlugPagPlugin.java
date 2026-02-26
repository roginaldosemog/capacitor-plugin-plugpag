package com.galago.plugins.plugpag;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "PlugPag")
public class PlugPagPlugin extends Plugin {

    private PlugPag implementation;

    @Override
    public void load() {
        implementation = new PlugPag();
        if (getContext() != null) {
            implementation.initializeWrapper(getContext());
            com.getcapacitor.Logger.info("PlugPag", "SDK Wrapper inicializado com sucesso.");
        }
    }

    @PluginMethod
    public void isAuthenticated(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("value", implementation.isAuthenticated());
        call.resolve(ret);
    }

    @PluginMethod
    public void isServiceBusy(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("value", implementation.isServiceBusy());
        call.resolve(ret);
    }

    @PluginMethod
    public void initialize(PluginCall call) {
        String activationCode = call.getString("activationCode");

        if (activationCode == null) {
            call.reject("Código de ativação é obrigatório.");
            return;
        }

        getBridge().execute(() -> {
            try {
                JSObject result = implementation.initialize(activationCode);
                call.resolve(result);
            } catch (Exception e) {
                call.reject(e.getMessage());
            }
        });
    }

    @PluginMethod
    public void doPayment(PluginCall call) {
        Integer type = call.getInt("type", 1); // Ex: 1 = Crédito, 2 = Débito
        Integer amount = call.getInt("amount"); // Centavos
        Integer installmentType = call.getInt("installmentType", 1);
        Integer installments = call.getInt("installments", 1);
        String userReference = call.getString("userReference", "");
        Boolean printReceipt = call.getBoolean("printReceipt", true);

        if (amount == null) {
            call.reject("O valor (amount) é obrigatório.");
            return;
        }

        getBridge().execute(() -> {
            try {
                JSObject result = implementation.doPayment(
                    type, amount, installmentType, installments, userReference, printReceipt,

                    (message, eventCode) -> {
                        JSObject eventData = new JSObject();
                        eventData.put("message", message);
                        eventData.put("code", eventCode);
                        notifyListeners("paymentProgress", eventData);
                    }
                );

                call.resolve(result);
            } catch (Exception e) {
                call.reject(e.getMessage());
            }
        });
    }

    @PluginMethod
    public void voidPayment(PluginCall call) {
        String transactionCode = call.getString("transactionCode");
        String transactionId = call.getString("transactionId");
        Boolean printReceipt = call.getBoolean("printReceipt", true);

        if (transactionCode == null) {
            call.reject("O transactionCode é obrigatório.");
            return;
        }

        if (transactionId == null) {
            call.reject("O transactionId é obrigatório.");
            return;
        }

        getBridge().execute(() -> {
            try {
                JSObject result = implementation.voidPayment(
                    transactionCode, transactionId, printReceipt,
                    (message, eventCode) -> {
                        JSObject eventData = new JSObject();
                        eventData.put("message", message);
                        eventData.put("code", eventCode);
                        notifyListeners("voidProgress", eventData);
                    }
                );

                call.resolve(result);
            } catch (Exception e) {
                call.reject(e.getMessage());
            }
        });
    }
}